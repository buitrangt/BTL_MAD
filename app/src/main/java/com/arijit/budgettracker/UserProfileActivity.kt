package com.arijit.budgettracker

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
        val tvUserName = findViewById<TextView>(R.id.tvUserName)

        val currentName = TokenManager.getName(this)
        if (!currentName.isNullOrEmpty()) {
            tvUserName.text = currentName
        } else {
            tvUserName.text = "Người dùng"
        }

        // Cập nhật nút Logout để hiện Popup thay vì chuyển trang ngay
        logout.setOnClickListener {
            Vibration.vibrate(this, 50)
            showLogoutDialog()
        }
        // Sự kiện mở trang Cài đặt tài khoản
        account.setOnClickListener {
//            Vibration.vibrate(this, 30)
            val intent = Intent(this, AccountSettingActivity::class.java)
            startActivity(intent)
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

    // Hàm tạo và hiển thị Popup Logout
    private fun showLogoutDialog() {
        val dialog = Dialog(this)
        // Nạp layout popup bạn đã sửa (Nút Đỏ trên, Nút Trắng dưới)
        dialog.setContentView(R.layout.layout_logout_dialog)

        // Áp dụng Style Animation bạn vừa tạo
        dialog.window?.setWindowAnimations(R.style.DialogAnimation)

        // Làm nền mặc định của Dialog trong suốt để hiện thị CardView bo góc
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirmLogout)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        // Xử lý nút Đăng xuất (Màu đỏ)
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            TokenManager.logout(this)
            val intent = Intent(this, LoginActivity::class.java)
            // Xóa hết các Activity cũ để tránh người dùng nhấn Back quay lại được
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        // Xử lý nút Hủy
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}