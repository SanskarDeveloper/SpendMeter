package com.example.SpendMeter

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.SpendMeter.Customers.AllCustomers.CustomerModel
import com.example.SpendMeter.databinding.ActivityCreateCustomerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

@Suppress("DEPRECATION")
class customer_create : AppCompatActivity() {
    private val binding: ActivityCreateCustomerBinding by lazy {
        ActivityCreateCustomerBinding.inflate(layoutInflater)
    }
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //Initialize firebase database reference
        databaseReference = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        binding.SaveButton.setOnClickListener {
            // gettext from edit text
            val username = binding.UserNameText.text.toString()
            binding.CustomerName.text = username

            if (username.isEmpty()) {
                Toast.makeText(this, "Please fill all the arguments", Toast.LENGTH_LONG).show()
            } else {
                val currentUser = auth.currentUser

                currentUser?.let { user ->
                    // Generate a unique key for the customer
                    val customerKey =
                        databaseReference.child("users").child(user.uid).child("customer")
                            .push().key
                    val customerItem = CustomerModel(username, customerKey ?: "", null)
                    if (customerKey != null) {

                        databaseReference.child("users").child(user.uid).child("customer")
                            .child(customerKey).setValue(customerItem)
                            .addOnCompleteListener { task ->
                                if (!task.isSuccessful) {
                                    Toast.makeText(this, "Failed to save Note", Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    finish()
                                }
                            }
                    }
                }
            }
        }
    }
}
