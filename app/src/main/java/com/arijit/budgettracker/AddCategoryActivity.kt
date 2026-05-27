package com.arijit.budgettracker

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.arijit.budgettracker.api.CategoryRequest
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.utils.AppRefreshBus
import com.arijit.budgettracker.utils.Vibration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity xử lý giao diện thêm mới hoặc chỉnh sửa danh mục (Category).
 * Thuộc luồng chức năng: Quản lý danh mục (Thêm/Sửa danh mục).
 * Chịu trách nhiệm:
 * 1. Thu thập dữ liệu danh mục từ người dùng (tên danh mục, mô tả).
 * 2. Gọi API để lưu mới hoặc cập nhật danh mục lên máy chủ.
 */
class AddCategoryActivity : AppCompatActivity() {
    private lateinit var etCategoryName: EditText
    private lateinit var etCategoryDescription: EditText
    private lateinit var btnSaveCategory: LinearLayout
    private lateinit var btnBack: TextView

    private var editingRemoteId: Long = 0L
    private var isEditMode: Boolean = false
    private var oldCategoryName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_category)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Check if editing - now we use remoteId (server id)
        if (intent.hasExtra("categoryRemoteId")) {
            isEditMode = true
            editingRemoteId = intent.getLongExtra("categoryRemoteId", 0L)
        }

        initViews()

        if (isEditMode) {
            oldCategoryName = intent.getStringExtra("categoryName") ?: ""
            etCategoryName.setText(oldCategoryName)
            etCategoryDescription.setText(intent.getStringExtra("categoryDescription") ?: "")

            val tvButtonLabel = (btnSaveCategory.getChildAt(0) as? TextView)
            tvButtonLabel?.text = "Cập nhật danh mục"
        }

        setupClickListeners()
    }

    private fun initViews() {
        etCategoryName = findViewById(R.id.etCategoryName)
        etCategoryDescription = findViewById(R.id.etCategoryDescription)
        btnSaveCategory = findViewById(R.id.btnSaveCategory)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            Vibration.vibrate(this, 50)
            finish()
        }

        btnSaveCategory.setOnClickListener {
            Vibration.vibrate(this, 50)
            saveCategory()
        }
    }

    // 1. Logic kiểm tra và gọi API lưu danh mục (Thêm hoặc Sửa)
    private fun saveCategory() {
        val categoryName = etCategoryName.text.toString().trim()
        val categoryDescription = etCategoryDescription.text.toString().trim()

        if (categoryName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show()
            return
        }

        if (!categoryName.matches(Regex("^[a-zA-Zà-ỿ0-9 ]+$"))) {
            Toast.makeText(this, "Tên danh mục chỉ được chứa chữ, số và khoảng trắng", Toast.LENGTH_SHORT).show()
            return
        }

        btnSaveCategory.isEnabled = false
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(applicationContext)
                val request = CategoryRequest(
                    name = categoryName,
                    note = categoryDescription.ifBlank { null }
                )

                val response = withContext(Dispatchers.IO) {
                    if (isEditMode && editingRemoteId > 0) {
                        api.updateCategory(editingRemoteId, request)
                    } else {
                        api.createCategory(request)
                    }
                }

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@AddCategoryActivity,
                        if (isEditMode) "Cập nhật danh mục thành công" else "Đã thêm danh mục",
                        Toast.LENGTH_SHORT
                    ).show()
                    AppRefreshBus.notifyChanged()
                    setResult(RESULT_OK, Intent().apply {
                        putExtra("selectedCategory", categoryName)
                    })
                    finish()
                } else {
                    Toast.makeText(
                        this@AddCategoryActivity,
                        "Lỗi: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    btnSaveCategory.isEnabled = true
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AddCategoryActivity,
                    "Lỗi kết nối: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                btnSaveCategory.isEnabled = true
            }
        }
    }
}
