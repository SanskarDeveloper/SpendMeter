package com.example.SpendMeter


data class AmountModel(var reason: String, var spend: Int, val flag: Boolean, val date: String, val time: String, val amountId: String){
    constructor():this("",0, false, "", "", "")
}