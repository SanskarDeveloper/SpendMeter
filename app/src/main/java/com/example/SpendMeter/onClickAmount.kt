package com.example.SpendMeter

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.SpendMeter.databinding.ActivityOnClickAmountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class onClickAmount : AppCompatActivity() {
    private val binding: ActivityOnClickAmountBinding by lazy {
        ActivityOnClickAmountBinding.inflate(layoutInflater)
    }
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference

        // Getting values from the intent
        val flag = intent.getBooleanExtra("Flag", false)
        val reason = intent.getStringExtra("Reason")
        val amount = intent.getStringExtra("Amount")
        val date = intent.getStringExtra("Date")
        val time = intent.getStringExtra("Time")
        val amountId = intent.getStringExtra("AmountId")
        val customerId = intent.getStringExtra("CustomerId")

        binding.backButton.setOnClickListener {
            startActivity(Intent(this, CustomerUpdate::class.java))
        }

        // Displaying details based on the flag
        if (flag) {
            binding.textView2.text = "You have to get amount"
            binding.amountTextView.setTextColor(Color.parseColor("#00FF00"))
        } else {
            binding.textView2.text = "You have to give amount"
            binding.amountTextView.setTextColor(Color.parseColor("#FF0000"))
        }

        binding.reasonTextView.text = "Reason : $reason"
        binding.amountTextView.text = "Amount : $amount"
        binding.dateTextView.text = "Date : $date"
        binding.timeTextView.text = "Time : $time"

        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.delete.setOnClickListener {
            SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Are you sure?")
                .setContentText("Won't be able to recover this file!")
                .setConfirmText("Yes, delete it!")
                .setCancelText("Cancel")
                .setCancelClickListener { dialog ->
                    dialog.dismissWithAnimation() // Dismiss dialog if user cancels
                }
                .setConfirmClickListener { sDialog ->
                    // Perform delete operation
                    deleteCustomer(customerId, amountId)

                    val currentUser = auth.currentUser
                    currentUser?.let { user ->
                        val customerRef = databaseReference
                            .child("users")
                            .child(user.uid)
                            .child("customer")
                            .child(customerId!!)

                        val getRef = databaseReference.child("users").child(user.uid)
                        val customerGetRef = getRef.child("customer").child(customerId)

                        updateTotalAmount(customerRef, amount?.toInt() ?: 0, flag)
                        if (flag) {
                            updateGetAmount(amount?.toInt() ?: 0, getRef)
                            updateCustomerGetAmount(amount?.toInt() ?: 0, customerGetRef)
                        }
                        else
                            updateGiveAmount(amount?.toInt() ?: 0, getRef)
                            updateCustomerGiveAmount(amount?.toInt() ?: 0, customerGetRef)
                    }

                    // Dismiss the warning dialog
                    sDialog.dismissWithAnimation()

                    // Show success dialog after deletion
                    SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("Deleted!")
                        .setContentText("Your file has been successfully deleted!")
                        .setConfirmText("OK")
                        .setConfirmClickListener { successDialog ->
                            successDialog.dismissWithAnimation() // Dismiss the success dialog
                            finish() // Close the activity or refresh the UI as needed
                        }
                        .show()
                }
                .show()
        }

    }

    private fun deleteCustomer(customerId: String?, amountId: String?) {
        if (customerId.isNullOrEmpty() || amountId.isNullOrEmpty()) {
            Toast.makeText(this, "Customer ID or Amount ID is null or empty", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val customerRef = databaseReference
                .child("users")
                .child(user.uid)
                .child("customer")
                .child(customerId)
                .child("amountList")
                .child(amountId)

            customerRef.removeValue()
                .addOnSuccessListener {
//                    Toast.makeText(this, "Amount deleted successfully", Toast.LENGTH_SHORT).show()
                    finish()  // Close the activity after deleting the customer
                }
                .addOnFailureListener { e ->
                    Log.e("CustomerUpdate", "Error deleting customer: ${e.message}")
                    Toast.makeText(this, "Failed to delete customer", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateGetAmount(amount: Int, customerRef: DatabaseReference) {
        customerRef.child("getAmount").runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val currentGet = mutableData.getValue(Int::class.java) ?: 0

                // Prevent negative values (optional)
                if (currentGet < amount) {
                    return Transaction.success(mutableData) // No change
                }

                mutableData.value = currentGet - amount
                return Transaction.success(mutableData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                dataSnapshot: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e("updateGetAmount", "Failed to update: ${error.message}")
                } else if (committed) {
                    Log.d("updateGetAmount", "Successfully updated getAmount")
                }
            }
        })
    }

    fun updateCustomerGetAmount(amount: Int, customerRef: DatabaseReference) {
        customerRef.child("getCustomerAmount").runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val currentGet = mutableData.getValue(Int::class.java) ?: 0

                // Prevent negative values (optional)
                if (currentGet < amount) {
                    return Transaction.success(mutableData) // No change
                }

                mutableData.value = currentGet - amount
                return Transaction.success(mutableData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                dataSnapshot: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e("updateGetAmount", "Failed to update: ${error.message}")
                } else if (committed) {
                    Log.d("updateGetAmount", "Successfully updated getAmount")
                }
            }
        })
    }



    fun updateGiveAmount(amount: Int, customerRef: DatabaseReference) {
        customerRef.child("giveAmount").runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val currentGive = mutableData.getValue(Int::class.java) ?: 0

                // Prevent negative values (optional)
                if (currentGive < amount) {
                    return Transaction.success(mutableData) // No change
                }

                mutableData.value = currentGive - amount
                return Transaction.success(mutableData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                dataSnapshot: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e("updateGetAmount", "Failed to update: ${error.message}")
                } else if (committed) {
                    Log.d("updateGetAmount", "Successfully updated getAmount")
                }
            }
        })
    }



    fun updateCustomerGiveAmount(amount: Int, customerRef: DatabaseReference) {
        customerRef.child("giveCustomerAmount").runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val currentGive = mutableData.getValue(Int::class.java) ?: 0

                // Prevent negative values (optional)
                if (currentGive < amount) {
                    return Transaction.success(mutableData) // No change
                }

                mutableData.value = currentGive - amount
                return Transaction.success(mutableData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                dataSnapshot: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e("updateGetAmount", "Failed to update: ${error.message}")
                } else if (committed) {
                    Log.d("updateGetAmount", "Successfully updated getAmount")
                }
            }
        })
    }


    private fun updateTotalAmount(customerRef: DatabaseReference, amount: Int, flag: Boolean) {
        // Fetch the current totalAmount from Firebase
        customerRef.child("totalAmount")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentTotal = snapshot.getValue(Double::class.java) ?: 0.0

                    // Determine the updated total based on the flag
                    val updatedTotal = if (flag) {
                        currentTotal - amount // Subtract for "get" scenario
                    } else {
                        currentTotal + amount // Add for "give" scenario
                    }

                    // Check for negative total, if not allowed

                    // Update the totalAmount in Firebase
                    customerRef.child("totalAmount").setValue(updatedTotal)
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

}