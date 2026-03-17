package com.arijit.budgettracker

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AccountSettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_account_setting)

        // 1. Xử lý khoảng cách thanh hệ thống (Pin, giờ) để không đè lên giao diện
        // Lưu ý: Đảm bảo ConstraintLayout gốc trong XML có id là @+id/main_layout
        val rootLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main_layout)
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // 2. Kết nối nút Back (btnBack)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val edtFullName = findViewById<EditText>(R.id.edtFullName)
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPhone = findViewById<EditText>(R.id.edtPhone)
        val btnSave = findViewById<Button>(R.id.btnSave)
        btnBack.setOnClickListener {
            // Đóng Activity này để quay lại màn hình Profile trước đó
            finish()
        }
        btnSave.setOnClickListener {
            // Hiển thị thông báo nhanh (Toast)
            Toast.makeText(this, "Đã lưu thay đổi thành công!", Toast.LENGTH_SHORT).show()

            // Sau khi lưu xong có thể đóng màn hình hoặc làm gì đó tiếp theo
            // finish()
        }
    }
}