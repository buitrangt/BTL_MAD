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
import java.util.Calendar
import java.util.Locale
import android.text.Editable
import android.text.TextWatcher
import android.app.DatePickerDialog
import android.util.Log
import android.widget.ImageButton

class HistoryFragment : Fragment() {
    private lateinit var viewModel: HomeViewModel
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var containerRv: RecyclerView
    private lateinit var apiService: ApiService
    private lateinit var etSearchTransaction: android.widget.EditText
    private lateinit var btnDatePicker: ImageButton
    private var selectedDate: Long = System.currentTimeMillis()
    private var isDateSelected: Boolean = false
    
    private lateinit var tvMonthIncome: android.widget.TextView
    private lateinit var tvMonthExpense: android.widget.TextView
    private lateinit var tvMonthSavings: android.widget.TextView
    private lateinit var tvMonthLabelExpense: android.widget.TextView
    private lateinit var tvMonthLabelIncome: android.widget.TextView
    private lateinit var tvMonthLabelSavings: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return try {
            val view = inflater.inflate(R.layout.fragment_history, container, false)
            
            // Initialize views
            tvMonthIncome = view.findViewById(R.id.tvMonthIncome)
            tvMonthExpense = view.findViewById(R.id.tvMonthExpense)
            tvMonthSavings = view.findViewById(R.id.tvMonthSavings)
            tvMonthLabelExpense = view.findViewById(R.id.tvMonthLabelExpense)
            tvMonthLabelIncome = view.findViewById(R.id.tvMonthLabelIncome)
            tvMonthLabelSavings = view.findViewById(R.id.tvMonthLabelSavings)
            etSearchTransaction = view.findViewById(R.id.etSearchTransaction)
            btnDatePicker = view.findViewById(R.id.btnDatePicker)
            containerRv = view.findViewById(R.id.rvHistory)
            
            // Setup date picker button
            btnDatePicker.setOnClickListener {
                if (isDateSelected) {
                    // If date is selected, clicking X clears the filter
                    resetToCurrentMonth()
                } else {
                    // Otherwise, open date picker
                    showDatePicker()
                }
            }
            
            // Initialize apiService here when context is available
            apiService = RetrofitClient.getApiService(requireContext())
            
            // Sync expenses from API to local database
            val syncManager = SyncManager(requireContext())
            lifecycleScope.launch {
                syncManager.syncExpensesFromAPI()
            }
            
            historyAdapter = HistoryAdapter()
            containerRv.layoutManager = LinearLayoutManager(requireContext())
            containerRv.adapter = historyAdapter

            viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
            
            // Initial observer - ONLY ONE for allExpenses
            viewModel.allExpenses.observe(viewLifecycleOwner) { expenses ->
                refreshTransactionList(expenses)
            }
            
            // Update month labels on first load
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            updateMonthLabels(calendar.timeInMillis)
            
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
                    val currentExpenses = viewModel.allExpenses.value ?: emptyList()
                    
                    if (keyword.isEmpty()) {
                        // Show all expenses (or filtered by date if selected)
                        refreshTransactionList(currentExpenses)
                    } else {
                        // Search from API
                        lifecycleScope.launch {
                            try {
                                val response = apiService.searchTransactions(keyword)
                                if (response.isSuccessful && response.body() != null) {
                                    val transactions = response.body()!!.map { tx ->
                                        Expense(
                                            id = tx.id.toInt(),
                                            amount = tx.amount.toDouble(),
                                            name = tx.name,
                                            category = tx.categoryName ?: tx.name,
                                            type = tx.type,
                                            timeStamp = tx.timeStamp,
                                            synced = true
                                        )
                                    }
                                    lifecycleScope.launch {
                                        val grouped = withContext(Dispatchers.Default) {
                                            groupExpensesByDate(transactions)
                                        }
                                        withContext(Dispatchers.Main) {
                                            historyAdapter.submitList(grouped)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            })

            view
        } catch (e: Exception) {
            Log.e("HistoryFragment", "Error in onCreateView: ${e.message}", e)
            e.printStackTrace()
            null
        }
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

    private fun refreshTransactionList(expenses: List<Expense>) {
        // Filter by selected date if date is selected
        val filtered = if (isDateSelected) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDate
            
            val startOfDay = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis / 1000
            
            val endOfDay = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis / 1000
            
            expenses.filter { it.timeStamp >= startOfDay && it.timeStamp <= endOfDay }
        } else {
            expenses
        }
        
        lifecycleScope.launch {
            val grouped = withContext(Dispatchers.Default) {
                groupExpensesByDate(filtered)
            }
            withContext(Dispatchers.Main) {
                historyAdapter.submitList(grouped)
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDate
        
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // Create calendar for selected date
                val selectedCal = Calendar.getInstance()
                selectedCal.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                selectedDate = selectedCal.timeInMillis
                isDateSelected = true
                
                // Update button icon to X
                btnDatePicker.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                
                // Update transactions for the selected date
                val allExpenses = viewModel.allExpenses.value ?: emptyList()
                refreshTransactionList(allExpenses)
                
                // Update statistics for the selected month
                updateStatsForSelectedMonth()
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun updateMonthLabels(monthStartTime: Long) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = monthStartTime
        
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        val monthText = "Tháng $month/$year"
        
        tvMonthLabelExpense.text = monthText
        tvMonthLabelIncome.text = monthText
        tvMonthLabelSavings.text = monthText
    }

    private fun updateStatsForSelectedMonth() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDate
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        
        val (income, expense, savings) = viewModel.getMonthlyStats(calendar.timeInMillis)
        
        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
        tvMonthIncome.text = "${formatter.format(income.toLong())} đ"
        tvMonthExpense.text = "${formatter.format(expense.toLong())} đ"
        tvMonthSavings.text = "${formatter.format(savings.toLong())} đ"
        
        // Update month labels with specific month and year
        updateMonthLabels(calendar.timeInMillis)
    }

    private fun resetToCurrentMonth() {
        selectedDate = System.currentTimeMillis()
        isDateSelected = false
        etSearchTransaction.setText("")
        
        // Update button icon back to calendar
        btnDatePicker.setImageResource(android.R.drawable.ic_menu_my_calendar)
        
        // Refresh to show all expenses
        val allExpenses = viewModel.allExpenses.value ?: emptyList()
        refreshTransactionList(allExpenses)
        
        // Reset stats to current month
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        
        val (income, expense, savings) = viewModel.getMonthlyStats(calendar.timeInMillis)
        
        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
        tvMonthIncome.text = "${formatter.format(income.toLong())} đ"
        tvMonthExpense.text = "${formatter.format(expense.toLong())} đ"
        tvMonthSavings.text = "${formatter.format(savings.toLong())} đ"
        
        // Update month labels with current month
        updateMonthLabels(calendar.timeInMillis)
    }
}
