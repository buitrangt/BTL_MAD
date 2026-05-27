package com.arijit.budgettracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.arijit.budgettracker.api.AuthRequest
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.utils.TokenManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * Activity xử lý giao diện đăng nhập (Login Screen) của hệ thống.
 * Chịu trách nhiệm:
 * 1. Tự động kiểm tra trạng thái đăng nhập từ phiên trước (nếu có token -> chuyển tiếp màn hình tương ứng).
 * 2. Phân quyền người dùng sau đăng nhập (ADMIN chuyển tới AdminOverviewActivity, USER chuyển tới MainActivity).
 * 3. Kiểm duyệt dữ liệu nhập vào (email, mật khẩu).
 * 4. Gọi API đăng nhập bất đồng bộ và xử lý kết quả thành công/lỗi.
 */
class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Tự động kiểm tra phiên đăng nhập cũ
        // Nếu người dùng đã đăng nhập trước đó và token chưa hết hạn, tự động chuyển màn hình theo Role
        if (TokenManager.isLoggedIn(this)) {
            val target = if (TokenManager.isAdmin(this)) {
                AdminOverviewActivity::class.java
            } else {
                MainActivity::class.java
            }
            startActivity(Intent(this, target))
            finish()
            return
        }

        // Kích hoạt chế độ hiển thị tràn viền (Edge-to-Edge)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Thiết lập lắng nghe inset hệ thống để tự động điều chỉnh padding, tránh việc giao diện đè lên status bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Ánh xạ các thành phần giao diện (UI Views)
        val etEmail = findViewById<android.widget.EditText>(R.id.et_email)
        val etPassword = findViewById<android.widget.EditText>(R.id.et_password)
        val btnLogin = findViewById<MaterialButton>(R.id.btn_login)
        val tvError = findViewById<TextView>(R.id.tv_error)
        val tvRegister = findViewById<TextView>(R.id.tv_register)
        val tvForgotPassword = findViewById<TextView>(R.id.tv_forgot_pw)

        // 2. Thiết lập sự kiện click nút Đăng nhập
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Kiểm duyệt dữ liệu đầu vào: Không được để trống các trường bắt buộc
            if (email.isEmpty() || password.isEmpty()) {
                tvError.text = "Vui lòng điền đầy đủ tất cả các trường dữ liệu"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Vô hiệu hóa nút bấm tạm thời để chống click spam liên tục
            btnLogin.isEnabled = false
            tvError.visibility = View.GONE // Ẩn thông báo lỗi cũ nếu có

            // Khởi chạy Coroutine chạy bất đồng bộ trong vòng đời Activity
            lifecycleScope.launch {
                try {
                    // Gọi API đăng nhập trên server thông qua Retrofit
                    val response = RetrofitClient.getApiService(this@LoginActivity)
                        .login(AuthRequest(email = email, password = password))

                    if (response.isSuccessful && response.body() != null) {
                        val authResponse = response.body()!!

                        // Lưu trữ JWT Token cục bộ bằng SharedPreferences thông qua TokenManager
                        TokenManager.saveToken(this@LoginActivity, authResponse.token)

                        // Lưu thông tin chi tiết người dùng
                        TokenManager.saveUser(
                            this@LoginActivity,
                            authResponse.email,
                            authResponse.name ?: "",
                            authResponse.phone ?: ""
                        )
                        TokenManager.saveRole(this@LoginActivity, authResponse.role)

                        // Phân tích Role của người dùng để điều hướng phù hợp
                        val target = if ("ADMIN".equals(authResponse.role, ignoreCase = true)) {
                            AdminOverviewActivity::class.java
                        } else {
                            MainActivity::class.java
                        }
                        startActivity(Intent(this@LoginActivity, target))
                        finish()
                    } else {
                        // Xử lý khi thông tin tài khoản hoặc mật khẩu không chính xác
                        tvError.text = "Email hoặc mật khẩu không đúng!"
                        tvError.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    // Xử lý khi có lỗi kết nối mạng (server sập, mất internet)
                    tvError.text = "Lỗi kết nối máy chủ: ${e.localizedMessage}"
                    tvError.visibility = View.VISIBLE
                } finally {
                    // Kích hoạt lại nút bấm đăng nhập
                    btnLogin.isEnabled = true
                }
            }
        }

        // 3. Sự kiện chuyển sang màn hình Đăng ký tài khoản mới
        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // 4. Sự kiện chuyển sang màn hình Quên mật khẩu
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }
}