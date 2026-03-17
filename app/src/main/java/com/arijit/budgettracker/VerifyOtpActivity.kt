package com.arijit.budgettracker

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class VerifyOtpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verify_otp)

        // 1. Xử lý Padding hệ thống
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        // 2. Ánh xạ các View
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnVerify = findViewById<MaterialButton>(R.id.btnVerify)
        val tvResendCode = findViewById<TextView>(R.id.tvResendCode)

        val otpInputs = arrayOf(
            findViewById<EditText>(R.id.otp1),
            findViewById<EditText>(R.id.otp2),
            findViewById<EditText>(R.id.otp3),
            findViewById<EditText>(R.id.otp4),
            findViewById<EditText>(R.id.otp5),
            findViewById<EditText>(R.id.otp6)
        )

        // 3. Thiết lập Logic nhập OTP tự động nhảy ô
        setupOtpLogic(otpInputs)

        // 4. Sự kiện nút Quay lại
        btnBack.setOnClickListener { finish() }

        // 5. Sự kiện nút Xác nhận
        btnVerify.setOnClickListener {
            val otpCode = otpInputs.joinToString("") { it.text.toString() }

            if (otpCode.length < 6) {
                Toast.makeText(this, "Vui lòng nhập đủ 6 chữ số", Toast.LENGTH_SHORT).show()
            } else {
                // Thực hiện gọi API xác thực OTP ở đây
                Toast.makeText(this, "Đang xác thực mã: $otpCode", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, ResetPasswordActivity::class.java)
                startActivity(intent)
            }
        }

        // 6. Sự kiện Gửi lại mã
        tvResendCode.setOnClickListener {
            Toast.makeText(this, "Đã gửi lại mã mới!", Toast.LENGTH_SHORT).show()
            // Logic bắt đầu lại đồng hồ đếm ngược có thể thêm ở đây
        }
    }

    private fun setupOtpLogic(inputs: Array<EditText>) {
        for (i in inputs.indices) {
            // Nhảy ô khi nhập liệu
            inputs[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < inputs.size - 1) {
                        inputs[i + 1].requestFocus()
                    }
                }
            })

            // Quay lại ô trước khi nhấn xóa (Backspace)
            inputs[i].setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (inputs[i].text.isEmpty() && i > 0) {
                        inputs[i - 1].requestFocus()
                        inputs[i - 1].text = null // Xóa luôn ký tự ô trước đó
                        return@setOnKeyListener true
                    }
                }
                false
            }
        }
    }
}