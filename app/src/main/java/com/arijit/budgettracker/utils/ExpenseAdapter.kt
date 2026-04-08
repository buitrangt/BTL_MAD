package com.arijit.budgettracker.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arijit.budgettracker.R
import com.arijit.budgettracker.db.Expense
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseAdapter(
    var onItemLongClick: ((Expense) -> Unit)? = null,
    var onEditClick: ((Expense) -> Unit)? = null,
    var onDeleteClick: ((Expense) -> Unit)? = null
) : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Expense>() {
            override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewCategory: TextView = itemView.findViewById(R.id.textViewCategory)
        private val textViewAmount: TextView = itemView.findViewById(R.id.textViewAmount)

        fun bind(expense: Expense) {
            textViewCategory.text = expense.category

            val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
            val formattedAmount = formatter.format(expense.amount.toLong())

            if (expense.type == "INCOME") {
                textViewAmount.text = "+$formattedAmount đ"
                textViewAmount.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark))
            } else {
                textViewAmount.text = "-$formattedAmount đ"
                textViewAmount.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark))
            }

            itemView.setOnLongClickListener {
                onItemLongClick?.invoke(expense)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
