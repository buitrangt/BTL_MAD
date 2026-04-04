package com.arijit.budgettracker

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.arijit.budgettracker.db.ExpenseDatabase
import com.arijit.budgettracker.utils.Vibration
import kotlinx.coroutines.launch

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
    private var allCategories: List<com.arijit.budgettracker.db.Category> = emptyList()

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
            finish()
        }

        btnAddCategory.setOnClickListener {
            Vibration.vibrate(this, 50)
            val intent = Intent(this, AddCategoryActivity::class.java)
            intent.putExtra("type", transactionType)
            startActivityForResult(intent, ADD_CATEGORY_REQUEST_CODE)
        }

        // Search functionality
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
                it.description.lowercase().contains(query.lowercase())
            }
        }
        displayCategories(filtered)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_CATEGORY_REQUEST_CODE && resultCode == RESULT_OK) {
            val newCategory = data?.getStringExtra("selectedCategory") ?: ""
            // If category was selected, return it
            if (newCategory.isNotEmpty()) {
                setResult(RESULT_OK, Intent().apply {
                    putExtra("selectedCategory", newCategory)
                })
                finish()
            } else {
                // If no category was selected, just reload the list
                loadCategories()
            }
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val db = ExpenseDatabase.getDatabase(applicationContext)
                val categoryDao = db.categoryDao()
                allCategories = categoryDao.getCategoriesByType(transactionType)
                displayCategories(allCategories)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun displayCategories(categories: List<com.arijit.budgettracker.db.Category>) {
        categoryGrid.removeAllViews()

        // Update category count
        val tvCategoryCount = findViewById<TextView>(R.id.tvCategoryCount)
        tvCategoryCount.text = "tổng ${categories.size}"

        // Calculate card width based on screen size
        categoryGrid.post {
            val gridWidth = categoryGrid.width
            val totalHorizontalPadding = categoryGrid.paddingLeft + categoryGrid.paddingRight
            val cardWidth = (gridWidth - totalHorizontalPadding) / 2 - 16 // 16 for margins

            for (category in categories) {
                val categoryCard = createCategoryCard(category)
                val params = GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
                    width = cardWidth.coerceAtLeast(100) // Minimum width 100
                    height = 120
                    setMargins(8, 8, 8, 8)
                }
                categoryGrid.addView(categoryCard, params)

                categoryCard.setOnClickListener {
                    Vibration.vibrate(this@SelectCategory, 50)
                    setResult(RESULT_OK, Intent().apply {
                        putExtra("selectedCategory", category.name)
                    })
                    finish()
                }
            }
        }
    }

    private fun createCategoryCard(category: com.arijit.budgettracker.db.Category): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#F0F0F0")) // Semi-transparent
            isClickable = true
            isFocusable = true
            alpha = 0.8f // Semi-transparent effect
        }

        val textViewName = TextView(this).apply {
            text = category.name
            textSize = 14f
            setTextColor(0xFF333333.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
        }
        card.addView(textViewName)

        // Add description if available
        if (category.description.isNotEmpty()) {
            val textViewDesc = TextView(this).apply {
                text = category.description
                textSize = 11f
                setTextColor(0xFF999999.toInt())
                gravity = android.view.Gravity.CENTER
                setPadding(8, 4, 8, 0)
            }
            card.addView(textViewDesc)
        }

        return card
    }
}