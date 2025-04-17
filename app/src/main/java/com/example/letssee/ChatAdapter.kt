package com.example.letssee

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class ChatAdapter(
    private val messageList: MutableList<Message>,
    private val currentUserId: String,
    private val onMessageLongClick: (Message) -> Unit  // Pass a function to handle long-press
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val tvSeen: TextView? = view.findViewById(R.id.tvSeenStatus)  // New Seen TextView

        fun bind(message: Message, isLastMessage: Boolean) {
            tvMessage.text = message.message


            // Show "Seen" only on the last message
            if (isLastMessage && message.senderId == currentUserId) {
                tvSeen?.visibility = View.VISIBLE
                tvSeen?.text = if (message.seen) "Seen ✅" else "Sent ⏳"
            } else {
                tvSeen?.visibility = View.GONE  // Hide for all other messages
            }

            if (message.senderId == currentUserId) {
                tvMessage.setOnLongClickListener {
                    onMessageLongClick(message)
                    true
                }
            } else {
                tvMessage.setOnLongClickListener(null)
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layout = if (viewType == 0) R.layout.item_message_sent else R.layout.item_message_received
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val isLastMessage = position == messageList.size - 1
        holder.bind(messageList[position], isLastMessage)
    }



    override fun getItemCount(): Int = messageList.size

    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].senderId == currentUserId) 0 else 1
    }
}
