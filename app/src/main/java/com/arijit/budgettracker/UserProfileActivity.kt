package com.arijit.budgettracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.arijit.budgettracker.utils.TokenManager
import com.arijit.budgettracker.utils.Vibration

class UserProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val account = findViewById<RelativeLayout>(R.id.itemAccount)
        val currency = findViewById<RelativeLayout>(R.id.itemCurrency)
        val notification = findViewById<RelativeLayout>(R.id.itemNotification)
        val theme = findViewById<RelativeLayout>(R.id.itemTheme)
        val logout = findViewById< Button>(R.id.btnLogout)

        logout.setOnClickListener {
            Vibration.vibrate(this, 50)
            TokenManager.logout(this)
            startActivity(Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
        }
        account.setOnClickListener {
            // mở trang chỉnh sửa tài khoản
        }

        currency.setOnClickListener {
            // chọn tiền tệ
        }

        notification.setOnClickListener {
            // bật tắt thông báo
        }

        theme.setOnClickListener {
            // đổi dark mode
        }
    }
}