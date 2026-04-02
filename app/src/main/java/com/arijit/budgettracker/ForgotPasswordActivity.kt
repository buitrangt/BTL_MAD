package com.arijit.budgettracker

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.arijit.budgettracker.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        // 1. Xử lý Padding hệ thống để không bị tràn vào Status Bar/Navigation Bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Giữ nguyên Left/Right là 0 vì đã dùng Guideline 24dp trong XML
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        // 2. Ánh xạ các View
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etEmail = findViewById<EditText>(R.id.et_email_forgot)
        val btnSendCode = findViewById<MaterialButton>(R.id.btn_send_code)
        val tvBackToLogin = findViewById<TextView>(R.id.tv_back_to_login_forgot)

        // 3. Sự kiện nút Back (Mũi tên)
        btnBack.setOnClickListener {
            finish() // Đóng activity này để quay lại màn hình Login
        }

        // 4. Sự kiện dòng "Quay lại Đăng nhập"
        tvBackToLogin.setOnClickListener {
            finish()
        }

        // 5. Xử lý nút Gửi mã xác nhận
        btnSendCode.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Vui lòng nhập địa chỉ email"
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Định dạng email không hợp lệ"
                return@setOnClickListener
            }

            // Gọi hàm xử lý gửi mã (API)
            sendVerificationCode(email)
        }
    }

    private fun sendVerificationCode(email: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getApiService(this@ForgotPasswordActivity).forgotPassword(email)
                if (response.isSuccessful) {
                    Toast.makeText(this@ForgotPasswordActivity, "Mã đã gửi đến $email", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@ForgotPasswordActivity, VerifyOtpActivity::class.java)
                    intent.putExtra("EXTRA_EMAIL", email) // Gửi email đi
                    startActivity(intent)
                } else {
                    val error = response.errorBody()?.string() ?: "Email không tồn tại"
                    Toast.makeText(this@ForgotPasswordActivity, error, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ForgotPasswordActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}