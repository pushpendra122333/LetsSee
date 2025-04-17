package com.example.letssee

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener

class Chat : AppCompatActivity() {

    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<Message>()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var messagesRef: CollectionReference
    private lateinit var currentUserId: String

    private lateinit var tvUsername: TextView
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnBack: ImageView
    private var isChatOpen = true

    private var receiverId: String? = null
    private var receiverName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid.toString()

        receiverId = intent.getStringExtra("receiverId") ?: return

        tvUsername = findViewById(R.id.tvUsername)
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)

        receiverId = intent.getStringExtra("receiverId")
        receiverName = intent.getStringExtra("receiverName")
        tvUsername.text = receiverName ?: "User"

        firestore = FirebaseFirestore.getInstance()
        val chatId = getChatId(currentUserId, receiverId!!)
        messagesRef = firestore.collection("chats")
            .document(chatId)
            .collection("messages")

        chatAdapter = ChatAdapter(messageList, currentUserId) { message ->
            showDeleteMessageDialog(message)
        }

        rvMessages.adapter = chatAdapter
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        loadChatHistory()

        btnSend.setOnClickListener {
            sendMessage()
        }

        etMessage.setOnClickListener {
            scrollToBottom()
        }

        btnBack.setOnClickListener { finish() }

        rvMessages.viewTreeObserver.addOnGlobalLayoutListener {
            scrollToBottom()
        }
        scheduleNewMessageCheck(chatId)
    }

    private fun loadChatHistory() {
        messagesRef.orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener(EventListener { snapshots, error ->
                if (error != null) {
                    Log.e("Chat", "Listen failed", error)
                    return@EventListener
                }

                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            val message = dc.document.toObject(Message::class.java)
                            messageList.add(message)
                            chatAdapter.notifyItemInserted(messageList.size - 1)
                            scrollToBottom()

                            if (isChatOpen && message.receiverId == currentUserId && !message.seen) {
                                dc.document.reference.update("seen", true)
                            }
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val updatedMessage = dc.document.toObject(Message::class.java)
                            val index = messageList.indexOfFirst { it.timestamp == updatedMessage.timestamp }
                            if (index != -1) {
                                messageList[index] = updatedMessage
                                chatAdapter.notifyItemChanged(index)
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            val removedMessage = dc.document.toObject(Message::class.java)
                            val index = messageList.indexOfFirst { it.timestamp == removedMessage.timestamp }
                            if (index != -1) {
                                messageList.removeAt(index)
                                chatAdapter.notifyItemRemoved(index)
                                chatAdapter.notifyItemRangeChanged(index, messageList.size)
                            }
                        }
                    }
                }
            })
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty() || receiverId.isNullOrEmpty()) return

        val messageId = messagesRef.document().id
        val message = Message(
            senderId = currentUserId,
            receiverId = receiverId!!,
            message = text,
            timestamp = System.currentTimeMillis(),
            seen = false
        )

        messagesRef.document(messageId).set(message)
            .addOnSuccessListener {
                etMessage.text.clear()
                scrollToBottom()
            }
            .addOnFailureListener {
                Log.e("Chat", "Failed to send message", it)
            }
    }

    private fun deleteMessage(message: Message) {
        messagesRef.whereEqualTo("message", message.message)
            .whereEqualTo("timestamp", message.timestamp)
            .get()
            .addOnSuccessListener { docs ->
                for (doc in docs) {
                    doc.reference.delete()
                        .addOnSuccessListener {
                            val index = messageList.indexOfFirst { it.timestamp == message.timestamp }
                            if (index != -1) {
                                messageList.removeAt(index)
                                chatAdapter.notifyItemRemoved(index)
                                chatAdapter.notifyItemRangeChanged(index, messageList.size)
                            }
                        }
                        .addOnFailureListener {
                            Log.e("Chat", "Delete failed", it)
                        }
                }
            }
    }

    private fun scheduleNewMessageCheck(chatId: String) {
        val workRequest = OneTimeWorkRequest.Builder(NewMessageWorker::class.java)
            .setInputData(workDataOf("receiverId" to receiverId, "chatId" to chatId))  // Pass receiver ID and chat ID
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun showDeleteMessageDialog(message: Message) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_message, null)
        val alertDialog = AlertDialog.Builder(this).setView(view).create()

        view.findViewById<TextView>(R.id.tvDeleteForEveryone).setOnClickListener {
            deleteMessage(message)
            alertDialog.dismiss()
        }

        view.findViewById<TextView>(R.id.tvCancel).setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun getChatId(user1: String, user2: String): String {
        return if (user1 < user2) "$user1-$user2" else "$user2-$user1"
    }

    private fun scrollToBottom() {
        rvMessages.postDelayed({
            if (messageList.isNotEmpty()) {
                rvMessages.smoothScrollToPosition(messageList.size - 1)
            }
        }, 100)
    }

    override fun onResume() {
        super.onResume()
        isChatOpen = true
    }

    override fun onPause() {
        super.onPause()
        isChatOpen = false
    }
}
