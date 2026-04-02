package com.arijit.budgettracker.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arijit.budgettracker.R
import com.arijit.budgettracker.models.DailyExpense

class HistoryAdapter : ListAdapter<DailyExpense, HistoryAdapter.HistoryViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DailyExpense>() {
            override fun areItemsTheSame(oldItem: DailyExpense, newItem: DailyExpense): Boolean {
                return oldItem.date == newItem.date
            }

            override fun areContentsTheSame(oldItem: DailyExpense, newItem: DailyExpense): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewDate: TextView = itemView.findViewById(R.id.date)
        val expenseRecyclerView: RecyclerView = itemView.findViewById(R.id.expense_rv)
        val totalAmount: TextView? = itemView.findViewById(R.id.total_amount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.hist_item, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val dailyExpense = getItem(position)
        holder.textViewDate.text = dailyExpense.date

        if (holder.expenseRecyclerView.layoutManager == null) {
            holder.expenseRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
            holder.expenseRecyclerView.isNestedScrollingEnabled = false
        }

        val innerAdapter = holder.expenseRecyclerView.adapter as? ExpenseAdapter ?: ExpenseAdapter()
        holder.expenseRecyclerView.adapter = innerAdapter
        innerAdapter.submitList(dailyExpense.expenses)

        // Nếu hist_item.xml có total_amount thì set ở đây
        holder.totalAmount?.text = ""
    }
}