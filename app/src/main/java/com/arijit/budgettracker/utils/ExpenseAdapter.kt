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
        val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
        val txtCategoryTime: TextView = itemView.findViewById(R.id.txtCategoryTime)
        val txtAmount: TextView = itemView.findViewById(R.id.txtAmount)
        val btnEdit: android.widget.Button? = itemView.findViewById(R.id.btnEdit)
        val btnDelete: android.widget.Button? = itemView.findViewById(R.id.btnDelete)

        fun bind(expense: Expense) {
            // Display transaction name in bold
            txtTitle.text = expense.name

            val timeFormat = SimpleDateFormat("HH:mm", Locale("vi", "VN"))
            // timeStamp from DB is in SECONDS, Date() expects MILLISECONDS
            val timeText = timeFormat.format(Date(expense.timeStamp * 1000))
            // Display category and time below
            txtCategoryTime.text = "${expense.category} • $timeText"

            val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
            val formattedAmount = formatter.format(expense.amount.toLong())

            if (expense.type == "INCOME") {
                txtAmount.text = "+$formattedAmount đ"
                txtAmount.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark))
            } else {
                txtAmount.text = "-$formattedAmount đ"
                txtAmount.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark))
            }

            itemView.setOnLongClickListener {
                showPopupMenu(itemView, expense)
                true
            }

            btnEdit?.setOnClickListener {
                onEditClick?.invoke(expense)
            }

            btnDelete?.setOnClickListener {
                onDeleteClick?.invoke(expense)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
