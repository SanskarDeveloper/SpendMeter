package com.example.SpendMeter

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.SpendMeter.databinding.ActivityEnterAmountGetBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class EnterAmountGet : AppCompatActivity() {

    private val binding: ActivityEnterAmountGetBinding by lazy {
        ActivityEnterAmountGetBinding.inflate(layoutInflater)
    }
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var customerId: String? = null  // To store the customerId passed from CustomerUpdate

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // Retrieve customerId from intent
        customerId = intent.getStringExtra("customerId")
        if (customerId.isNullOrEmpty()) {
            finish()
            return
        }

        // Set UI text
        binding.userName.text = "You get amount"

        // Save button click listener
        binding.saveButton.setOnClickListener {
            val reason = binding.reason.text.toString().trim()
            val amountText = binding.spend.text.toString().trim()

            if (reason.isEmpty() || amountText.isEmpty()) {
                showError("Please fill in all fields")
                return@setOnClickListener
            }

            val amount = try {
                amountText.toInt()
            } catch (_: NumberFormatException) {
                showError("Invalid amount entered")
                return@setOnClickListener
            }

            if (amount <= 0) {
                showError("Please enter a valid positive amount")
                return@setOnClickListener
            }

            val currentUser = auth.currentUser
            if (currentUser != null) {
                val customerRef = databaseReference.child("users").child(currentUser.uid).child("customer")
                saveCustomerAmount(customerRef, reason, amount)
            } else {
                showError("User not logged in")
            }

            val getRef = databaseReference.child("users").child(currentUser?.uid.toString())
            updateGetAmount(amount, getRef)

            val customerGiveRef =
                databaseReference.child("users").child(currentUser?.uid.toString())
                    .child("customer").child(customerId!!)
            updateGetCustomerAmount(amount, customerGiveRef)
        }
    }

    fun updateGetAmount(amount: Int, customerRef: DatabaseReference){
        customerRef.child("getAmount").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentGet = snapshot.getValue(Int::class.java) ?: 0

                // Calculate updated total
                val updatedGet = currentGet + amount

                // Update totalAmount in Firebase
                customerRef.child("getAmount").setValue(updatedGet)
            }

            override fun onCancelled(error: DatabaseError) {
                showError("Database error: ${error.message}")
            }
        })
    }

    fun updateGetCustomerAmount(amount: Int, customerRef: DatabaseReference) {
        customerRef.child("getCustomerAmount")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentGet = snapshot.getValue(Int::class.java) ?: 0

                    // Calculate updated total
                    val updatedGet = currentGet + amount

                    // Update totalAmount in Firebase
                    customerRef.child("getCustomerAmount").setValue(updatedGet)
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Database error: ${error.message}")
                }
            })
    }



    private fun saveCustomerAmount(customerRef: DatabaseReference, reason: String, amount: Int) {
        customerRef.child(customerId!!).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    saveAmountToDatabase(customerRef, reason, amount)
                } else {
                    showError("Customer data not found")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showError("Database error: ${error.message}")
            }
        })
    }

    private fun saveAmountToDatabase(customerRef: DatabaseReference, reason: String, amount: Int) {
        val flag = true
        val (date, time) = getCurrentDateAndTime()
        val amountKey = customerRef.child(customerId!!).child("amountList").push().key

        if (amountKey != null) {
            val amountData = AmountModel(reason, amount, flag, date, time, amountKey)

            customerRef.child(customerId!!).child("amountList").child(amountKey)
                .setValue(amountData)
                .addOnSuccessListener {
                    ensureTotalAmountInitialized(customerRef.child(customerId!!)) {
                        updateTotalAmount(customerRef.child(customerId!!), amount, flag)
                    }
                    // Navigate to CustomerUpdate on success
                    startActivity(Intent(this@EnterAmountGet, CustomerUpdate::class.java))
                    finish() // Optional: Close the current activity
                }
                .addOnFailureListener { exception ->
                    showError("Failed to save amount: ${exception.message}")
                }
        } else {
            showError("Failed to generate a unique key")
        }
    }


    private fun ensureTotalAmountInitialized(customerRef: DatabaseReference, onInitialized: () -> Unit) {
        customerRef.child("totalAmount").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // Initialize totalAmount to 0.0 if it doesn't exist
                    customerRef.child("totalAmount").setValue(0.0)
                        .addOnSuccessListener {
                            onInitialized()
                        }
                        .addOnFailureListener { exception ->
                            showError("Failed to initialize total amount: ${exception.message}")
                        }
                } else {
                    onInitialized()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showError("Database error: ${error.message}")
            }
        })
    }

    private fun updateTotalAmount(customerRef: DatabaseReference, amount: Int, flag: Boolean) {
        customerRef.child("totalAmount").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentTotal = snapshot.getValue(Double::class.java) ?: 0.0

                // Calculate updated total
                val updatedTotal = if (flag) {
                    currentTotal + amount // Add if flag is true
                } else {
                    currentTotal - amount // Subtract if flag is false
                }

                // Update totalAmount in Firebase
                customerRef.child("totalAmount").setValue(updatedTotal)
                    .addOnFailureListener { exception ->
                        showError("Failed to update total amount: ${exception.message}")
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                showError("Database error: ${error.message}")
            }
        })
    }





    private fun getCurrentDateAndTime(): Pair<String, String> {
        val currentDateTime = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return Pair(dateFormat.format(currentDateTime), timeFormat.format(currentDateTime))
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e("EnterAmountGet", message)
    }
}