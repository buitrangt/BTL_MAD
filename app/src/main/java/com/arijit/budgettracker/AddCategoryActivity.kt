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
import com.arijit.budgettracker.db.Category
import com.arijit.budgettracker.db.ExpenseDatabase
import com.arijit.budgettracker.utils.Vibration
import kotlinx.coroutines.launch

class AddCategoryActivity : AppCompatActivity() {
    private lateinit var etCategoryName: EditText
    private lateinit var etCategoryDescription: EditText
    private lateinit var btnSaveCategory: LinearLayout
    private lateinit var btnBack: TextView
    private var categoryType: String = "expense"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_category)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        categoryType = intent.getStringExtra("type") ?: "expense"

        initViews()
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

    private fun saveCategory() {
        val categoryName = etCategoryName.text.toString().trim()
        val categoryDescription = etCategoryDescription.text.toString().trim()

        if (categoryName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val db = ExpenseDatabase.getDatabase(applicationContext)
                val categoryDao = db.categoryDao()
                
                val newCategory = Category(
                    name = categoryName,
                    icon = "📁",  // Default icon
                    type = categoryType,
                    description = categoryDescription,
                    createdAt = System.currentTimeMillis()
                )
                
                categoryDao.insertCategory(newCategory)
                
                Toast.makeText(this@AddCategoryActivity, "Danh mục đã được thêm", Toast.LENGTH_SHORT).show()
                
                // Return to SelectCategory without selecting the new category
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddCategoryActivity, "Lỗi khi lưu danh mục: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
