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
import com.arijit.budgettracker.db.Expense
import com.arijit.budgettracker.models.DailyExpense

class HistoryAdapter(
    var onExpenseEditClick: ((Expense) -> Unit)? = null,
    var onExpenseDeleteClick: ((Expense) -> Unit)? = null
) : ListAdapter<DailyExpense, HistoryAdapter.HistoryViewHolder>(DIFF_CALLBACK) {

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
        
        // Cache nested adapter to avoid recreating on every bind
        var expenseAdapter: ExpenseAdapter? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.hist_item, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val dailyExpense = getItem(position)
        holder.textViewDate.text = dailyExpense.date

        // Initialize layout manager only once
        if (holder.expenseRecyclerView.layoutManager == null) {
            holder.expenseRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
            holder.expenseRecyclerView.isNestedScrollingEnabled = false
        }

        // Reuse cached adapter (avoid recreation on every bind)
        if (holder.expenseAdapter == null) {
            holder.expenseAdapter = ExpenseAdapter()
            holder.expenseRecyclerView.adapter = holder.expenseAdapter
        }
        
        // Only update list data
        holder.expenseAdapter?.submitList(dailyExpense.expenses)
    }
}
