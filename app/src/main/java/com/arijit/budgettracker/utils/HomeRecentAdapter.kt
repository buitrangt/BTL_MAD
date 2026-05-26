package com.arijit.budgettracker.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arijit.budgettracker.R
import com.arijit.budgettracker.db.Expense
import com.arijit.budgettracker.utils.CurrencyPrefs

class HomeRecentAdapter(
    var onItemLongClick: ((Expense) -> Unit)? = null,
    var onEditClick: ((Expense) -> Unit)? = null,
    var onDeleteClick: ((Expense) -> Unit)? = null
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
            val isIncome = expense.type.equals("income", ignoreCase = true)
            val sign = if (isIncome) "+" else "-"
            tvAmount.text = "$sign${CurrencyPrefs.format(expense.amount)}"
            tvAmount.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                    itemView.context,
                    if (isIncome) android.R.color.holo_green_dark else android.R.color.holo_red_dark
                )
            )
            textViewCategory.text = expense.category
            textViewSubtitle.text = expense.note.takeIf { it.isNotBlank() } ?: "Ghi chú"
            tvDate.text = formatDate(expense.timeStamp)

            itemView.setOnLongClickListener {
                showPopupMenu(itemView, expense)
                true
            }
        }

        private fun showPopupMenu(view: View, expense: Expense) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_transaction_action, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> {
                        onEditClick?.invoke(expense)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteClick?.invoke(expense)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        private fun formatDate(timeStamp: Long): String {
            val bangkok = java.util.TimeZone.getTimeZone("Asia/Bangkok")
            val calendar = java.util.Calendar.getInstance(bangkok)
            val tsMillis = if (timeStamp in 1..9_999_999_999L) timeStamp * 1000 else timeStamp
            calendar.timeInMillis = tsMillis
            
            val today = java.util.Calendar.getInstance(bangkok)
            val yesterday = java.util.Calendar.getInstance(bangkok).apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
            
            return when {
                calendar.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR) &&
                calendar.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) -> "Hôm nay"
                calendar.get(java.util.Calendar.DAY_OF_YEAR) == yesterday.get(java.util.Calendar.DAY_OF_YEAR) &&
                calendar.get(java.util.Calendar.YEAR) == yesterday.get(java.util.Calendar.YEAR) -> "Hôm qua"
                else -> "${calendar.get(java.util.Calendar.DAY_OF_MONTH)}/${calendar.get(java.util.Calendar.MONTH) + 1}"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeRecentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_transaction, parent, false)
        return HomeRecentViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeRecentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
