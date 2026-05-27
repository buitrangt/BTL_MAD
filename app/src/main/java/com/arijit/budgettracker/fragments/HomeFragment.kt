package com.arijit.budgettracker.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arijit.budgettracker.AddTransActivity
import com.arijit.budgettracker.MainActivity
import com.arijit.budgettracker.R
import com.arijit.budgettracker.models.HomeViewModel
import com.arijit.budgettracker.utils.HomeRecentAdapter
import com.arijit.budgettracker.utils.SyncManager
import com.arijit.budgettracker.utils.TokenManager
import com.arijit.budgettracker.utils.Vibration
import com.arijit.budgettracker.utils.CurrencyPrefs
import com.arijit.budgettracker.utils.AppRefreshBus
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Fragment hiển thị trang chủ của ứng dụng.
 * Thuộc luồng chức năng: Xem chi tiêu hôm nay/tuần này/tháng này.
 * Chịu trách nhiệm:
 * 1. Hiển thị tổng quan chi tiêu theo các khoảng thời gian (Hôm nay, Tuần này, Tháng này) tính ở thời điểm xem.
 * 2. Cung cấp truy cập nhanh đến chức năng Thêm giao dịch và AI phân tích.
 */
class HomeFragment : Fragment() {
    private lateinit var todayAmt: TextView
    private lateinit var thisWeekAmt: TextView
    private lateinit var thisMonthAmt: TextView
    private lateinit var expenseBtn: CardView
    private lateinit var welcomeName: TextView
    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: HomeRecentAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        todayAmt = view.findViewById(R.id.today_amt)
        thisWeekAmt = view.findViewById(R.id.this_week_amt)
        thisMonthAmt = view.findViewById(R.id.this_month_amt)
        welcomeName = view.findViewById(R.id.tv_welcome_name)
        expenseBtn = view.findViewById(R.id.add_expense_btn)
        expenseBtn.setOnClickListener {
            Vibration.vibrate(requireContext(), 100)
            startActivity(Intent(requireContext(), AddTransActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_ai_analysis).setOnClickListener {
            Vibration.vibrate(requireContext(), 50)
            startActivity(Intent(requireContext(), com.arijit.budgettracker.InsightsActivity::class.java))
        }

        refreshWelcomeName()

        recyclerView = view.findViewById(R.id.recent_rv)
        adapter = HomeRecentAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        view.findViewById<TextView>(R.id.tv_see_all).setOnClickListener {
            (activity as? MainActivity)?.navigateToHistory()
        }

        adapter.onDeleteClick = { expense ->
            showDeleteConfirmDialog(expense)
        }

        adapter.onEditClick = { expense ->
            val intent = Intent(requireContext(), AddTransActivity::class.java)
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

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        viewModel.recentExpenses.observe(viewLifecycleOwner) { expenses ->
            adapter.submitList(expenses)
            if (expenses.isNotEmpty()) {
                recyclerView.post {
                    (recyclerView.layoutManager as? LinearLayoutManager)
                        ?.scrollToPositionWithOffset(0, 0)
                        ?: recyclerView.scrollToPosition(0)
                }
            }
        }


        // 1. Hiển thị chi tiêu trong ngày hôm nay
        viewModel.todayAmount.observe(viewLifecycleOwner) {
            todayAmt.text = com.arijit.budgettracker.utils.CurrencyPrefs.format(it)
        }

        // 2. Hiển thị chi tiêu trong tuần này
        viewModel.weekAmount.observe(viewLifecycleOwner) {
            thisWeekAmt.text = com.arijit.budgettracker.utils.CurrencyPrefs.format(it)
        }

        // 3. Hiển thị chi tiêu trong tháng này
        viewModel.monthAmount.observe(viewLifecycleOwner) {
            thisMonthAmt.text = com.arijit.budgettracker.utils.CurrencyPrefs.format(it)
        }

        // Load data from API
        viewModel.loadHomeOverview()

        // Global refresh: any trans/category changes should update Home immediately
        AppRefreshBus.refreshTick.observe(viewLifecycleOwner) {
            viewModel.loadHomeOverview()
            refreshWelcomeName()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // Refresh from API when returning to Home tab
        viewModel.loadHomeOverview()
        refreshWelcomeName()
    }

    private fun refreshWelcomeName() {
        if (!::welcomeName.isInitialized) return
        val name = TokenManager.getName(requireContext())?.takeIf { it.isNotBlank() } ?: "User"
        welcomeName.text = name
    }

    private fun showDeleteConfirmDialog(expense: com.arijit.budgettracker.db.Expense) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa giao dịch")
            .setMessage("Bạn có chắc muốn xóa giao dịch này?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteTransaction(expense)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}