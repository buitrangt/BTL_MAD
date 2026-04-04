package com.arijit.budgettracker.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arijit.budgettracker.R
import com.arijit.budgettracker.db.Expense

class HomeRecentAdapter(
    var onItemLongClick: ((Expense) -> Unit)? = null
) : ListAdapter<Expense, HomeRecentAdapter.HomeRecentViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Expense>() {
            override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean =
                oldItem == newItem
        }
    }

    inner class HomeRecentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val textViewCategory: TextView = itemView.findViewById(R.id.textViewCategory)
        private val textViewSubtitle: TextView = itemView.findViewById(R.id.textViewSubtitle)

        fun bind(expense: Expense) {
            val sign = if (expense.type == "income") "+" else "-"
            tvAmount.text = "$sign₫%.2f".format(expense.amount)
            textViewCategory.text = expense.category
            textViewSubtitle.text = expense.note.takeIf { it.isNotBlank() } ?: "Ghi chú"
            tvDate.text = formatDate(expense.timeStamp)

            itemView.setOnLongClickListener {
                onItemLongClick?.invoke(expense)
                true
            }
        }

        private fun formatDate(timeStamp: Long): String {
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = timeStamp
            val today = java.util.Calendar.getInstance()
            val yesterday = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
            
            return when {
                calendar.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR) -> "Hôm nay"
                calendar.get(java.util.Calendar.DAY_OF_YEAR) == yesterday.get(java.util.Calendar.DAY_OF_YEAR) -> "Hôm qua"
                else -> "${calendar.get(java.util.Calendar.DAY_OF_MONTH)}/${calendar.get(java.util.Calendar.MONTH) + 1}"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeRecentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_expense, parent, false)
        return HomeRecentViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeRecentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
