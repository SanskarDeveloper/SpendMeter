package com.example.SpendMeter.Customers.AllCustomers

import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import androidx.recyclerview.widget.RecyclerView
import com.example.SpendMeter.CustomerUpdate
import com.example.SpendMeter.databinding.CustomerItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.abs

class RvAdapter(private val customers: List<CustomerModel>) :
    RecyclerView.Adapter<RvAdapter.CustomerViewHolder>() {

        interface OnItemClickListener{
            fun onDeleteClick(customerId : String)
        }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): CustomerViewHolder {
        val binding =
            CustomerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomerViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: CustomerViewHolder, position: Int
    ) {
        val customer = customers[position]
        holder.bind(customer)

        amin(holder.itemView)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, CustomerUpdate::class.java)

            // Pass the customer ID and other details to the next activity
            intent.putExtra("customerId", customer.customerId)
            intent.putExtra("CUSTOMER_NAME",customer.name)

            context.startActivity(intent)
        }


    }

    override fun getItemCount(): Int {
        return customers.size
    }

    class CustomerViewHolder(val binding: CustomerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(customer: CustomerModel) {
            // Example usage in an Activity or ViewModel
            val customerId = customer.customerId // Replace with the actual customerId

            fetchTotalAmount(customerId) { totalAmount ->
                if (totalAmount != null) {
                    // Successfully retrieved the totalAmount
                    Log.d("TotalAmount", "Total amount is: $totalAmount")
                    // Use the totalAmount here, e.g., update UI or perform further calculations

                    if (totalAmount<0){
                        var amount=abs(totalAmount)
                        binding.Amount.text = "₹$amount"
                        binding.Amount.setTextColor(Color.parseColor("#FF5C5C"))
                    }else if(totalAmount>0){
                        binding.Amount.text = "₹${totalAmount}"
                        binding.Amount.setTextColor(Color.parseColor("#2ECC71"))
                    }
                } else {
                    // Error occurred, totalAmount is null
                    Log.e("TotalAmount", "Failed to fetch total amount")
                }
            }

            binding.name.text = customer.name
            val imageView = binding.profileImage
            val bitmap = customer.profilePhoto // Generate the profile image
            imageView.setImageBitmap(bitmap)

        }

        fun fetchTotalAmount(customerId: String, onResult: (Int?) -> Unit) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val databaseReference = FirebaseDatabase.getInstance().reference
                val totalAmountRef = databaseReference
                    .child("users")
                    .child(currentUser.uid)
                    .child("customer")
                    .child(customerId)
                    .child("totalAmount")

                // Fetch the totalAmount from the database
                totalAmountRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Get the value as Double
                        val totalAmount = snapshot.getValue(Int::class.java)
                        // Call the callback function to return the totalAmount
                        onResult(totalAmount)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Log the error and return null in case of failure
                        Log.e("FetchTotalAmount", "Database error: ${error.message}")
                        onResult(null)
                    }
                })
            } else {
                // User not logged in
                Log.e("FetchTotalAmount", "User not logged in")
                onResult(null)
            }
        }
    }

    fun amin(view:View){
        val animation = AlphaAnimation(0.0f, 1.0f)
        animation.duration = 500
        view.startAnimation(animation)
    }

}