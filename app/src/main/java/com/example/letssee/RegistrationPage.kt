package com.example.letssee

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistrationPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration_page)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val spinnerGender = findViewById<Spinner>(R.id.spinnerGender)

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.gender_array,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = adapter


        val btnRegister = findViewById<Button>(R.id.btnRegister)
        btnRegister.setOnClickListener { registerUser() }
    }

    private fun registerUser() {
        val name = findViewById<EditText>(R.id.etName).text.toString().trim()
        val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
        val phone = findViewById<EditText>(R.id.etPhone).text.toString().trim()
        val password = findViewById<EditText>(R.id.etPassword).text.toString().trim()

        // Handle gender field
        val gender = findViewById<Spinner>(R.id.spinnerGender).selectedItem?.toString()?.trim()

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || gender.isNullOrEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid ?: return@addOnSuccessListener

                val userMap = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "gender" to gender,
                    "uid" to uid
                )

                firestore.collection("users").document(uid).set(userMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginPage::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Firestore error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Auth failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
