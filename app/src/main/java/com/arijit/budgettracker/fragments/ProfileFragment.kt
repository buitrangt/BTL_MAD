package com.arijit.budgettracker.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.arijit.budgettracker.AccountSettingActivity
import com.arijit.budgettracker.LoginActivity
import com.arijit.budgettracker.R
import com.arijit.budgettracker.models.ProfileViewModel
import com.arijit.budgettracker.utils.CurrencyPrefs
import com.arijit.budgettracker.utils.TokenManager
import com.arijit.budgettracker.utils.Vibration

class ProfileFragment : Fragment() {
    private lateinit var vm: ProfileViewModel
    private lateinit var tvUserName: TextView // Khai báo biến toàn cục trong class
    private lateinit var tvUserEmail: TextView
    override fun onResume() {
        super.onResume()
        if (::vm.isInitialized) vm.loadStats()
        updateUserUI()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.activity_user_profile, container, false)

        tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        tvUserName.text = TokenManager.getName(requireContext())?.takeIf { it.isNotBlank() } ?: "Người dùng"

        tvUserEmail = view.findViewById<TextView>(R.id.tvUserEmail)
        tvUserEmail.text = TokenManager.getEmail(requireContext()) ?: ""

        val tvBalance = view.findViewById<TextView>(R.id.tvBalance)
        val tvSpent = view.findViewById<TextView>(R.id.tvSpent)
        val btnLogout = view.findViewById<View>(R.id.btnLogout)

        vm = ViewModelProvider(this)[ProfileViewModel::class.java]
        vm.balance.observe(viewLifecycleOwner) { tvBalance.text = CurrencyPrefs.format(it) }
        vm.totalExpense.observe(viewLifecycleOwner) { tvSpent.text = CurrencyPrefs.format(it) }

        // Account settings - opens AccountSettingActivity
        view.findViewById<View>(R.id.itemAccount).setOnClickListener {
            Vibration.vibrate(requireContext(), 50)
            startActivity(Intent(requireContext(), AccountSettingActivity::class.java))
        }

        // Currency, Notification, Theme - placeholders (locked to VND, no theme switch yet)
        view.findViewById<View>(R.id.itemCurrency).setOnClickListener {
            Toast.makeText(requireContext(), "Đơn vị tiền cố định: VND (₫)", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.itemNotification).setOnClickListener {
            Toast.makeText(requireContext(), "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.itemTheme).setOnClickListener {
            Toast.makeText(requireContext(), "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener {
            Vibration.vibrate(requireContext(), 50)
            TokenManager.logout(requireContext())
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            activity?.finish()
        }
        updateUserUI()
        return view
    }
    private fun updateUserUI() {
        val context = context ?: return
        tvUserName.text = TokenManager.getName(context)?.takeIf { it.isNotBlank() } ?: "Người dùng"
        tvUserEmail.text = TokenManager.getEmail(context) ?: ""
    }
}

