package com.arijit.budgettracker.fragments

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
    
    // Pagination variables
    private var currentPage = 1
    private val pageSize = 10
    private var isLoadingMore = false
    private var hasMorePages = true
    
    // Store API transactions in memory
    private var allApiTransactions: List<TransactionResponse> = emptyList()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize TextWatcher
        searchTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val keyword = s.toString().trim()
                
                // Cancel previous search job
                searchJob?.cancel()
                
                if (keyword.isEmpty()) {
                    searchJob = null
                    currentPage = 1
                    hasMorePages = true
                    // Clear adapter immediately to prevent state mismatch
                    historyAdapter.submitList(emptyList())
                    displayPagedTransactions(1)
                    // Keep stats from current month, not affected by search
                    if (isDateSelected) {
                        updateStatsForSelectedMonth()
                    } else {
                        calculateAndUpdateStats(allApiTransactions)
                    }
                } else {
                    // Search from API
                    searchJob = lifecycleScope.launch {
                        try {
                            val response = apiService.searchTransactions(keyword)
                            if (response.isSuccessful && response.body() != null) {
                                val searchResults = response.body()!!
                                
                                // Convert TransactionResponse to Expense
                                val transactions = searchResults.map { tx ->
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
                                
                                // Group by date and display search results
                                val grouped = withContext(Dispatchers.Default) {
                                    groupExpensesByDate(transactions)
                                }
                                withContext(Dispatchers.Main) {
                                    displayedTransactions = grouped
                                    historyAdapter.submitList(grouped)
                                    
                                    // Keep stats from current month, NOT affected by search keyword
                                    if (isDateSelected) {
                                        updateStatsForSelectedMonth()
                                    } else {
                                        calculateAndUpdateStats(allApiTransactions)
                                    }
                                }
                            } else {
                                Log.e("HistoryFragment", "Search failed: ${response.code()}")
                                withContext(Dispatchers.Main) {
                                    displayedTransactions = emptyList()
                                    historyAdapter.submitList(emptyList())
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("HistoryFragment", "Search failed: ${e.message}", e)
                            withContext(Dispatchers.Main) {
                                displayedTransactions = emptyList()
                                historyAdapter.submitList(emptyList())
                            }
                        }
                    }
                }
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
                currentPage = 1
                hasMorePages = true
                isLoadingMore = false
                loadTransactionsFromAPI()
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
            historyAdapter = HistoryAdapter()
            val layoutManager = LinearLayoutManager(requireContext())
            containerRv.layoutManager = layoutManager
            containerRv.adapter = historyAdapter
            
            // Setup pagination scroll listener
            setupPaginationListener(layoutManager)

            // Load transactions from API
            loadTransactionsFromAPI()
            
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
        // Always reload to sync with any new data from AddExpenseActivity
        loadTransactionsFromAPI()
    }

    private fun groupExpensesByDate(expenses: List<Expense>): List<DailyExpense> {
        // Use cached formatter (thread-local to this fragment)
        return expenses
            .groupBy { dateFormatter.format(Date(it.timeStamp * 1000)) }
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

    private fun loadTransactionsFromAPI() {
        lifecycleScope.launch {
            try {
                swipeRefreshLayout.isRefreshing = true
                val response = apiService.getAllTransactions()
                if (response.isSuccessful && response.body() != null) {
                    allApiTransactions = response.body()!!
                    currentPage = 1
                    hasMorePages = true
                    displayPagedTransactions(1)
                    calculateAndUpdateStats(allApiTransactions)
                } else {
                    Log.e("HistoryFragment", "Failed to load transactions: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("HistoryFragment", "Load failed: ${e.message}", e)
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun setupPaginationListener(layoutManager: LinearLayoutManager) {
        containerRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val totalItemCount = layoutManager.itemCount
                val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                
                // Load more khi cuộn tới 80% của danh sách
                if (!isLoadingMore && hasMorePages && lastVisiblePosition >= totalItemCount - 10) {
                    loadMoreTransactions()
                }
            }
        })
    }

    private fun loadMoreTransactions() {
        // Prevent race condition by double-checking
        if (isLoadingMore || !hasMorePages || etSearchTransaction.text.toString().isNotEmpty()) {
            return
        }
        
        isLoadingMore = true
        currentPage++
        displayPagedTransactions(currentPage)
    }

    private fun displayPagedTransactions(page: Int) {
        // Guard: prevent concurrent execution
        if (isProcessing) {
            Log.d("HistoryFragment", "Already processing, ignoring request for page $page")
            if (page > 1) {
                isLoadingMore = false  // Reset flag so next scroll can trigger
            }
            return
        }
        
        lifecycleScope.launch {
            try {
                isProcessing = true
                
                // Offload heavy work to background thread
                val result = withContext(Dispatchers.Default) {
                    // Calculate pagination indices
                    val startIndex = (page - 1) * pageSize
                    val endIndex = minOf(page * pageSize, allApiTransactions.size)
                    
                    // Check if there are more pages
                    hasMorePages = endIndex < allApiTransactions.size
                    
                    // Get paginated transactions
                    val paginatedTransactions = allApiTransactions.subList(startIndex, endIndex)
                    
                    // Apply filters AND convert in single pass (reduce iterations)
                    val expenses = if (isDateSelected) {
                        // Create separate calendar instances to avoid object reuse bug
                        val startOfDayCal = Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                        val startOfDay = startOfDayCal.timeInMillis / 1000
                        
                        val endOfDayCal = Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                        }
                        val endOfDay = endOfDayCal.timeInMillis / 1000
                        
                        // Filter AND map in one pass
                        paginatedTransactions.filter { tx ->
                            tx.timeStamp >= startOfDay && tx.timeStamp <= endOfDay
                        }.map { tx ->
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
                    } else {
                        // Convert without filter
                        paginatedTransactions.map { tx ->
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
                    }
                    
                    // Group by date on background thread
                    groupExpensesByDate(expenses)
                }
                
                // Only update UI on main thread
                withContext(Dispatchers.Main) {
                    if (page == 1) {
                        // First page: replace all
                        displayedTransactions = result
                        historyAdapter.submitList(result)
                    } else {
                        // Next pages: append (memory-efficient way)
                        val newList = displayedTransactions.toMutableList().apply { addAll(result) }
                        displayedTransactions = newList
                        historyAdapter.submitList(newList)
                    }
                    isLoadingMore = false
                    isProcessing = false
                }
            } catch (e: Exception) {
                Log.e("HistoryFragment", "Pagination failed: ${e.message}", e)
                isLoadingMore = false
            }
        }
    }

    private fun calculateAndUpdateStats(transactions: List<TransactionResponse>) {
        lifecycleScope.launch {
            val stats = withContext(Dispatchers.Default) {
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                val currentMonth = calendar.get(Calendar.MONTH) + 1
                
                var income = 0.0
                var expense = 0.0
                
                // Create single reusable Calendar instance
                val txCalendar = Calendar.getInstance()
                
                // Single pass through transactions
                for (tx in transactions) {
                    // Reuse calendar instance for each transaction
                    txCalendar.timeInMillis = tx.timeStamp * 1000
                    val txYear = txCalendar.get(Calendar.YEAR)
                    val txMonth = txCalendar.get(Calendar.MONTH) + 1
                    
                    if (txYear == currentYear && txMonth == currentMonth) {
                        if (tx.type == "INCOME") {
                            income += tx.amount
                        } else if (tx.type == "EXPENSE") {
                            expense += tx.amount
                        }
                    }
                }
                
                Triple(income, expense, income - expense)
            }
            
            // Update UI only
            withContext(Dispatchers.Main) {
                tvMonthIncome.text = "${numberFormatter.format(stats.first.toLong())} đ"
                tvMonthExpense.text = "${numberFormatter.format(stats.second.toLong())} đ"
                tvMonthSavings.text = "${numberFormatter.format(stats.third.toLong())} đ"
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
                
                // Reset pagination and reload
                currentPage = 1
                hasMorePages = true
                displayPagedTransactions(1)
                
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
        lifecycleScope.launch {
            val calendarSelected = Calendar.getInstance().apply { timeInMillis = selectedDate }
            val selectedYear = calendarSelected.get(Calendar.YEAR)
            val selectedMonth = calendarSelected.get(Calendar.MONTH) + 1
            
            val stats = withContext(Dispatchers.Default) {
                var income = 0.0
                var expense = 0.0
                
                // Create single reusable Calendar instance
                val txCalendar = Calendar.getInstance()
                
                for (tx in allApiTransactions) {
                    txCalendar.timeInMillis = tx.timeStamp * 1000
                    val txYear = txCalendar.get(Calendar.YEAR)
                    val txMonth = txCalendar.get(Calendar.MONTH) + 1
                    
                    if (txYear == selectedYear && txMonth == selectedMonth) {
                        if (tx.type == "INCOME") {
                            income += tx.amount
                        } else if (tx.type == "EXPENSE") {
                            expense += tx.amount
                        }
                    }
                }
                
                Triple(income, expense, income - expense)
            }
            
            withContext(Dispatchers.Main) {
                tvMonthIncome.text = "${numberFormatter.format(stats.first.toLong())} đ"
                tvMonthExpense.text = "${numberFormatter.format(stats.second.toLong())} đ"
                tvMonthSavings.text = "${numberFormatter.format(stats.third.toLong())} đ"
                
                // Update month labels with specific month and year
                updateMonthLabels(selectedDate)
            }
        }
    }

    private fun resetToCurrentMonth() {
        // Cancel any pending search job
        searchJob?.cancel()
        searchJob = null
        
        selectedDate = System.currentTimeMillis()
        isDateSelected = false
        
        // Update button icon back to calendar
        btnDatePicker.setImageResource(android.R.drawable.ic_menu_my_calendar)
        
        // Reset pagination and reload
        currentPage = 1
        hasMorePages = true
        
        // Clear search without triggering afterTextChanged cascade
        etSearchTransaction.removeTextChangedListener(searchTextWatcher)
        etSearchTransaction.setText("")
        etSearchTransaction.addTextChangedListener(searchTextWatcher)
        
        displayPagedTransactions(1)
        
        // Reset stats to current month
        calculateAndUpdateStats(allApiTransactions)
    }
}
