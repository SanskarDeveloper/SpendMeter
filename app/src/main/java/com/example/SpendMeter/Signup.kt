package com.example.SpendMeter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.SpendMeter.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Signup : AppCompatActivity() {
    private val binding: ActivitySignupBinding by lazy {
        ActivitySignupBinding.inflate(layoutInflater)
    }
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference

        binding.SignInButton.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        binding.RegisterButton.setOnClickListener {
            // Get text from input fields
            val email = binding.Email.text.toString()
            val userName = binding.Username.text.toString()
            val password = binding.Password.text.toString()
            val repeatPassword = binding.repeatPassword.text.toString()

            // Validation
            if (email.isEmpty() || userName.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all the details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (repeatPassword != password) {
                Toast.makeText(this, "Repeat Password must match Password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create user with email and password
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()

                        dataSaving(userName, this)

                        val intent = Intent(this, Login::class.java)
                        intent.putExtra("userName", userName)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}

fun dataSaving(userName: String, context: Context) {
    if (userName.isBlank()) {
        Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_LONG).show()
        return
    }

    val databaseReference = FirebaseDatabase.getInstance().reference
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    if (currentUser == null) {
        Toast.makeText(
            context,
            "User not authenticated. Please log in and try again.",
            Toast.LENGTH_LONG 
        ).show()
        return
    }

    databaseReference.child("users").child(currentUser.uid).child("UserName").setValue(userName)
}