package com.arijit.budgettracker

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.arijit.budgettracker.api.ResetPasswordRequest
import com.arijit.budgettracker.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Activity xử lý đặt lại mật khẩu mới (Reset Password Screen) sau khi xác thực OTP thành công.
 * Chịu trách nhiệm:
 * 1. Cho phép nhập mật khẩu mới và mật khẩu xác nhận lại.
 * 2. Đối khớp mật khẩu Client-side trước khi truyền qua mạng.
 * 3. Gọi API `/api/auth/reset-password` qua Retrofit.
 * 4. Chuyển hướng người dùng về màn hình đăng nhập (LoginActivity) sau khi hoàn thành.
 */
class ResetPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reset_password)

        // 1. Xử lý Padding hệ thống (Tránh tràn viền, chạm status/navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Giữ nguyên Left/Right là 0 vì đã dùng Guideline trong XML
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        // 2. Ánh xạ các UI View từ Layout XML
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etNewPassword = findViewById<TextInputEditText>(R.id.et_new_password)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.et_confirm_password)
        val btnReset = findViewById<MaterialButton>(R.id.btn_reset_password)
        val tvBackToLogin = findViewById<TextView>(R.id.tv_back_to_login)

        // 3. Logic nút Back (Quay lại màn hình trước đó)
        btnBack.setOnClickListener {
            finish()
        }

        // 4. Logic nút "Quay lại trang đăng nhập" dưới cùng
        tvBackToLogin.setOnClickListener {
            finish()
        }

        // 5. Logic nút Cập nhật mật khẩu
        btnReset.setOnClickListener {
            val newPass = etNewPassword.text.toString().trim()
            val confirmPass = etConfirmPassword.text.toString().trim()

            // Kiểm duyệt: Các trường dữ liệu không được để trống
            if (newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Kiểm duyệt: Mật khẩu mới và mật khẩu nhập lại phải trùng khớp
            if (newPass != confirmPass) {
                Toast.makeText(this, "Mật khẩu xác nhận không trùng khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Gọi API đổi mật khẩu
            performResetPassword(newPass)
        }
    }

    /**
     * Thực thi gọi API đặt lại mật khẩu mới thông qua RetrofitClient.
     */
    private fun performResetPassword(password: String) {
        val email = intent.getStringExtra("EXTRA_EMAIL") ?: ""

        lifecycleScope.launch {
            try {
                // Đóng gói request đặt lại mật khẩu
                val request = ResetPasswordRequest(email, password)
                val response = RetrofitClient.getApiService(this@ResetPasswordActivity).resetPassword(request)

                if (response.isSuccessful) {
                    Toast.makeText(this@ResetPasswordActivity, "Đặt lại mật khẩu thành công!", Toast.LENGTH_LONG).show()
                    
                    // Quay trở lại LoginActivity và xóa toàn bộ lịch sử các màn hình trung gian
                    val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    Toast.makeText(this@ResetPasswordActivity, "Lỗi từ hệ thống khi cập nhật mật khẩu", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ResetPasswordActivity, "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show()
            }
        }
    }
}