package com.example.letssee

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class UserListActivity : AppCompatActivity() {

    private lateinit var userAdapter: UserAdapter
    private lateinit var rvChatList: RecyclerView
    private val userList = mutableListOf<AppUser>()
    private lateinit var firestore: FirebaseFirestore
    private var currentUserId: String? = null
    private lateinit var btnLogout: TextView
    private var userListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid



        rvChatList = findViewById(R.id.rvChatList)
        rvChatList.layoutManager = LinearLayoutManager(this)
        rvChatList.addItemDecoration(SpacingItemDecoration(30))

        firestore = FirebaseFirestore.getInstance()

        userAdapter = UserAdapter(userList) { user ->
            val intent = Intent(this, Chat::class.java).apply {
                putExtra("senderId", currentUserId)
                putExtra("receiverId", user.id)
                putExtra("receiverName", user.name)
            }
            startActivity(intent)
        }

        rvChatList.adapter = userAdapter

        fetchUsersFromFirestore()

        btnLogout = findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginPage::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun fetchUsersFromFirestore() {
        userListener = firestore.collection("users")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("Firestore", "Error fetching users", error)
                    return@addSnapshotListener
                }

                val usersToFetch = mutableListOf<AppUser>()
                for (doc in snapshots!!.documents) {
                    val id = doc.id
                    val name = doc.getString("name") ?: ""

                    if (id != currentUserId) {
                        val user = AppUser(id, name, "", 0)
                        usersToFetch.add(user)
                    }
                }

                if (usersToFetch.isNotEmpty()) {
                    fetchLastMessages(usersToFetch) { updatedUsers ->
                        userAdapter.updateUserList(updatedUsers)
                    }
                } else {
                    userAdapter.updateUserList(emptyList())
                }
            }
    }

    private fun fetchLastMessages(users: List<AppUser>, callback: (List<AppUser>) -> Unit) {
        val updatedUsers = users.toMutableList() // Start with a copy of users

        for ((index, user) in users.withIndex()) {
            val chatId = getChatId(currentUserId!!, user.id)

            firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("Firestore", "Error fetching last message", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val doc = snapshot.documents[0]
                        val lastMessage = doc.getString("message") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: 0

                        updatedUsers[index].lastMessage = lastMessage
                        updatedUsers[index].timestamp = timestamp

                        // Notify the adapter for just this user
                        callback(updatedUsers.sortedByDescending { it.timestamp })
                    }
                }
        }
    }


    private fun getChatId(user1: String, user2: String): String {
        return if (user1 < user2) "$user1-$user2" else "$user2-$user1"
    }

    override fun onDestroy() {
        super.onDestroy()
        userListener?.remove()
    }
}
