package com.example.SpendMeter

import android.annotation.SuppressLint
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
import androidx.viewpager2.widget.ViewPager2
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.SpendMeter.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PageAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private var username: String? = ""

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val currentUserUid = auth.currentUser?.uid
        if (currentUserUid != null) {
            databaseRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserUid)
            setupUsernameListener()
            listenForAmountChanges(databaseRef)
        } else {
            binding.userName.text = "Guest"
            Log.e("MainActivity", "Current user is null.")
        }

        setupSignOut()
        setupEditUserNameNavigation()
        setupViewPagerAndTabs()
    }

    private fun setupUsernameListener() {
        databaseRef.child("UserName").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                username = snapshot.getValue(String::class.java)
                username?.let {
                    binding.profileImage.setImageBitmap(profileImageGenerator(it))
                    binding.userName.text = capitalizeFirstLetter(it)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun setupSignOut() {
        binding.SignOut.setOnClickListener {
            SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Are you sure?")
                .setContentText("You will be signed out of your account.")
                .setConfirmText("Yes, Sign Out!")
                .setConfirmClickListener { sDialog ->
                    auth.signOut()
                    sDialog.dismissWithAnimation()
                    SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("Signed Out!")
                        .setContentText("You have successfully signed out.")
                        .setConfirmText("OK")
                        .setConfirmClickListener { successDialog ->
                            successDialog.dismissWithAnimation()
                            startActivity(Intent(this, Login::class.java))
                            finish()
                        }.show()
                }.show()
        }
    }

    private fun setupEditUserNameNavigation() {
        binding.cardView.setOnClickListener {
            val intent = Intent(this, EditUserName::class.java)
            intent.putExtra("userName", username)
            intent.putExtra("idCustomer", true)
            startActivity(intent)
        }
    }

    /**
     * Listens for real-time changes in getAmount and giveAmount and updates UI dynamically.
     */
    private fun listenForAmountChanges(userRef: DatabaseReference) {
        userRef.child("getAmount").addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                val getAmount = snapshot.getValue(Long::class.java)?.toInt() ?: 0
                binding.youWillGetAmount.text = "₹$getAmount"
                updateTotalAmount()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error fetching getAmount: ${error.message}")
            }
        })

        userRef.child("giveAmount").addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                val giveAmount = snapshot.getValue(Long::class.java)?.toInt() ?: 0
                binding.youWillGiveAmount.text = "₹$giveAmount"
                updateTotalAmount()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error fetching giveAmount: ${error.message}")
            }
        })
    }

    /**
     * Calculates and updates the total amount dynamically.
     */
    @SuppressLint("SetTextI18n")
    private fun updateTotalAmount() {
        val getAmount =
            binding.youWillGetAmount.text.toString().replace("₹", "").trim().toIntOrNull() ?: 0
        val giveAmount =
            binding.youWillGiveAmount.text.toString().replace("₹", "").trim().toIntOrNull() ?: 0
        val totalAmount = getAmount - giveAmount

        binding.totalAmount.text = "₹${abs(totalAmount)}"
        binding.totalAmount.setTextColor(
            when {
                totalAmount > 0 -> Color.parseColor("#2ECC71") // Green Shade
                totalAmount < 0 -> Color.parseColor("#FF5C5C") // Custom Red
                else -> Color.parseColor("#212121") // Dark Gray/Black
            }
        )
    }

    /**
     * Generating Profile Image
     */
    private fun profileImageGenerator(name: String): Bitmap {
        val size = 100
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val firstLetter = name.firstOrNull()?.uppercase() ?: "?"
        val hash = name.hashCode()
        val hue = (hash % 360).toFloat()
        val color = Color.HSVToColor(floatArrayOf(hue, 0.7f, 0.8f))

        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        paint.color = Color.WHITE
        paint.textSize = size / 2.5f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)

        val xPos = size / 2f
        val yPos = (size / 2 - (paint.descent() + paint.ascent()) / 2)
        canvas.drawText(firstLetter, xPos, yPos, paint)

        return bitmap
    }

    private fun capitalizeFirstLetter(text: String): String {
        if (text.isEmpty()) return ""
        return text.split(" ")
            .filter { it.isNotEmpty() }
            .joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.uppercaseChar() } }
    }

    /**
     * Sets up ViewPager and Tabs for navigation.
     */
    private fun setupViewPagerAndTabs() {
        adapter = PageAdapter(supportFragmentManager, lifecycle)
        binding.viewPager.adapter = adapter

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let { binding.viewPager.currentItem = it.position }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(position))
            }
        })
    }
}