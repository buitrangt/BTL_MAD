package com.arijit.budgettracker

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.arijit.budgettracker.api.ChangePasswordRequest
import com.arijit.budgettracker.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var edtOldPassword: EditText
    private lateinit var edtNewPassword: EditText
    private lateinit var edtConfirmPassword: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnBack: ImageButton
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_password)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        edtOldPassword = findViewById(R.id.edtOldPassword)
        edtNewPassword = findViewById(R.id.edtNewPassword)
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnBack = findViewById(R.id.btnBack)
        tvError = findViewById(R.id.tvError)

        btnBack.setOnClickListener { finish() }
        btnSubmit.setOnClickListener { submit() }
    }

    private fun showError(msg: String) {
        tvError.text = msg
        tvError.visibility = View.VISIBLE
    }

    private fun clearError() {
        tvError.visibility = View.GONE
    }

    private fun submit() {
        clearError()
        val oldPwd = edtOldPassword.text.toString()
        val newPwd = edtNewPassword.text.toString()
        val confirmPwd = edtConfirmPassword.text.toString()

        if (oldPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin")
            return
        }
        if (newPwd.length < 6) {
            showError("Mật khẩu mới phải có ít nhất 6 ký tự")
            return
        }
        if (newPwd != confirmPwd) {
            showError("Mật khẩu xác nhận không khớp")
            return
        }
        if (oldPwd == newPwd) {
            showError("Mật khẩu mới phải khác mật khẩu cũ")
            return
        }

        btnSubmit.isEnabled = false
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.getApiService(this@ChangePasswordActivity)
                        .changePassword(ChangePasswordRequest(oldPwd, newPwd))
                }
                if (response.isSuccessful) {
                    Toast.makeText(this@ChangePasswordActivity, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errMsg = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        ?: "Lỗi ${response.code()}"
                    showError(errMsg)
                    btnSubmit.isEnabled = true
                }
            } catch (e: Exception) {
                showError("Lỗi kết nối: ${e.message}")
                btnSubmit.isEnabled = true
            }
        }
    }
}
