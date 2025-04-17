package com.example.letssee

import java.io.Serializable

data class AppUser(
    val id: String,
    val name: String,
    var lastMessage: String = "",
    var timestamp: Long = 0,
    var isNewMessage: Boolean = false
) : Serializable