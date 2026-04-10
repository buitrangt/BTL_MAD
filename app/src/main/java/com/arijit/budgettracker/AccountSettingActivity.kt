package com.arijit.budgettracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.models.User
import com.arijit.budgettracker.utils.TokenManager
import kotlinx.coroutines.launch

class AccountSettingActivity : AppCompatActivity() {

    private lateinit var edtFullName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPhone: EditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_account_setting)

        // 1. Khởi tạo View
        initViews()

        // 2. Xử lý WindowInsets (Chống tràn màn hình)
        val mainLayout = findViewById<View>(R.id.main_layout)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 3. Tải dữ liệu từ DB ngay khi vào trang
        loadUserData()

        // 4. Sự kiện nút Back
        btnBack.setOnClickListener { finish() }

        // 5. Sự kiện nút Lưu
        btnSave.setOnClickListener { updateUserData() }

        // 6. Card Đổi mật khẩu
        findViewById<View>(R.id.itemChangePassword).setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }
    }

    private fun initViews() {
        edtFullName = findViewById(R.id.edtFullName)
        edtEmail = findViewById(R.id.edtEmail)
        edtPhone = findViewById(R.id.edtPhone)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)

        // Email thường là duy nhất và dùng để định danh, nên hạn chế cho sửa trực tiếp ở đây
        // edtEmail.isEnabled = false
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getApiService(this@AccountSettingActivity).getUserProfile()
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    edtFullName.setText(user.name)
                    edtEmail.setText(user.email)
                    edtPhone.setText(user.phone)
                } else {
                    // Nếu lỗi API, dùng dữ liệu tạm từ SharedPreferences
                    fillDataFromLocal()
                }
            } catch (e: Exception) {
                fillDataFromLocal()
            }
        }
    }

    private fun fillDataFromLocal() {
        edtFullName.setText(TokenManager.getName(this))
        edtEmail.setText(TokenManager.getEmail(this))

        // Cập nhật Phone từ máy nếu API lỗi
        val savedPhone = TokenManager.getPhone(this)
        if (!savedPhone.isNullOrEmpty()) {
            edtPhone.setText(savedPhone)
        }
        // Bạn có thể thêm hàm getEmail trong TokenManager tương tự getName
        // edtEmail.setText(TokenManager.getEmail(this))
    }

    private fun updateUserData() {
        val newName = edtFullName.text.toString().trim()
        val newPhone = edtPhone.text.toString().trim()
        val email = edtEmail.text.toString().trim()

        if (newName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show()
            return
        }

        btnSave.isEnabled = false
        lifecycleScope.launch {
            try {
                // SỬA TẠI ĐÂY: Tạo UpdateProfileRequest thay vì User
                val request = com.arijit.budgettracker.api.UpdateProfileRequest(
                    email = email,
                    name = newName,
                    phone = newPhone
                )

                val response = RetrofitClient.getApiService(this@AccountSettingActivity).updateProfile(request)

                if (response.isSuccessful) {
                    // Lưu lại local để các màn hình khác cập nhật theo
                    TokenManager.saveUser(this@AccountSettingActivity, email, newName, newPhone)
                    Toast.makeText(this@AccountSettingActivity, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@AccountSettingActivity, "Lỗi ${response.code()}: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AccountSettingActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnSave.isEnabled = true
            }
        }
    }
}