package com.example.SpendMeter.Customers.AllCustomers

import android.graphics.Bitmap

data class CustomerModel(var name : String, val customerId : String, var profilePhoto: Bitmap?){
    constructor():this("","", null)
}