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
// Import đúng class từ package api
import com.arijit.budgettracker.api.AuthRequest
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.utils.TokenManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kiểm tra đăng nhập - redirect theo role
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

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail = findViewById<android.widget.EditText>(R.id.et_email)
        val etPassword = findViewById<android.widget.EditText>(R.id.et_password)
        val btnLogin = findViewById<MaterialButton>(R.id.btn_login)
        val tvError = findViewById<TextView>(R.id.tv_error)
        val tvRegister = findViewById<TextView>(R.id.tv_register)
        val tvForgotPassword = findViewById<TextView>(R.id.tv_forgot_pw)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                tvError.text = "Please fill all fields"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            tvError.visibility = View.GONE // Ẩn lỗi cũ khi bắt đầu request mới

            lifecycleScope.launch {
                try {
                    // Sử dụng AuthRequest (phone truyền null vì login không cần phone)
                    val response = RetrofitClient.getApiService(this@LoginActivity)
                        .login(AuthRequest(email = email, password = password))

                    if (response.isSuccessful && response.body() != null) {
                        val authResponse = response.body()!!

                        // Lưu Token
                        TokenManager.saveToken(this@LoginActivity, authResponse.token)

                        // Lưu thông tin User (Xử lý null an toàn cho name)
                        TokenManager.saveUser(
                            this@LoginActivity,
                            authResponse.email,
                            authResponse.name ?: "",
                            authResponse.phone ?: ""
                        )
                        TokenManager.saveRole(this@LoginActivity, authResponse.role)

                        // Redirect theo role
                        val target = if ("ADMIN".equals(authResponse.role, ignoreCase = true)) {
                            AdminOverviewActivity::class.java
                        } else {
                            MainActivity::class.java
                        }
                        startActivity(Intent(this@LoginActivity, target))
                        finish()
                    } else {
                        tvError.text = "Invalid email or password"
                        tvError.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    // nếu MainActivity bắt buộc phải có dữ liệu từ server.
                    tvError.text = "Connection error: ${e.localizedMessage}"
                    tvError.visibility = View.VISIBLE

                    // Code cho phép offline access:
                    // startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    // finish()
                } finally {
                    btnLogin.isEnabled = true
                }
            }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }
}