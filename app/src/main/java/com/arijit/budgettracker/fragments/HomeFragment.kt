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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private lateinit var todayAmt: TextView
    private lateinit var thisWeekAmt: TextView
    private lateinit var thisMonthAmt: TextView
    private lateinit var expenseBtn: CardView
    private lateinit var welcomeName: TextView
    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: HomeRecentAdapter

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

        val name = TokenManager.getName(requireContext())?.takeIf { it.isNotBlank() } ?: "User"
        welcomeName.text = name

        val recyclerView = view.findViewById<RecyclerView>(R.id.recent_rv)
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
            intent.putExtra("type", expense.type)
            intent.putExtra("category", expense.category)
            intent.putExtra("amount", expense.amount)
            intent.putExtra("note", expense.note)
            intent.putExtra("timeStamp", expense.timeStamp)
            startActivity(intent)
        }

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        viewModel.recentExpenses.observe(viewLifecycleOwner) { expenses ->
            adapter.submitList(expenses)
        }


        viewModel.todayAmount.observe(viewLifecycleOwner) {
            todayAmt.text = "₫%.2f".format(it)
        }

        viewModel.weekAmount.observe(viewLifecycleOwner) {
            thisWeekAmt.text = "₫%.2f".format(it)
        }

        viewModel.monthAmount.observe(viewLifecycleOwner) {
            thisMonthAmt.text = "₫%.2f".format(it)
        }

        return view
    }

    override fun onResume() {
        super.onResume()

        // Force refresh amounts display
        viewModel.todayAmount.value?.let { todayAmt.text = "₫%.2f".format(it) }
        viewModel.weekAmount.value?.let { thisWeekAmt.text = "₫%.2f".format(it) }
        viewModel.monthAmount.value?.let { thisMonthAmt.text = "₫%.2f".format(it) }
    }

    private fun showDeleteConfirmDialog(expense: com.arijit.budgettracker.db.Expense) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xóa giao dịch")
            .setMessage("Bạn có chắc muốn xóa giao dịch này?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteExpense(expense)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}