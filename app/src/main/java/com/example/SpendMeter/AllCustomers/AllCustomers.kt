package com.example.SpendMeter.AllCustomers

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.SpendMeter.Customers.AllCustomers.CustomerModel
import com.example.SpendMeter.Customers.AllCustomers.RvAdapter
import com.example.SpendMeter.customer_create
import com.example.SpendMeter.databinding.FragmentAllCustomersBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.SpendMeter.R

@Suppress("DEPRECATION")
class AllCustomers : Fragment(), RvAdapter.OnItemClickListener {
    private lateinit var binding: FragmentAllCustomersBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: RvAdapter
    private val customerList = mutableListOf<CustomerModel>()
    private var loadingDialog: SweetAlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAllCustomersBinding.inflate(inflater, container, false)

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // Set up RecyclerView
        binding.customerRecyclerview.layoutManager = LinearLayoutManager(requireContext())

        // Fetch data
        showLoadingDialog()
        setupCustomerListener()

        // Add new customer button
        binding.addCustomer.setOnClickListener {
            val intent = Intent(requireContext(), customer_create::class.java)
            startActivity(intent)
        }

        return binding.root
    }

    private fun setupCustomerListener() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            dismissLoadingDialog()
            return
        }

        val customerReference =
            databaseReference.child("users").child(currentUser.uid).child("customer")

        // Add real-time listener
        customerReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                customerList.clear() // Clear the list to avoid duplication
                for (customerSnapshot in snapshot.children) {
                    val customer = customerSnapshot.getValue(CustomerModel::class.java)
                    customer?.let {
                        it.name = capitalizeFirstLetter(it.name)
                        it.profilePhoto = profileImageGenerator(it.name)
                        customerList.add(it)
                    }
                }
                customerList.reverse() // Optional: Reverse for latest data first
                adapter = RvAdapter(customerList)
                binding.customerRecyclerview.adapter = adapter
                dismissLoadingDialog()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(), "Failed to load data: ${error.message}", Toast.LENGTH_SHORT
                ).show()
                dismissLoadingDialog()
            }
        })
    }

    /**
     * Generating Profile Image
     */
    fun profileImageGenerator(name: String): Bitmap {
        val size = 100 // High-resolution image
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Extract the first letter (uppercase), fallback to "?" if name is empty
        val firstLetter = name.firstOrNull()?.uppercase() ?: "?"

        // Generate a unique color using HSL for a better color spread
        val hash = name.hashCode()
        val hue = (hash % 360).toFloat() // Convert hash to a valid hue (0-360°)
        val color = Color.HSVToColor(floatArrayOf(hue, 0.7f, 0.8f)) // Vibrant color

        // Draw a perfect circular background with the generated color
        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Set text properties
        paint.color = Color.WHITE
        paint.textSize = size / 2.5f // Adjusted for better visibility
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)

        // Calculate text position for perfect centering
        val xPos = size / 2f
        val yPos = (size / 2 - (paint.descent() + paint.ascent()) / 2)

        // Draw the letter in the center
        canvas.drawText(firstLetter, xPos, yPos, paint)

        return bitmap
    }

    override fun onDeleteClick(customerId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val customerReference =
            databaseReference.child("users").child(currentUser.uid).child("customer")
        customerReference.child(customerId).removeValue().addOnSuccessListener {
            Toast.makeText(requireContext(), "Customer deleted successfully", Toast.LENGTH_LONG)
                .show()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to delete customer", Toast.LENGTH_LONG).show()
        }
    }

    private fun showLoadingDialog() {
        if (loadingDialog == null) {
            loadingDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
            loadingDialog?.titleText = "Loading..."
            loadingDialog?.setCancelable(false)
        }
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
    }

    fun capitalizeFirstLetter(text: String): String {
        if (text.isEmpty()) return ""

        val textList = text.split(" ").filter { it.isNotEmpty() }
        val changedText = textList.joinToString(" ") {
            it.lowercase().replaceFirstChar { char -> char.uppercaseChar() }
        }
        return changedText
    }
}
