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

/**
 * Activity xử lý yêu cầu quên mật khẩu (Forgot Password Screen).
 * Chịu trách nhiệm:
 * 1. Cho phép người dùng nhập địa chỉ email đã đăng ký.
 * 2. Xác thực định dạng email hợp lệ tại Client.
 * 3. Gọi API `/api/auth/forgot-password` để yêu cầu hệ thống sinh mã OTP và gửi email.
 * 4. Truyền email sang và điều hướng người dùng đến màn hình xác thực OTP (VerifyOtpActivity).
 */
class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Kích hoạt tràn viền
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        // 1. Xử lý Padding hệ thống để không bị tràn vào Status Bar/Navigation Bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Giữ nguyên Left/Right là 0 vì đã dùng Guideline 24dp trong XML
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        // 2. Ánh xạ các UI View từ XML
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etEmail = findViewById<EditText>(R.id.et_email_forgot)
        val btnSendCode = findViewById<MaterialButton>(R.id.btn_send_code)
        val tvBackToLogin = findViewById<TextView>(R.id.tv_back_to_login_forgot)

        // 3. Sự kiện nút Back (Mũi tên trên thanh tiêu đề)
        btnBack.setOnClickListener {
            finish() // Đóng activity này để quay lại màn hình Login
        }

        // 4. Sự kiện dòng "Quay lại Đăng nhập" dưới cùng
        tvBackToLogin.setOnClickListener {
            finish()
        }

        // 5. Xử lý sự kiện khi nhấn nút "Gửi mã xác nhận"
        btnSendCode.setOnClickListener {
            val email = etEmail.text.toString().trim()

            // Kiểm duyệt: Email không được trống
            if (email.isEmpty()) {
                etEmail.error = "Vui lòng nhập địa chỉ email"
                return@setOnClickListener
            }

            // Kiểm duyệt: Đúng định dạng email tiêu chuẩn
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Định dạng email không hợp lệ"
                return@setOnClickListener
            }

            // Gọi hàm xử lý gửi yêu cầu sinh OTP lên Server
            sendVerificationCode(email)
        }
    }

    /**
     * Hàm gọi API gửi mã xác thực (OTP) tới email người dùng
     */
    private fun sendVerificationCode(email: String) {
        lifecycleScope.launch {
            try {
                // Gọi API forgotPassword qua RetrofitClient
                val response = RetrofitClient.getApiService(this@ForgotPasswordActivity).forgotPassword(email)
                
                if (response.isSuccessful) {
                    Toast.makeText(this@ForgotPasswordActivity, "Mã xác thực đã được gửi đến $email", Toast.LENGTH_SHORT).show()

                    // Điều hướng sang màn hình nhập mã OTP (VerifyOtpActivity)
                    val intent = Intent(this@ForgotPasswordActivity, VerifyOtpActivity::class.java)
                    intent.putExtra("EXTRA_EMAIL", email) // Truyền email sang để dùng ở bước xác thực
                    startActivity(intent)
                } else {
                    // Lỗi từ server (Ví dụ email không tồn tại trong hệ thống)
                    val error = response.errorBody()?.string() ?: "Email không tồn tại trong hệ thống"
                    Toast.makeText(this@ForgotPasswordActivity, error, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                // Lỗi kết nối mạng
                Toast.makeText(this@ForgotPasswordActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}