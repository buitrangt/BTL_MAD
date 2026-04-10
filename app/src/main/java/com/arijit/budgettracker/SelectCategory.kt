package com.arijit.budgettracker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.arijit.budgettracker.api.CategoryResponse
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.utils.Vibration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SelectCategory : AppCompatActivity() {
    companion object {
        private const val ADD_CATEGORY_REQUEST_CODE = 102
    }

    private lateinit var categoryGrid: GridLayout
    private lateinit var btnAddCategory: LinearLayout
    private lateinit var btnBack: TextView
    private lateinit var etSearch: EditText
    private lateinit var ivSearch: ImageView
    private var transactionType: String = "expense"
    private var allCategories: List<CategoryResponse> = emptyList()
    private var currentCategory: String = ""
    private var editingCategoryOldName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_select_category)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        transactionType = intent.getStringExtra("type") ?: "expense"
        currentCategory = intent.getStringExtra("currentCategory") ?: ""

        initViews()
        setupClickListeners()
        loadCategories()
    }

    private fun initViews() {
        categoryGrid = findViewById(R.id.categoryGrid)
        btnAddCategory = findViewById(R.id.btnAddCategory)
        btnBack = findViewById(R.id.btnBack)
        etSearch = findViewById(R.id.etSearch)
        ivSearch = findViewById(R.id.ivSearch)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            Vibration.vibrate(this, 50)
            if (currentCategory.isNotEmpty()) {
                setResult(RESULT_OK, Intent().apply {
                    putExtra("selectedCategory", currentCategory)
                })
            }
            finish()
        }

        btnAddCategory.setOnClickListener {
            Vibration.vibrate(this, 50)
            val intent = Intent(this, AddCategoryActivity::class.java)
            intent.putExtra("type", transactionType)
            startActivityForResult(intent, ADD_CATEGORY_REQUEST_CODE)
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterCategories(s.toString())
            }
        })

        ivSearch.setOnClickListener {
            etSearch.text.clear()
        }
    }

    private fun filterCategories(query: String) {
        val filtered = if (query.isEmpty()) {
            allCategories
        } else {
            allCategories.filter {
                it.name.lowercase().contains(query.lowercase()) ||
                        (it.note ?: "").lowercase().contains(query.lowercase())
            }
        }
        displayCategories(filtered)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_CATEGORY_REQUEST_CODE && resultCode == RESULT_OK) {
            val updatedCategory = data?.getStringExtra("selectedCategory")?.trim().orEmpty()
            if (updatedCategory.isNotEmpty() && currentCategory == editingCategoryOldName) {
                currentCategory = updatedCategory
            }
            editingCategoryOldName = ""
            loadCategories()
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(applicationContext)
                val response = withContext(Dispatchers.IO) { api.getAllCategories() }
                if (response.isSuccessful) {
                    allCategories = response.body().orEmpty()
                    displayCategories(allCategories)
                } else {
                    Toast.makeText(this@SelectCategory, "Không tải được danh mục", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SelectCategory, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayCategories(categories: List<CategoryResponse>) {
        categoryGrid.removeAllViews()

        val tvCategoryCount = findViewById<TextView>(R.id.tvCategoryCount)
        tvCategoryCount.text = "tổng ${categories.size}"

        categoryGrid.post {
            val gridWidth = categoryGrid.width
            val totalHorizontalPadding = categoryGrid.paddingLeft + categoryGrid.paddingRight
            val cardWidth = (gridWidth - totalHorizontalPadding) / 2 - 16

            for (category in categories) {
                val categoryCard = createCategoryCard(category)
                val params = GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                    width = cardWidth.coerceAtLeast(100)
                    height = 120
                    setMargins(8, 8, 8, 8)
                }
                categoryGrid.addView(categoryCard, params)
            }
        }
    }

    private fun createCategoryCard(category: CategoryResponse): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#F0F0F0"))
            isClickable = true
            isFocusable = true
            alpha = 0.8f
        }

        val textViewName = TextView(this).apply {
            text = category.name
            textSize = 14f
            setTextColor(0xFF333333.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
        }
        card.addView(textViewName)

        if (!category.note.isNullOrEmpty()) {
            val textViewDesc = TextView(this).apply {
                text = category.note
                textSize = 11f
                setTextColor(0xFF999999.toInt())
                gravity = android.view.Gravity.CENTER
                setPadding(8, 4, 8, 0)
            }
            card.addView(textViewDesc)
        }

        card.setOnClickListener {
            Vibration.vibrate(this@SelectCategory, 50)
            currentCategory = category.name
            setResult(RESULT_OK, Intent().apply {
                putExtra("selectedCategory", category.name)
            })
            finish()
        }

        // Long-press menu only for non-default categories
        if (!category.isDefault) {
            card.setOnLongClickListener {
                showCategoryPopupMenu(card, category)
                true
            }
        }

        return card
    }

    private fun showCategoryPopupMenu(view: android.view.View, category: CategoryResponse) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_category_action, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit_category -> {
                    editingCategoryOldName = category.name
                    val intent = Intent(this, AddCategoryActivity::class.java)
                    intent.putExtra("categoryRemoteId", category.id)
                    intent.putExtra("categoryName", category.name)
                    intent.putExtra("categoryDescription", category.note ?: "")
                    intent.putExtra("type", transactionType)
                    startActivityForResult(intent, ADD_CATEGORY_REQUEST_CODE)
                    true
                }
                R.id.action_delete_category -> {
                    showDeleteCategoryConfirmDialog(category)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeleteCategoryConfirmDialog(category: CategoryResponse) {
        AlertDialog.Builder(this)
            .setTitle("Xóa danh mục")
            .setMessage("Bạn có chắc muốn xóa danh mục \"${category.name}\"?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteCategory(category)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteCategory(category: CategoryResponse) {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getApiService(applicationContext)
                val response = withContext(Dispatchers.IO) {
                    api.deleteCategory(category.id)
                }
                if (response.isSuccessful) {
                    Toast.makeText(this@SelectCategory, "Đã xóa danh mục", Toast.LENGTH_SHORT).show()
                    loadCategories()
                } else {
                    // Backend may reject if category in use
                    Toast.makeText(
                        this@SelectCategory,
                        "Không thể xóa: danh mục có thể đang được sử dụng",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SelectCategory, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
