package com.arijit.budgettracker

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
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
import androidx.lifecycle.lifecycleScope
import com.arijit.budgettracker.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class VerifyOtpActivity : AppCompatActivity() {

    private var countDownTimer: CountDownTimer? = null
    private val otpTimeInMs: Long = 5 * 60 * 1000
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
        val email = intent.getStringExtra("EXTRA_EMAIL") ?: ""

        val otpInputs = arrayOf(
            findViewById<EditText>(R.id.otp1),
            findViewById<EditText>(R.id.otp2),
            findViewById<EditText>(R.id.otp3),
            findViewById<EditText>(R.id.otp4),
            findViewById<EditText>(R.id.otp5),
            findViewById<EditText>(R.id.otp6)
        )

        startCountDownTimer(tvResendCode)

        // 3. Thiết lập Logic nhập OTP tự động nhảy ô
        setupOtpLogic(otpInputs)

        // 4. Sự kiện nút Quay lại
        btnBack.setOnClickListener { finish() }

        // 5. Sự kiện nút Xác nhận
        btnVerify.setOnClickListener {
            val otpCode = otpInputs.joinToString("") { it.text.toString() }

            if (otpCode.length < 6) {
                Toast.makeText(this, "Vui lòng nhập đủ 6 chữ số", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.getApiService(this@VerifyOtpActivity).verifyOtp(email, otpCode)
                    if (response.isSuccessful) {
                        val intent = Intent(this@VerifyOtpActivity, ResetPasswordActivity::class.java)
                        intent.putExtra("EXTRA_EMAIL", email) // Tiếp tục truyền email sang màn đổi pass
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@VerifyOtpActivity, "Mã OTP sai hoặc hết hạn", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@VerifyOtpActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 6. Sự kiện Gửi lại mã
        tvResendCode.setOnClickListener {
            lifecycleScope.launch {
                try {
                    // Gọi API gửi lại OTP
                    val response = RetrofitClient.getApiService(this@VerifyOtpActivity).forgotPassword(email)
                    if (response.isSuccessful) {
                        Toast.makeText(this@VerifyOtpActivity, "Đã gửi lại mã mới!", Toast.LENGTH_SHORT).show()
                        // Reset lại đồng hồ đếm ngược 5 phút
                        startCountDownTimer(tvResendCode)
                    } else {
                        Toast.makeText(this@VerifyOtpActivity, "Lỗi gửi lại mã", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@VerifyOtpActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
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

    private fun startCountDownTimer(tvResendCode: TextView) {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(otpTimeInMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60

                // Định dạng mm:ss (ví dụ: 04:59)
                val timeString = String.format("%02d:%02d", minutes, seconds)

                tvResendCode.text = "Gửi lại mã ($timeString)"
                tvResendCode.isEnabled = false // Khóa nút khi đang đếm ngược
                tvResendCode.setTextColor(android.graphics.Color.GRAY)
            }

            override fun onFinish() {
                tvResendCode.text = "Gửi lại mã"
                tvResendCode.isEnabled = true // Mở lại nút khi hết giờ
                tvResendCode.setTextColor(android.graphics.Color.parseColor("#10B981"))
            }
        }.start()
    }
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}