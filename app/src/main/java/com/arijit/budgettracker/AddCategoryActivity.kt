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
    private var categoryType: String = "both"
    
    private var editingCategoryId: Int = 0 // 0 means new category
    private var isEditMode: Boolean = false
    private var oldCategoryName: String = "" // Store original name for updates

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_category)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        categoryType = "both"
        
        // Check if editing
        if (intent.hasExtra("categoryId")) {
            isEditMode = true
            editingCategoryId = intent.getIntExtra("categoryId", 0)
        }

        initViews()
        
        if (isEditMode) {
            // Populate fields for editing
            oldCategoryName = intent.getStringExtra("categoryName") ?: ""
            etCategoryName.setText(oldCategoryName)
            etCategoryDescription.setText(intent.getStringExtra("categoryDescription") ?: "")
            
            // Change button text - find TextView child in LinearLayout
            val tvButtonLabel = (btnSaveCategory.getChildAt(0) as? TextView)
            if (tvButtonLabel != null) {
                tvButtonLabel.text = "Cập nhật danh mục"
            }
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

    private fun saveCategory() {
        var categoryName = etCategoryName.text.toString().trim()
        val categoryDescription = etCategoryDescription.text.toString().trim()

        if (categoryName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Validate category name format - no special chars
        if (!categoryName.matches(Regex("^[a-zA-Zà-ỿ0-9 ]+$"))) {
            Toast.makeText(this, "Tên danh mục chỉ được chứa chữ, số và khoảng trắng", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val db = ExpenseDatabase.getDatabase(applicationContext)
                val categoryDao = db.categoryDao()
                val expenseDao = db.expenseDao()
                
                if (isEditMode) {
                    // Check if category name changed
                    val categoryNameChanged = (categoryName != oldCategoryName)
                    
                    // Update existing category
                    val category = Category(
                        id = editingCategoryId,
                        name = categoryName,
                        icon = "📁",
                        type = categoryType,
                        description = categoryDescription,
                        createdAt = System.currentTimeMillis()
                    )
                    categoryDao.updateCategory(category)
                    
                    // If name changed, update all expenses with old category name
                    if (categoryNameChanged) {
                        expenseDao.updateExpenseCategoryName(oldCategoryName, categoryName)
                        Toast.makeText(this@AddCategoryActivity, "Cập nhật danh mục và ${expenseDao.getExpenseCountByCategory(categoryName)} giao dịch", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@AddCategoryActivity, "Cập nhật danh mục thành công", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Insert new category
                    val newCategory = Category(
                        name = categoryName,
                        icon = "📁",
                        type = categoryType,
                        description = categoryDescription,
                        createdAt = System.currentTimeMillis()
                    )
                    categoryDao.insertCategory(newCategory)
                    Toast.makeText(this@AddCategoryActivity, "Danh mục đã được thêm", Toast.LENGTH_SHORT).show()
                }
                
                // Return updated category name so caller can update their reference
                setResult(RESULT_OK, Intent().apply {
                    putExtra("selectedCategory", categoryName)
                })
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddCategoryActivity, "Lỗi khi lưu danh mục: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
