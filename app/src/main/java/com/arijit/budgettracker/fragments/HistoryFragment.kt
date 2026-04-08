package com.arijit.budgettracker.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.arijit.budgettracker.R
import com.arijit.budgettracker.api.ApiService
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.api.TransactionResponse
import com.arijit.budgettracker.db.Expense
import com.arijit.budgettracker.models.DailyExpense
import com.arijit.budgettracker.utils.HistoryAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var containerRv: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
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
    
    // Local-first: keep a snapshot for filtering (search/date)
    private var allLocalExpenses: List<Expense> = emptyList()
    private var displayedTransactions: List<DailyExpense> = emptyList()
    
    // Search job for cancellation
    private var searchJob: Job? = null
    
    // Cache SimpleDateFormat at fragment level (thread-local within fragment)
    private val dateFormatter by lazy { SimpleDateFormat("dd MMMM, yyyy", Locale("vi", "VN")) }
    
    // Cache NumberFormat at fragment level
    private val numberFormatter by lazy { NumberFormat.getInstance(Locale("vi", "VN")) }
    
    // Loading state
    private var isProcessing = false
    
    // TextWatcher as member to avoid recreation and enable removal
    private lateinit var searchTextWatcher: TextWatcher
    private var hasInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize TextWatcher
        searchTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val keyword = s.toString().trim()
                applyLocalFilters(keyword = keyword)
            }
        }
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
            
            // Setup SwipeRefreshLayout for pull-to-refresh
            swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
            swipeRefreshLayout.setOnRefreshListener {
                lifecycleScope.launch {
                    try {
                        com.arijit.budgettracker.utils.SyncManager.syncIfOnline(requireContext().applicationContext)
                    } catch (_: Exception) {
                        // ignore
                    } finally {
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
            
            // Setup date picker button
            btnDatePicker.setOnClickListener {
                if (isDateSelected) {
                    resetToCurrentMonth()
                } else {
                    showDatePicker()
                }
            }
            
            // Initialize apiService
            apiService = RetrofitClient.getApiService(requireContext())
            
            // Setup adapter
            historyAdapter = HistoryAdapter().apply {
                onExpenseEditClick = { expense ->
                    val intent = Intent(requireContext(), com.arijit.budgettracker.AddTransActivity::class.java)
                    intent.putExtra("expenseId", expense.id)
                    expense.remoteId?.let { intent.putExtra("remoteId", it) }
                    intent.putExtra("type", expense.type)
                    intent.putExtra("category", expense.category)
                    intent.putExtra("name", expense.name)
                    intent.putExtra("amount", expense.amount)
                    intent.putExtra("note", expense.note)
                    intent.putExtra("timeStamp", expense.timeStamp)
                    startActivity(intent)
                }
                onExpenseDeleteClick = { expense ->
                    AlertDialog.Builder(requireContext())
                        .setTitle("Xóa giao dịch")
                        .setMessage("Bạn có chắc muốn xóa giao dịch này?")
                        .setPositiveButton("Xóa") { _, _ ->
                            lifecycleScope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        val db = com.arijit.budgettracker.db.ExpenseDatabase.getDatabase(requireContext().applicationContext)
                                        db.expenseDao().deleteExpense(expense)
                                    }
                                    // Best-effort server delete.
                                    com.arijit.budgettracker.utils.SyncManager.deleteExpenseIfOnline(requireContext().applicationContext, expense)
                                } catch (_: Exception) {
                                    // ignore
                                }
                            }
                        }
                        .setNegativeButton("Hủy", null)
                        .show()
                }
            }
            val layoutManager = LinearLayoutManager(requireContext())
            containerRv.layoutManager = layoutManager
            containerRv.adapter = historyAdapter

            // Observe local DB so list updates immediately on add/edit/delete.
            observeLocalDb()
            
            // Update month labels on first load
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            updateMonthLabels(calendar.timeInMillis)

            // Search functionality - use member TextWatcher
            etSearchTransaction.addTextChangedListener(searchTextWatcher)

            view
        } catch (e: Exception) {
            Log.e("HistoryFragment", "Error in onCreateView: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset any search/date filters when returning to History tab.
        // (ViewPager keeps fragments alive; without this, filters "stick".)
        if (hasInitialized) {
            resetToDefaultState()
        } else {
            hasInitialized = true
        }

        // Best-effort sync, but UI is driven by local DB.
        lifecycleScope.launch {
            try {
                com.arijit.budgettracker.utils.SyncManager.syncIfOnline(requireContext().applicationContext)
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    private fun resetToDefaultState() {
        searchJob?.cancel()
        searchJob = null

        selectedDate = System.currentTimeMillis()
        isDateSelected = false
        btnDatePicker.setImageResource(android.R.drawable.ic_menu_my_calendar)

        // Clear search without triggering afterTextChanged cascade repeatedly.
        etSearchTransaction.removeTextChangedListener(searchTextWatcher)
        etSearchTransaction.setText("")
        etSearchTransaction.addTextChangedListener(searchTextWatcher)

        applyLocalFilters(keyword = "")

        // Reset month labels + stats to current month.
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        updateMonthLabels(calendar.timeInMillis)
        updateLocalMonthStats(allLocalExpenses)
    }

    private fun observeLocalDb() {
        val db = com.arijit.budgettracker.db.ExpenseDatabase.getDatabase(requireContext().applicationContext)
        val dao = db.expenseDao()
        viewLifecycleOwner.lifecycleScope.launch {
            dao.getAllExpensesFlow().collect { expenses ->
                allLocalExpenses = expenses
                applyLocalFilters(keyword = etSearchTransaction.text?.toString()?.trim().orEmpty())
                updateLocalMonthStats(expenses)
            }
        }
    }

    private fun applyLocalFilters(keyword: String) {
        val filtered = allLocalExpenses
            .asSequence()
            .filter { e ->
                if (keyword.isBlank()) true
                else {
                    val k = keyword.lowercase()
                    e.category.lowercase().contains(k) ||
                        e.name.lowercase().contains(k) ||
                        e.note.lowercase().contains(k)
                }
            }
            .filter { e ->
                if (!isDateSelected) true
                else {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = selectedDate
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis
                    val end = start + 24 * 60 * 60 * 1000
                    e.timeStamp in start until end
                }
            }
            .sortedByDescending { it.timeStamp }
            .toList()

        val grouped = groupExpensesByDate(filtered)
        displayedTransactions = grouped
        historyAdapter.submitList(grouped)
    }

    private fun updateLocalMonthStats(expenses: List<Expense>) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        val monthIncome = expenses.asSequence()
            .filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.timeStamp }
                c.get(Calendar.YEAR) == currentYear && c.get(Calendar.MONTH) == currentMonth && it.type.equals("income", true)
            }
            .sumOf { it.amount }

        val monthExpense = expenses.asSequence()
            .filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.timeStamp }
                c.get(Calendar.YEAR) == currentYear && c.get(Calendar.MONTH) == currentMonth && it.type.equals("expense", true)
            }
            .sumOf { it.amount }

        tvMonthIncome.text = "${numberFormatter.format(monthIncome.toLong())} đ"
        tvMonthExpense.text = "${numberFormatter.format(monthExpense.toLong())} đ"
        tvMonthSavings.text = "${numberFormatter.format((monthIncome - monthExpense).toLong())} đ"
    }

    private fun groupExpensesByDate(expenses: List<Expense>): List<DailyExpense> {
        // Use cached formatter (thread-local to this fragment)
        return expenses
            .groupBy { dateFormatter.format(Date(it.timeStamp)) }
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

                applyLocalFilters(keyword = etSearchTransaction.text?.toString()?.trim().orEmpty())
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
        val calendarSelected = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val selectedYear = calendarSelected.get(Calendar.YEAR)
        val selectedMonth = calendarSelected.get(Calendar.MONTH)

        val monthIncome = allLocalExpenses.asSequence()
            .filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.timeStamp }
                c.get(Calendar.YEAR) == selectedYear && c.get(Calendar.MONTH) == selectedMonth && it.type.equals("income", true)
            }
            .sumOf { it.amount }

        val monthExpense = allLocalExpenses.asSequence()
            .filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.timeStamp }
                c.get(Calendar.YEAR) == selectedYear && c.get(Calendar.MONTH) == selectedMonth && it.type.equals("expense", true)
            }
            .sumOf { it.amount }

        tvMonthIncome.text = "${numberFormatter.format(monthIncome.toLong())} đ"
        tvMonthExpense.text = "${numberFormatter.format(monthExpense.toLong())} đ"
        tvMonthSavings.text = "${numberFormatter.format((monthIncome - monthExpense).toLong())} đ"

        updateMonthLabels(selectedDate)
    }

    private fun resetToCurrentMonth() {
        selectedDate = System.currentTimeMillis()
        isDateSelected = false
        
        // Update button icon back to calendar
        btnDatePicker.setImageResource(android.R.drawable.ic_menu_my_calendar)

        // Clear search without triggering afterTextChanged cascade
        etSearchTransaction.removeTextChangedListener(searchTextWatcher)
        etSearchTransaction.setText("")
        etSearchTransaction.addTextChangedListener(searchTextWatcher)

        applyLocalFilters(keyword = "")
        updateLocalMonthStats(allLocalExpenses)
    }
}
