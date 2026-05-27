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

/**
 * Activity xử lý giao diện xác thực mã OTP (Verify OTP Screen) cho chức năng Quên mật khẩu.
 * Chịu trách nhiệm:
 * 1. Hiển thị 6 ô nhập mã số OTP tương ứng với 6 chữ số nhận từ email.
 * 2. Tự động chuyển tiêu điểm (focus) sang ô tiếp theo khi người dùng nhập số.
 * 3. Hỗ trợ phím xóa lùi (Backspace): Tự động quay về ô trước đó và xóa ký tự.
 * 4. Chạy đồng hồ đếm ngược (5 phút) giới hạn hiệu lực OTP. Cho phép và vô hiệu hóa nút gửi lại (Resend OTP).
 * 5. Gọi API `/api/auth/verify-otp` để xác minh mã và chuyển tiếp sang màn hình đặt lại mật khẩu (ResetPasswordActivity).
 */
class VerifyOtpActivity : AppCompatActivity() {

    private var countDownTimer: CountDownTimer? = null
    private val otpTimeInMs: Long = 5 * 60 * 1000 // Mã OTP có hiệu lực trong vòng 5 phút (300000ms)

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

        // 2. Ánh xạ các UI View từ Layout XML
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnVerify = findViewById<MaterialButton>(R.id.btnVerify)
        val tvResendCode = findViewById<TextView>(R.id.tvResendCode)
        val email = intent.getStringExtra("EXTRA_EMAIL") ?: ""

        // Tạo mảng chứa 6 ô nhập OTP
        val otpInputs = arrayOf(
            findViewById<EditText>(R.id.otp1),
            findViewById<EditText>(R.id.otp2),
            findViewById<EditText>(R.id.otp3),
            findViewById<EditText>(R.id.otp4),
            findViewById<EditText>(R.id.otp5),
            findViewById<EditText>(R.id.otp6)
        )

        // Khởi động đồng hồ đếm ngược cho mã OTP
        startCountDownTimer(tvResendCode)

        // 3. Thiết lập Logic nhập OTP tự động nhảy ô
        setupOtpLogic(otpInputs)

        // 4. Sự kiện nút Quay lại màn hình trước đó
        btnBack.setOnClickListener { finish() }

        // 5. Sự kiện nút Xác nhận mã OTP
        btnVerify.setOnClickListener {
            // Nối 6 ký tự từ 6 ô nhập thành 1 chuỗi OTP duy nhất
            val otpCode = otpInputs.joinToString("") { it.text.toString() }

            if (otpCode.length < 6) {
                Toast.makeText(this, "Vui lòng nhập đủ 6 chữ số của mã xác nhận", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Gọi API kiểm tra mã OTP
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.getApiService(this@VerifyOtpActivity).verifyOtp(email, otpCode)
                    if (response.isSuccessful) {
                        // Xác thực thành công: Chuyển sang màn hình đặt lại mật khẩu mới
                        val intent = Intent(this@VerifyOtpActivity, ResetPasswordActivity::class.java)
                        intent.putExtra("EXTRA_EMAIL", email) // Truyền email tiếp sang để thực hiện cập nhật password
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@VerifyOtpActivity, "Mã OTP không chính xác hoặc đã hết hạn!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@VerifyOtpActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 6. Sự kiện Gửi lại mã OTP
        tvResendCode.setOnClickListener {
            lifecycleScope.launch {
                try {
                    // Gọi lại API forgotPassword để hệ thống sinh mã OTP mới gửi về email
                    val response = RetrofitClient.getApiService(this@VerifyOtpActivity).forgotPassword(email)
                    if (response.isSuccessful) {
                        Toast.makeText(this@VerifyOtpActivity, "Mã OTP mới đã được gửi đi thành công!", Toast.LENGTH_SHORT).show()
                        // Reset lại đồng hồ đếm ngược 5 phút
                        startCountDownTimer(tvResendCode)
                    } else {
                        Toast.makeText(this@VerifyOtpActivity, "Lỗi gửi lại mã xác nhận", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@VerifyOtpActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Logic tự động chuyển focus giữa 6 ô nhập mã OTP:
     * - Khi nhập xong 1 chữ số, tự động trỏ đến ô bên phải.
     * - Khi nhấn phím xóa (Backspace) khi ô đang trống, tự động quay lại ô bên trái và xóa ký tự của ô đó.
     */
    private fun setupOtpLogic(inputs: Array<EditText>) {
        for (i in inputs.indices) {
            // Tự động nhảy sang ô tiếp theo sau khi nhập liệu xong chữ số
            inputs[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < inputs.size - 1) {
                        inputs[i + 1].requestFocus()
                    }
                }
            })

            // Tự động quay lại ô phía trước khi nhấn xóa (Backspace) trên bàn phím ảo
            inputs[i].setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (inputs[i].text.isEmpty() && i > 0) {
                        inputs[i - 1].requestFocus()
                        inputs[i - 1].text = null // Xóa ký tự ở ô trước
                        return@setOnKeyListener true
                    }
                }
                false
            }
        }
    }

    /**
     * Đồng hồ đếm ngược hiển thị thời hạn nhập mã OTP:
     * - Vô hiệu hóa nút gửi lại OTP khi đang đếm ngược.
     * - Đổi màu xám biểu thị trạng thái khóa.
     * - Khi kết thúc đếm ngược, cho phép nhấn nút trở lại và chuyển màu xanh lục nổi bật.
     */
    private fun startCountDownTimer(tvResendCode: TextView) {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(otpTimeInMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60

                // Định dạng hiển thị kiểu mm:ss (Ví dụ: 04:59)
                val timeString = String.format("%02d:%02d", minutes, seconds)

                tvResendCode.text = "Gửi lại mã ($timeString)"
                tvResendCode.isEnabled = false // Khóa nút không cho click spam gửi lại OTP
                tvResendCode.setTextColor(android.graphics.Color.GRAY)
            }

            override fun onFinish() {
                tvResendCode.text = "Gửi lại mã"
                tvResendCode.isEnabled = true // Kích hoạt lại nút gửi khi đồng hồ đếm xong
                tvResendCode.setTextColor(android.graphics.Color.parseColor("#10B981"))
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hủy timer để tránh rò rỉ bộ nhớ (memory leak) khi Activity bị destroy
        countDownTimer?.cancel()
    }
}