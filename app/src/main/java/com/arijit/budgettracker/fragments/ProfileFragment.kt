package com.arijit.budgettracker.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.arijit.budgettracker.LoginActivity
import com.arijit.budgettracker.R
import com.arijit.budgettracker.models.ProfileViewModel
import com.arijit.budgettracker.utils.CurrencyPrefs
import com.arijit.budgettracker.utils.TokenManager
import com.arijit.budgettracker.utils.Vibration

class ProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.activity_user_profile, container, false)

        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        tvUserName.text = TokenManager.getName(requireContext())?.takeIf { it.isNotBlank() } ?: "Người dùng"

        val tvBalance = view.findViewById<TextView>(R.id.tvBalance)
        val tvSpent = view.findViewById<TextView>(R.id.tvSpent)
        val btnLogout = view.findViewById<View>(R.id.btnLogout)

        val vm = ViewModelProvider(this)[ProfileViewModel::class.java]
        vm.balance.observe(viewLifecycleOwner) { tvBalance.text = CurrencyPrefs.format(it) }
        vm.totalExpense.observe(viewLifecycleOwner) { tvSpent.text = CurrencyPrefs.format(it) }

        btnLogout.setOnClickListener {
            Vibration.vibrate(requireContext(), 50)
            TokenManager.logout(requireContext())
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            activity?.finish()
        }

        return view
    }
}

