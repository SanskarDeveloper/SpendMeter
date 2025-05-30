package com.example.SpendMeter

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import androidx.recyclerview.widget.RecyclerView
import com.example.SpendMeter.databinding.AmountItemBinding

class AmountAdapter(private val amounts: List<AmountModel>, private val customerId: String?) :
    RecyclerView.Adapter<AmountAdapter.AmountViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AmountViewHolder {
        val binding = AmountItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AmountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AmountViewHolder, position: Int) {
        holder.bind(amounts[position])

        amin(holder.itemView)

        holder.itemView.setOnClickListener{
            val context = holder.itemView.context
            // Create an Intent to start a new Activity
            val intent = Intent(context, onClickAmount::class.java)
            intent.putExtra("AmountId", amounts[position].amountId.toString())
            intent.putExtra("CustomerId", customerId)
            intent.putExtra("Flag", amounts[position].flag)
            intent.putExtra("Reason", amounts[position].reason)
            intent.putExtra("Amount", amounts[position].spend.toString())
            intent.putExtra("Date", amounts[position].date)
            intent.putExtra("Time", amounts[position].time)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = amounts.size

    class AmountViewHolder(private val binding: AmountItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: AmountModel) {
            binding.reason.text = capitalizeFirstLetter(item.reason)

            if (item.flag) {
                binding.save.text = item.spend.toString()
            } else {
                binding.spend.text = item.spend.toString()
            }
        }
    }

    fun amin(view:View){
        val animation = AlphaAnimation(0.0f, 1.5f)
        animation.duration = 700
        view.startAnimation(animation)
    }
}

fun capitalizeFirstLetter(text: String): String {
    if (text.isEmpty()) return ""

    val textList = text.split(" ").filter { it.isNotEmpty() }
    val changedText = textList.joinToString(" ") {
        it.lowercase().replaceFirstChar { char -> char.uppercaseChar() }
    }
    return changedText
}