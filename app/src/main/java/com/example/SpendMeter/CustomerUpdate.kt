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
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.SpendMeter.databinding.ActivityCustomerUpdateBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs

class CustomerUpdate : AppCompatActivity() {

    private val binding: ActivityCustomerUpdateBinding by lazy {
        ActivityCustomerUpdateBinding.inflate(layoutInflater)
    }
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private var customerId: String? = null
    lateinit var getGiveCustomerArray: MutableList<Int>
    private var customerName: String? = null
    private var amountListener: ValueEventListener? = null
    private lateinit var dropDown: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        databaseReference = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()
        recyclerView = binding.rv
        recyclerView.layoutManager = LinearLayoutManager(this)
        dropDown = binding.dropdownMenu
        val currentUser = auth.currentUser


        // creating drop down menu
        val dropDownList = listOf("Any Time", "Today", "This Week", "This Month")
        val arrayAdapter = ArrayAdapter(this, R.layout.selected_drop_down_item, dropDownList)
        arrayAdapter.setDropDownViewResource(R.layout.spinner_list)
        dropDown.adapter = arrayAdapter
        dropDown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                try {
                    getGiveCustomerArray =
                        fetchCustomerData(position) as MutableList<Int> // Fetch customer data using the customerId
                } catch (e: Exception) {
                    Log.e("CustomerUpdate", "Error initializing activity: ${e.message}", e)
                    Toast.makeText(
                        this@CustomerUpdate,
                        "Error loading data: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                try {
                    getGiveCustomerArray =
                        fetchCustomerData(0) as MutableList<Int> // Fetch customer data using the customerId
                } catch (e: Exception) {
                    Log.e("CustomerUpdate", "Error initializing activity: ${e.message}", e)
                    Toast.makeText(
                        this@CustomerUpdate,
                        "Error loading data: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }


        // Retrieve customerId from intent
        customerId = intent.getStringExtra("customerId")
        if (customerId.isNullOrEmpty()) {
            finish() // Close the activity if no customerId is passed
            return
        }


        // Delete Customer
        binding.deleteButton.setOnClickListener {
            SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Are you sure?")
                .setContentText("This action is irreversible!")
                .setConfirmText("Yes, delete it!")
                .setConfirmClickListener {
                    changeGetGiveAmount(
                        databaseReference.child("users").child(currentUser?.uid.toString()),
                        customerId, getGiveCustomerArray
                    )

                    deleteCustomer(customerId)
                    it.dismissWithAnimation()
                }
                .show()
        }

        // Rename Customer
        binding.cardView.setOnClickListener {
            if (customerId.isNullOrEmpty()) {
                Toast.makeText(this, "Customer ID is missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, EditUserName::class.java)
            intent.putExtra("userName", customerName ?: "")
            intent.putExtra("CustomerId", customerId)
            intent.putExtra("isCustomer", true)
            startActivity(intent)
        }

        // Navigate to 'You Gave' screen
        binding.youGave.setOnClickListener {
            startActivity(Intent(this, EnterAmount::class.java).apply {
                putExtra("customerId", customerId) // Pass customerId when navigating
            })
        }

        // Navigate to 'You Got' screen
        binding.youGot.setOnClickListener {
            startActivity(Intent(this, EnterAmountGet::class.java).apply {
                putExtra("customerId", customerId) // Pass customerId when navigating
            })
        }
    }

    private fun fetchCustomerData(position: Int): Any {
        val currentUser = auth.currentUser
        val getGiveCustomerArray: MutableList<Int>

        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return 0
        }

        val customerRef = databaseReference.child("users")
            .child(currentUser.uid)
            .child("customer")
            .child(customerId!!)


        customerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    customerName = snapshot.child("name").getValue(String::class.java)
                    val imageView = binding.profileImage
                    val bitmap =
                        profileImageGenerator(customerName.toString()) // Generate the profile image
                    imageView.setImageBitmap(bitmap) // Set the bitmap in ImageView
                    binding.CustomerName.text = capitalizeFirstLetter(customerName.toString())
                    fetchAmountList(position) // Proceed to fetch amount list for this customer
                } else {
                    Toast.makeText(
                        this@CustomerUpdate,
                        "Customer data not found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CustomerUpdate", "Error fetching customer: ${error.message}")
                Toast.makeText(
                    this@CustomerUpdate,
                    "Database error: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        val amountChangeRef =
            databaseReference.child("users").child(currentUser.uid.toString()).child("customer")
                .child(customerId!!)
        // Start listening for getAmount and giveAmount changes
        getGiveCustomerArray = listenForAmountChanges(amountChangeRef)

        return getGiveCustomerArray
    }


    private fun listenForAmountChanges(userRef: DatabaseReference): MutableList<Int> {

        val getGiveCustomerArray = mutableListOf<Int>()

        userRef.child("getCustomerAmount").addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                val getAmount = snapshot.getValue(Long::class.java)?.toInt() ?: 0
                binding.amountYouWillGet.text = "₹$getAmount"
                getGiveCustomerArray.add(getAmount)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error fetching getAmount: ${error.message}")
            }
        })

        userRef.child("giveCustomerAmount").addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                val giveAmount = snapshot.getValue(Long::class.java)?.toInt() ?: 0
                binding.amountYouWillGive.text = "₹$giveAmount"
                getGiveCustomerArray.add(giveAmount)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error fetching giveAmount: ${error.message}")
            }
        })

        userRef.child("totalAmount").addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                val totalAmount = snapshot.getValue(Long::class.java)?.toInt() ?: 0
                binding.amountTotal.text = "₹${abs(totalAmount)}"
                binding.amountTotal.setTextColor(if (totalAmount >= 0) Color.GREEN else Color.RED)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Error fetching giveAmount: ${error.message}")
            }
        })

        return getGiveCustomerArray
    }

    fun capitalizeFirstLetter(text: String): String {
        if (text.isEmpty()) return ""

        val textList = text.split(" ").filter { it.isNotEmpty() }
        val changedText = textList.joinToString(" ") {
            it.lowercase().replaceFirstChar { char -> char.uppercaseChar() }
        }
        return changedText
    }

    /**
     * Generating Profile Image
     */
    fun profileImageGenerator(name: String): Bitmap {
        val size = 120 // High-resolution image
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

    private fun fetchAmountList(position: Int) {
        if (customerId.isNullOrEmpty()) {
            Toast.makeText(this, "Customer ID is null or empty", Toast.LENGTH_SHORT).show()
            return
        }

        val amountReference = databaseReference.child("users")
            .child(auth.currentUser!!.uid)
            .child("customer")
            .child(customerId!!)
            .child("amountList")

        amountListener = amountReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val amountList = mutableListOf<AmountModel>()
                for (amountSnapshot in snapshot.children) {
                    val amount = amountSnapshot.getValue(AmountModel::class.java)
                    if (position == 0) // any time
                        amount?.let { amountList.add(it) }
                    else if (position == 1) { // today
                        amount?.let {
                            if (isDateToday(parseDate(it.date)))
                                amountList.add(it)
                        }
                    } else if (position == 2) { // this week
                        amount?.let {
                            if (isDateInCurrentWeek(parseDate(it.date)))
                                amountList.add(it)
                        }
                    } else if (position == 3) { // this Month
                        amount?.let {
                            if (isDateInCurrentMonth(parseDate(it.date)))
                                amountList.add(it)
                        }
                    }
                }

                amountList.reverse()
                val adapter = AmountAdapter(amountList, customerId!!)
                recyclerView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CustomerUpdate", "Error fetching amounts: ${error.message}")
            }
        })
    }

    private fun deleteCustomer(customerId: String?) {
        if (customerId.isNullOrEmpty()) {
            Toast.makeText(this, "Customer ID is null or empty", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val customerRef = databaseReference.child("users")
                .child(user.uid)
                .child("customer")
                .child(customerId)
            customerRef.removeValue()
                .addOnSuccessListener {
                    SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("Deleted!")
                        .setContentText("Customer deleted successfully!")
                        .setConfirmText("OK")
                        .setConfirmClickListener {
                            finish() // Close the activity after deleting the customer
                            it.dismissWithAnimation()
                        }
                        .show()
                }
                .addOnFailureListener { e ->
                    Log.e("CustomerUpdate", "Error deleting customer: ${e.message}")
                    Toast.makeText(
                        this,
                        "Failed to delete customer: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } ?: run {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    fun parseDate(dateString: String?): LocalDate? = dateString?.let {
        try {
            LocalDate.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } catch (_: DateTimeParseException) {
            println("Error parsing date: $it. Invalid format.")
            null
        }
    }


    fun isDateToday(date: LocalDate?): Boolean {
        return date == LocalDate.now()
    }

    fun isDateInCurrentWeek(date: LocalDate?): Boolean {
        val currentDate = LocalDate.now()
        val weekFields = WeekFields.of(Locale.getDefault())

        val currentWeek = currentDate.get(weekFields.weekOfWeekBasedYear())
        val currentYear = currentDate.year

        val dateWeek = date?.get(weekFields.weekOfWeekBasedYear())
        val dateYear = date?.year

        return currentWeek == dateWeek && currentYear == dateYear
    }

    fun isDateInCurrentMonth(date: LocalDate?): Boolean {
        val currentDate = LocalDate.now()

        val currentMonth = currentDate.month
        val currentYear = currentDate.year

        val dateMonth = date?.month
        val dateYear = date?.year

        return currentMonth == dateMonth && currentYear == dateYear
    }

    fun changeGetGiveAmount(
        getGiveRef: DatabaseReference,
        customerId: String?,
        getGiveCustomerList: MutableList<Int>
    ) {
        if (customerId == null) {
            Toast.makeText(this, "Customer ID is null", Toast.LENGTH_SHORT).show()
            return
        }
        var getAmount = 0
        var giveAmount = 0

        getGiveRef.child("getAmount")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    getAmount = snapshot.getValue(Long::class.java)?.toInt() ?: 0

                    getGiveRef.child("giveAmount")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                giveAmount = snapshot.getValue(Long::class.java)?.toInt() ?: 0

                                // 🔥 Final Calculations
                                val totalReceived = getAmount - getGiveCustomerList[0]
                                val totalGiven = giveAmount - getGiveCustomerList[1]

                                // ✅ Updating Firebase
                                getGiveRef.child("getAmount").setValue(totalReceived)
                                getGiveRef.child("giveAmount").setValue(totalGiven)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e(
                                    "FirebaseError",
                                    "Error fetching giveAmount: ${error.message}"
                                )
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Error fetching getAmount: ${error.message}")
                }
            })
    }


    override fun onDestroy() {
        super.onDestroy()
        // Remove the ValueEventListener when the activity is destroyed to avoid memory leaks
        amountListener?.let {
            val amountReference = databaseReference.child("users")
                .child(auth.currentUser!!.uid)
                .child("customer")
                .child(customerId ?: "")
                .child("amountList")
            amountReference.removeEventListener(it)
        }
    }
}