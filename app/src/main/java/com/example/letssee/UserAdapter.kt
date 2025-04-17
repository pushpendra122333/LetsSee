package com.example.letssee

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(
    private var userList: MutableList<AppUser>,
    private val onUserClick: (AppUser) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        private val tvLastMessage: TextView = view.findViewById(R.id.tvLastMessage)

        fun bind(user: AppUser) {
            tvUserName.text = user.name
            tvLastMessage.text = user.lastMessage.ifEmpty { "No messages yet" }
            itemView.setOnClickListener { onUserClick(user) }  // 👈 Handle click event
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(userList[position])

    }

    override fun getItemCount(): Int = userList.size

    fun updateUserList(newList: List<AppUser>) {
        userList.clear()
        userList.addAll(newList)
        notifyDataSetChanged()
    }
}
