package com.arijit.budgettracker.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arijit.budgettracker.R
import com.arijit.budgettracker.api.ApiService
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.api.SyncManager
import com.arijit.budgettracker.db.Expense
import com.arijit.budgettracker.models.DailyExpense
import com.arijit.budgettracker.models.HomeViewModel
import com.arijit.budgettracker.utils.HistoryAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.text.Editable
import android.text.TextWatcher

class HistoryFragment : Fragment() {
    private lateinit var viewModel: HomeViewModel
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var containerRv: RecyclerView
    private lateinit var apiService: ApiService
    private lateinit var etSearchTransaction: android.widget.EditText
    
    private lateinit var tvMonthIncome: android.widget.TextView
    private lateinit var tvMonthExpense: android.widget.TextView
    private lateinit var tvMonthSavings: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        
        // Initialize views
        tvMonthIncome = view.findViewById(R.id.tvMonthIncome)
        tvMonthExpense = view.findViewById(R.id.tvMonthExpense)
        tvMonthSavings = view.findViewById(R.id.tvMonthSavings)
        etSearchTransaction = view.findViewById(R.id.etSearchTransaction)
        
        // Initialize apiService here when context is available
        apiService = RetrofitClient.getApiService(requireContext())
        
        // Sync expenses from API to local database
        val syncManager = SyncManager(requireContext())
        lifecycleScope.launch {
            syncManager.syncExpensesFromAPI()
        }
        
        containerRv = view.findViewById(R.id.rvHistory)
        
        historyAdapter = HistoryAdapter()
        containerRv.layoutManager = LinearLayoutManager(requireContext())
        containerRv.adapter = historyAdapter

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        viewModel.allExpenses.observe(viewLifecycleOwner) { expenses ->
            lifecycleScope.launch {
                val grouped = withContext(Dispatchers.Default) {
                    groupExpensesByDate(expenses)
                }
                withContext(Dispatchers.Main) {
                    historyAdapter.submitList(grouped)
                }
            }
        }
        
        // Observe monthly totals
        viewModel.monthIncome.observe(viewLifecycleOwner) { income ->
            val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
            tvMonthIncome.text = "${formatter.format(income.toLong())} đ"
        }
        
        viewModel.monthExpense.observe(viewLifecycleOwner) { expense ->
            val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
            tvMonthExpense.text = "${formatter.format(expense.toLong())} đ"
        }
        
        viewModel.monthSavings.observe(viewLifecycleOwner) { savings ->
            val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
            tvMonthSavings.text = "${formatter.format(savings.toLong())} đ"
        }

        // Search functionality
        etSearchTransaction.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val keyword = s.toString().trim()
                if (keyword.isEmpty()) {
                    // Reset to all expenses
                    viewModel.allExpenses.observe(viewLifecycleOwner) { expenses ->
                        lifecycleScope.launch {
                            val grouped = withContext(Dispatchers.Default) {
                                groupExpensesByDate(expenses)
                            }
                            withContext(Dispatchers.Main) {
                                historyAdapter.submitList(grouped)
                            }
                        }
                    }
                } else {
                    // Search from API
                    lifecycleScope.launch {
                        try {
                            val response = apiService.searchTransactions(keyword)
                            if (response.isSuccessful && response.body() != null) {
                                val transactions = response.body()!!.map { tx ->
                                    com.arijit.budgettracker.db.Expense(
                                        id = tx.id.toInt(),
                                        amount = tx.amount.toDouble(),
                                        name = tx.name,  // Transaction name
                                        category = tx.categoryName ?: tx.name,  // Category name
                                        type = tx.type,
                                        timeStamp = tx.timeStamp,
                                        synced = true
                                    )
                                }
                                val grouped = withContext(Dispatchers.Default) {
                                    groupExpensesByDate(transactions)
                                }
                                withContext(Dispatchers.Main) {
                                    historyAdapter.submitList(grouped)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        })

        return view
    }

    override fun onResume() {
        super.onResume()
        historyAdapter.notifyDataSetChanged()
    }


    private fun groupExpensesByDate(expenses: List<Expense>): List<DailyExpense> {
        val sdf = SimpleDateFormat("dd MMMM, yyyy", Locale("vi", "VN"))
        return expenses
            .groupBy { sdf.format(Date(it.timeStamp * 1000)) }
            .map { entry -> 
                DailyExpense(
                    entry.key, 
                    entry.value.sortedByDescending { it.timeStamp }
                ) 
            }
            .sortedByDescending { dailyExpense ->
                dailyExpense.expenses.firstOrNull()?.timeStamp ?: 0L
            }
    }
}
