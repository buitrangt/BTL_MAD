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

class ResetPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reset_password)

        // 1. Xử lý Padding hệ thống (Tránh tràn viền)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Giữ nguyên Left/Right là 0 vì đã dùng Guideline trong XML
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        // 2. Ánh xạ các View từ XML
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etNewPassword = findViewById<TextInputEditText>(R.id.et_new_password)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.et_confirm_password)
        val btnReset = findViewById<MaterialButton>(R.id.btn_reset_password)
        val tvBackToLogin = findViewById<TextView>(R.id.tv_back_to_login)

        // 3. Logic nút Back (Quay lại màn hình trước đó)
        btnBack.setOnClickListener {
            finish()
        }

        // 4. Logic nút "Quay lại trang đăng nhập"
        tvBackToLogin.setOnClickListener {
            finish()
        }

        // 5. Logic nút Cập nhật mật khẩu
        btnReset.setOnClickListener {
            val newPass = etNewPassword.text.toString().trim()
            val confirmPass = etConfirmPassword.text.toString().trim()

            // Kiểm tra các trường có trống không
            if (newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Kiểm tra so khớp mật khẩu
            if (newPass != confirmPass) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                // Bạn có thể hiển thị lỗi trực tiếp lên TextInputLayout nếu muốn
                return@setOnClickListener
            }

            // Nếu mọi thứ ổn, thực hiện gọi API đổi mật khẩu ở đây
            performResetPassword(newPass)
        }
    }

    private fun performResetPassword(password: String) {
        val email = intent.getStringExtra("EXTRA_EMAIL") ?: ""

        lifecycleScope.launch {
            try {
                val request = ResetPasswordRequest(email, password)
                val response = RetrofitClient.getApiService(this@ResetPasswordActivity).resetPassword(request)

                if (response.isSuccessful) {
                    Toast.makeText(this@ResetPasswordActivity, "Đổi mật khẩu thành công!", Toast.LENGTH_LONG).show()
                    val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    Toast.makeText(this@ResetPasswordActivity, "Lỗi cập nhật mật khẩu", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ResetPasswordActivity, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
            }
        }
    }
}