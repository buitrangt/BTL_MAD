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

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etName = findViewById<android.widget.EditText>(R.id.et_name)
        val etEmail = findViewById<android.widget.EditText>(R.id.et_email)
        val etPassword = findViewById<android.widget.EditText>(R.id.et_password)
        val btnRegister = findViewById<MaterialButton>(R.id.btn_register)
        val tvError = findViewById<TextView>(R.id.tv_error)
        val tvLogin = findViewById<TextView>(R.id.tv_login)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                tvError.text = "Please fill email and password"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            tvError.visibility = View.GONE

            lifecycleScope.launch {
                try {
                    // Truyền null cho phone vì sẽ cập nhật sau ở Profile
                    val response = RetrofitClient.getApiService(this@RegisterActivity)
                        .register(AuthRequest(
                            email = email,
                            password = password,
                            name = name,
                            phone = null
                        ))

                    if (response.isSuccessful && response.body() != null) {

                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
                        finish()
                    } else {
                        tvError.text = "Error: ${response.code()} - ${response.errorBody()?.string()}"
                        tvError.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    tvError.text = "Connection error: ${e.localizedMessage}"
                    tvError.visibility = View.VISIBLE
                } finally {
                    btnRegister.isEnabled = true
                }
            }
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }
}