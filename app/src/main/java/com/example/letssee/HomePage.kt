package com.example.letssee

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth


class HomePage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)


        // Find the TextView where you want to display the User ID
        val userIdTextView = findViewById<TextView>(R.id.tvUserId)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val vc = findViewById<Button>(R.id.videocall)

        auth = FirebaseAuth.getInstance()

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginPage::class.java))
        }



        // Fetch the user ID of the currently logged-in user
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            // Set the user ID in the TextView
            userIdTextView.text = "User ID: $userId"
        } else {
            // No user is logged in
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
            userIdTextView.text = "No user logged in"
        }

        // Applying window insets to adjust the layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    }


}
