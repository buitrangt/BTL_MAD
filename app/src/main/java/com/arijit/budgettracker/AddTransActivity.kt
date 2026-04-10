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
import com.arijit.budgettracker.api.ExpenseRequest
import com.arijit.budgettracker.api.RetrofitClient
import com.arijit.budgettracker.db.Expense
import com.arijit.budgettracker.utils.Vibration
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var originalExpense: Expense? = null
    
    private var editingExpenseId: Int = 0 // 0 means new expense
    private var isEditMode: Boolean = false
    private var editingRemoteId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_trans)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Check if editing
        if (intent.hasExtra("expenseId") || intent.hasExtra("remoteId")) {
            isEditMode = true
            editingExpenseId = intent.getIntExtra("expenseId", 0)
            editingRemoteId = intent.getLongExtra("remoteId", 0L).takeIf { it > 0L }
            transactionType = intent.getStringExtra("type") ?: "expense"
            selectedCategory = intent.getStringExtra("category") ?: ""
            val selectedName = intent.getStringExtra("name") ?: selectedCategory
            selectedAmount = intent.getDoubleExtra("amount", 0.0).toString()
            selectedNote = intent.getStringExtra("note") ?: ""
            selectedDate = intent.getLongExtra("timeStamp", System.currentTimeMillis())
            originalExpense = Expense(
                id = editingExpenseId,
                remoteId = editingRemoteId,
                amount = intent.getDoubleExtra("amount", 0.0),
                name = selectedName,
                category = selectedCategory,
                note = selectedNote,
                type = transactionType,
                timeStamp = selectedDate,
                synced = true
            )
        }

        initViews()
        if (isEditMode) {
            updateConfirmButtonText()
        }
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
        
        // Populate fields if editing
        if (isEditMode) {
            updateTypeDisplay()
            updateCategoryDisplay()
            updateAmountDisplay()
            updateDateDisplay()
            noteInput.setText(selectedNote)
        }
    }

    private fun updateConfirmButtonText() {
        confirmBtn.text = "Cập nhật"
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
            intent.putExtra("currentCategory", selectedCategory)
            startActivityForResult(intent, CATEGORY_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CATEGORY_REQUEST_CODE && resultCode == RESULT_OK) {
            val updatedCategory = data?.getStringExtra("selectedCategory")?.trim()
            if (!updatedCategory.isNullOrEmpty()) {
                selectedCategory = updatedCategory
                updateCategoryDisplay()
            }
        }
    }

    private fun updateCategoryDisplay() {
        val tvCategory = catgBtn.findViewById<TextView>(R.id.tvCategory)
        tvCategory.text = if (selectedCategory.isEmpty()) "CHỌN DANH MỤC" else selectedCategory
    }

    private fun setupDatePicker() {
        calendarFooter.setOnClickListener {
            Vibration.vibrate(this, 50)
            
            // Convert Bangkok timestamp to UTC for date picker selection
            val bangkok = TimeZone.getTimeZone("Asia/Bangkok")
            val bangkokCal = Calendar.getInstance(bangkok)
            bangkokCal.timeInMillis = selectedDate
            
            val dayOfMonth = bangkokCal.get(Calendar.DAY_OF_MONTH)
            val month = bangkokCal.get(Calendar.MONTH)
            val year = bangkokCal.get(Calendar.YEAR)
            
            // Create UTC calendar for the same date
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            utcCal.set(year, month, dayOfMonth, 0, 0, 0)
            utcCal.set(Calendar.MILLISECOND, 0)
            val utcSelection = utcCal.timeInMillis
            
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setSelection(utcSelection)
                .setTitleText("Chọn ngày")
                .build()
            
            datePicker.addOnPositiveButtonClickListener { selection ->
                // Convert UTC selection back to Bangkok midnight
                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                utcCal.timeInMillis = selection
                
                val dayOfMonth = utcCal.get(Calendar.DAY_OF_MONTH)
                val month = utcCal.get(Calendar.MONTH)
                val year = utcCal.get(Calendar.YEAR)
                
                val bangkokCal = Calendar.getInstance(bangkok)
                bangkokCal.set(year, month, dayOfMonth, 0, 0, 0)
                bangkokCal.set(Calendar.MILLISECOND, 0)
                
                selectedDate = bangkokCal.timeInMillis
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
        confirmBtn.isEnabled = false
        lifecycleScope.launch {
            val api = RetrofitClient.getApiService(applicationContext)
            val request = ExpenseRequest(
                amount = selectedAmount.toDoubleOrNull() ?: 0.0,
                category = selectedCategory.trim(),
                timeStamp = selectedDate,
                note = selectedNote.takeIf { it.isNotBlank() },
                type = transactionType
            )

            try {
                val response = withContext(Dispatchers.IO) {
                    if (isEditMode && editingRemoteId != null) {
                        api.updateExpense(editingRemoteId!!, request)
                    } else {
                        api.createExpense(request)
                    }
                }

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@AddTransActivity,
                        if (isEditMode) "Cập nhật thành công" else "Đã thêm giao dịch",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@AddTransActivity,
                        "Lỗi: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    confirmBtn.isEnabled = true
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@AddTransActivity,
                    "Lỗi kết nối: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                confirmBtn.isEnabled = true
            }
        }
    }
}