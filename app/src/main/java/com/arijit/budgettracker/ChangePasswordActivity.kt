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

/**
 * Activity xử lý giao diện Đổi mật khẩu (Change Password Screen) dành cho người dùng đã đăng nhập.
 * Chịu trách nhiệm:
 * 1. Cho phép người dùng nhập mật khẩu cũ, mật khẩu mới và xác nhận mật khẩu mới.
 * 2. Thực hiện kiểm tra phía Client: các trường không trống, mật khẩu mới ít nhất 6 ký tự, mật khẩu mới khác mật khẩu cũ.
 * 3. Gọi API bảo mật `/api/auth/change-password` đính kèm Token xác thực trong Header (qua AuthInterceptor).
 * 4. Đóng màn hình sau khi thay đổi mật khẩu thành công.
 */
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

        // Thiết lập Insets để không đè lên các thanh hệ thống
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Ánh xạ các UI View từ Layout XML
        edtOldPassword = findViewById(R.id.edtOldPassword)
        edtNewPassword = findViewById(R.id.edtNewPassword)
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnBack = findViewById(R.id.btnBack)
        tvError = findViewById(R.id.tvError)

        // Thiết lập các sự kiện click
        btnBack.setOnClickListener { finish() }
        btnSubmit.setOnClickListener { submit() }
    }

    /**
     * Hiển thị thông báo lỗi lên TextView chỉ định trên giao diện
     */
    private fun showError(msg: String) {
        tvError.text = msg
        tvError.visibility = View.VISIBLE
    }

    /**
     * Ẩn thông báo lỗi khi bắt đầu thực hiện kiểm duyệt hoặc request mới
     */
    private fun clearError() {
        tvError.visibility = View.GONE
    }

    /**
     * Quy trình xử lý nghiệp vụ khi nhấn nút Đổi mật khẩu:
     * 1. Lấy dữ liệu và kiểm duyệt Client-side.
     * 2. Vô hiệu hóa nút Submit để tránh click spam.
     * 3. Khởi chạy luồng gọi API bất đồng bộ trên Dispatchers.IO.
     */
    private fun submit() {
        clearError()
        val oldPwd = edtOldPassword.text.toString()
        val newPwd = edtNewPassword.text.toString()
        val confirmPwd = edtConfirmPassword.text.toString()

        // 1. Kiểm duyệt dữ liệu đầu vào
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
        // 2. Gọi API bất đồng bộ
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.getApiService(this@ChangePasswordActivity)
                        .changePassword(ChangePasswordRequest(oldPwd, newPwd))
                }
                if (response.isSuccessful) {
                    Toast.makeText(this@ChangePasswordActivity, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                    finish() // Đóng activity và quay về màn hình trước đó
                } else {
                    // Xử lý lỗi trả về từ server (ví dụ mật khẩu cũ không đúng)
                    val errMsg = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        ?: "Lỗi hệ thống: ${response.code()}"
                    showError(errMsg)
                    btnSubmit.isEnabled = true
                }
            } catch (e: Exception) {
                showError("Lỗi kết nối máy chủ: ${e.message}")
                btnSubmit.isEnabled = true
            }
        }
    }
}
