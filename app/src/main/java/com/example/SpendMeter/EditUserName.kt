package com.example.SpendMeter

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.SpendMeter.databinding.ActivityEditUserNameBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class EditUserName : AppCompatActivity() {

    private lateinit var binding: ActivityEditUserNameBinding
    private lateinit var auth: FirebaseAuth
    private val dbReference by lazy { FirebaseDatabase.getInstance().reference }
    private var userName: String? = null
    private var isCustomer: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditUserNameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Retrieve data from the intent safely
        userName = intent.getStringExtra("userName")
        isCustomer = intent.getBooleanExtra("idCustomer", false)

        // Check if userName is null before using it
        if (userName != null) {
            auth.currentUser?.let { user ->
                dbReference.child("users").child(user.uid)

                binding.apply {
                    profileImage.setImageBitmap(profileImageGenerator(userName!!, 100))
                    bigProfileImage.setImageBitmap(profileImageGenerator(userName!!, 700))
                    CustomerName.text = userName?.capitalizeFirstLetter()
                }
            }
        } else {
            // Handle the case where userName is null (e.g., show an error message)
            showToast("Error: Username not provided.")
            finish() // Close the activity if userName is essential
        }

        binding.SaveBtn.setOnClickListener {
            if (isCustomer)
                saveUserName()
            else
                saveCustomerName()
        }
    }

    private fun saveUserName() {
        val name = binding.UserName.text.toString().trim()
        if (name.isEmpty()) {
            showToast("Username cannot be empty")
            return
        }

        auth.currentUser?.let { user ->
            dbReference.child("users").child(user.uid).child("UserName")
                .setValue(name)
                .addOnSuccessListener {
                    showToast("Username updated successfully")
                    Log.d("EditUserName", "Successfully updated username: $name")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener { error ->
                    showToast("Failed to update username: ${error.message}")
                }
        } ?: showToast("User not logged in")
    }

    private fun saveCustomerName() {
        val name = binding.UserName.text.toString().trim()
        if (name.isEmpty()) {
            showToast("Customer name cannot be empty")
            return
        }

        val customerId = intent.getStringExtra("CustomerId")

        auth.currentUser?.let { user ->
            dbReference.child("users").child(user.uid).child("customer")
                .child(customerId.toString()).child("name")
                .setValue(name)
                .addOnSuccessListener {
                    showToast("Customer name updated successfully")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener { error ->
                    showToast("Failed to update Customer name: ${error.message}")
                }
        } ?: showToast("User not logged in")
    }

    private fun String.capitalizeFirstLetter(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }

    private fun profileImageGenerator(name: String, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val firstLetter = name.firstOrNull()?.uppercase() ?: "?"
        val hue = (name.hashCode() % 360).toFloat()
        val color = Color.HSVToColor(floatArrayOf(hue, 0.7f, 0.8f))

        paint.apply {
            this.color = color
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, this)

            this.color = Color.WHITE
            this.textSize = size / 2.5f
            this.textAlign = Paint.Align.CENTER
            this.typeface = Typeface.DEFAULT_BOLD
        }

        val yPos = (size / 2 - (paint.descent() + paint.ascent()) / 2)
        canvas.drawText(firstLetter, size / 2f, yPos, paint)

        return bitmap
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}