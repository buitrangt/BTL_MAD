package com.arijit.budgettracker

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.arijit.budgettracker.db.Expense
import com.arijit.budgettracker.db.ExpenseDatabase
import com.arijit.budgettracker.utils.SyncManager
import com.arijit.budgettracker.utils.Vibration
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddTransActivity : AppCompatActivity() {
    companion object {
        private const val CATEGORY_REQUEST_CODE = 101
    }

    private lateinit var cancelBtn: TextView
    private lateinit var confirmBtn: TextView
    private lateinit var typeBtn: LinearLayout
    private lateinit var catgBtn: LinearLayout
    private lateinit var calendarFooter: LinearLayout
    private lateinit var amountDisplay: TextView
    private lateinit var noteInput: EditText
    private var transactionType: String = "expense"
    private var selectedCategory: String = ""
    private var selectedNote: String = ""
    private var selectedAmount: String = ""
    private var selectedDate: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_trans)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupTypeSelection()
        setupCategorySelection()
        setupDatePicker()
        setupNumpad()
        setupConfirmCancel()
    }

    private fun initViews() {
        cancelBtn = findViewById(R.id.btnClose)
        confirmBtn = findViewById(R.id.btnConfirm)
        typeBtn = findViewById(R.id.btnType)
        catgBtn = findViewById(R.id.catg_btn)
        calendarFooter = findViewById(R.id.calendarFooter)
        amountDisplay = findViewById(R.id.tvAmount)
        noteInput = findViewById(R.id.etNote)
    }

    private fun setupTypeSelection() {
        typeBtn.setOnClickListener {
            Vibration.vibrate(this, 50)
            val options = arrayOf("KHOẢN CHI", "KHOẢN THU")
            android.app.AlertDialog.Builder(this)
                .setTitle("Chọn loại giao dịch")
                .setItems(options) { _, which ->
                    transactionType = if (which == 0) "expense" else "income"
                    updateTypeDisplay()
                }
                .show()
        }
    }

    private fun updateTypeDisplay() {
        val tvType = typeBtn.findViewById<TextView>(R.id.tvType)
        tvType.text = if (transactionType == "expense") "KHOẢN CHI" else "KHOẢN THU"
        
        // Change badge background color and dot based on type
        if (transactionType == "expense") {
            typeBtn.setBackgroundResource(R.drawable.bg_badge_red)
            val dot = typeBtn.getChildAt(0)
            if (dot != null) dot.setBackgroundResource(R.drawable.circle_red)
        } else {
            typeBtn.setBackgroundResource(R.drawable.bg_badge_green)
            val dot = typeBtn.getChildAt(0)
            if (dot != null) dot.setBackgroundResource(R.drawable.circle_green)
        }
    }

    private fun setupCategorySelection() {
        catgBtn.setOnClickListener {
            Vibration.vibrate(this, 50)
            val intent = android.content.Intent(this, SelectCategory::class.java)
            intent.putExtra("type", transactionType)
            startActivityForResult(intent, CATEGORY_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CATEGORY_REQUEST_CODE && resultCode == RESULT_OK) {
            selectedCategory = data?.getStringExtra("selectedCategory") ?: ""
            updateCategoryDisplay()
        }
    }

    private fun updateCategoryDisplay() {
        val tvCategory = catgBtn.findViewById<TextView>(R.id.tvCategory)
        tvCategory.text = if (selectedCategory.isEmpty()) "CHỌN DANH MỤC" else selectedCategory.uppercase()
    }

    private fun loadAndShowCategories() {
        lifecycleScope.launch {
            try {
                val db = ExpenseDatabase.getDatabase(applicationContext)
                val categoryDao = db.categoryDao()
                val categories = categoryDao.getCategoriesByType(transactionType)
                
                showCategoryBottomSheet(categories.map { it.name })
            } catch (e: Exception) {
                Toast.makeText(this@AddTransActivity, "Lỗi tải danh mục", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCategoryBottomSheet(categoryNames: List<String>) {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.catg_layout, null)
        bottomSheet.setContentView(view)

        // Clear existing views
        val container = view.findViewById<LinearLayout>(R.id.category_container)
        container?.removeAllViews()

        // Add category items dynamically
        for (categoryName in categoryNames) {
            val categoryView = layoutInflater.inflate(R.layout.item_category, container, false)
            val categoryText = categoryView.findViewById<TextView>(R.id.category_name)
            categoryText.text = categoryName

            categoryView.setOnClickListener {
                selectedCategory = categoryName
                bottomSheet.dismiss()
            }
            container?.addView(categoryView)
        }

        // Add "Thêm danh mục" button
        val addCategoryView = layoutInflater.inflate(R.layout.item_add_category, container, false)
        addCategoryView.setOnClickListener {
            bottomSheet.dismiss()
            // Open AddCategoryActivity
            val intent = android.content.Intent(this, AddCategoryActivity::class.java)
            intent.putExtra("type", transactionType)
            startActivity(intent)
        }
        container?.addView(addCategoryView)

        bottomSheet.show()
    }

    private fun setupDatePicker() {
        calendarFooter.setOnClickListener {
            Vibration.vibrate(this, 50)
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setSelection(selectedDate)
                .setTitleText("Chọn ngày")
                .build()
            
            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDate = selection
                updateDateDisplay()
            }
            
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }
    }

    private fun updateDateDisplay() {
        val tvDateFooter = findViewById<TextView>(R.id.tvDateFooter)
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
        val dateStr = sdf.format(Date(selectedDate))
        tvDateFooter.text = dateStr
    }

    private fun setupNumpad() {
        val buttons = mapOf(
            R.id.btn1 to "1", R.id.btn2 to "2", R.id.btn3 to "3",
            R.id.btn4 to "4", R.id.btn5 to "5", R.id.btn6 to "6",
            R.id.btn7 to "7", R.id.btn8 to "8", R.id.btn9 to "9",
            R.id.btn0 to "0", R.id.btnDot to "."
        )
        buttons.forEach { (id, digit) ->
            findViewById<TextView>(id)?.setOnClickListener { appendDigit(digit) }
        }

        findViewById<TextView>(R.id.btnBackspace)?.setOnClickListener { deleteDigit() }
    }

    private fun appendDigit(digit: String) {
        if (digit == ".") {
            if (selectedAmount.isEmpty() || selectedAmount.endsWith(".") || selectedAmount.contains(".")) return
        }
        selectedAmount += digit
        updateAmountDisplay()
    }

    private fun deleteDigit() {
        if (selectedAmount.isNotEmpty()) {
            selectedAmount = selectedAmount.dropLast(1)
            updateAmountDisplay()
        }
    }

    private fun updateAmountDisplay() {
        amountDisplay.text = if (selectedAmount.isEmpty()) "0.00" else selectedAmount
    }

    private fun setupConfirmCancel() {
        cancelBtn.setOnClickListener {
            finish()
        }
        confirmBtn.setOnClickListener {
            Vibration.vibrate(this, 100)
            selectedNote = noteInput.text.toString().trim()
            if (selectedAmount.isEmpty() || selectedCategory.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveTransaction()
        }
    }

    private fun saveTransaction() {
        lifecycleScope.launch {
            val db = ExpenseDatabase.getDatabase(applicationContext)
            val dao = db.expenseDao()
            val expense = Expense(
                amount = selectedAmount.toDoubleOrNull() ?: 0.0,
                category = selectedCategory,
                note = selectedNote,
                type = transactionType,
                timeStamp = selectedDate
            )
            dao.insertExpense(expense)
            SyncManager.syncIfOnline(applicationContext)
            finish()
        }
    }
}