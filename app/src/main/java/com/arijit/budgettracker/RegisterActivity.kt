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
 * Activity xử lý màn hình Đăng ký tài khoản mới (Registration Screen).
 * Chịu trách nhiệm:
 * 1. Hiển thị form đăng ký gồm: Họ tên, Email, Mật khẩu.
 * 2. Kiểm duyệt dữ liệu đầu vào phía Client (email, mật khẩu không được trống).
 * 3. Gọi API đăng ký bất đồng bộ `/api/auth/register` qua Retrofit.
 * 4. Tự động điều hướng về màn hình đăng nhập (LoginActivity) khi tạo tài khoản thành công.
 */
class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Kích hoạt giao diện tràn viền
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        // Thiết lập padding tự động tránh bị thanh điều hướng hoặc thanh trạng thái đè lên UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Ánh xạ các UI View từ Layout XML
        val etName = findViewById<android.widget.EditText>(R.id.et_name)
        val etEmail = findViewById<android.widget.EditText>(R.id.et_email)
        val etPassword = findViewById<android.widget.EditText>(R.id.et_password)
        val btnRegister = findViewById<MaterialButton>(R.id.btn_register)
        val tvError = findViewById<TextView>(R.id.tv_error)
        val tvLogin = findViewById<TextView>(R.id.tv_login)

        // Thiết lập sự kiện nhấn nút Đăng ký
        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // 1. Kiểm duyệt dữ liệu form cơ bản
            if (email.isEmpty() || password.isEmpty()) {
                tvError.text = "Vui lòng nhập đầy đủ Email và Mật khẩu"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Vô hiệu hóa nút Đăng ký để ngăn click đúp và ẩn thông báo lỗi cũ
            btnRegister.isEnabled = false
            tvError.visibility = View.GONE

            // 2. Chạy luồng gọi API đăng ký tài khoản bất đồng bộ
            lifecycleScope.launch {
                try {
                    // Gọi API đăng ký. Truyền phone = null vì thông tin này sẽ được cập nhật sau trong trang hồ sơ cá nhân
                    val response = RetrofitClient.getApiService(this@RegisterActivity)
                        .register(AuthRequest(
                            email = email,
                            password = password,
                            name = name,
                            phone = null
                        ))

                    if (response.isSuccessful && response.body() != null) {
                        // Đăng ký thành công: chuyển sang LoginActivity và xóa sạch stack các activity trước đó
                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
                        finish()
                    } else {
                        // Phản hồi từ server bị lỗi (ví dụ email đã tồn tại)
                        tvError.text = "Lỗi đăng ký: Email đã tồn tại hoặc không hợp lệ"
                        tvError.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    // Lỗi kết nối mạng hoặc lỗi server sập
                    tvError.text = "Lỗi kết nối: ${e.localizedMessage}"
                    tvError.visibility = View.VISIBLE
                } finally {
                    // Kích hoạt lại nút bấm
                    btnRegister.isEnabled = true
                }
            }
        }

        // Sự kiện click dòng chữ "Đã có tài khoản? Đăng nhập ngay" để quay lại LoginActivity
        tvLogin.setOnClickListener {
            finish()
        }
    }
}