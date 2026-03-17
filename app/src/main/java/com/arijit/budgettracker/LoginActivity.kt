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

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Skip login if already authenticated
        if (TokenManager.isLoggedIn(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        // Trong onCreate, thay thế đoạn setOnApplyWindowInsetsListener cũ bằng đoạn này:
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Chỉ lấy Top và Bottom để chống tràn vào Status Bar và Navigation Bar
            // Phần Left và Right chúng ta giữ nguyên (0) vì đã dùng Guideline 24dp trong XML
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
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.getApiService(this@LoginActivity)
                        .login(AuthRequest(email, password))

                    if (response.isSuccessful && response.body() != null) {
                        val authResponse = response.body()!!
                        TokenManager.saveToken(this@LoginActivity, authResponse.token)
                        TokenManager.saveUser(this@LoginActivity, authResponse.email, authResponse.name)
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        tvError.text = "Invalid email or password"
                        tvError.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    tvError.text = "Connection error :"+e.message;
                    tvError.visibility = View.VISIBLE
                    // Allow offline access
                     startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
                btnLogin.isEnabled = true
            }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        // Thiết lập điều hướng Quên mật khẩu
        tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }
}
