package com.arijit.budgettracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Deprecated: This activity is kept for backward compatibility only.
 * All functionality has been moved to AddTransActivity.
 * This class simply redirects to the new activity.
 */
class AddExpenseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_expense)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        expenseAmount = findViewById(R.id.expense_amt)
        cancelBtn = findViewById(R.id.close)
        cancelBtn.setOnClickListener {
            Vibration.vibrate(this, 100)
            finish()
        }

        setupKeypad()

        category = findViewById(R.id.catg_btn)
        category.setOnClickListener {
            Vibration.vibrate(this, 50)
            val bottomSheet = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.catg_layout, null)
            bottomSheet.setContentView(view)
            bottomSheet.show()

            val categoriesMap = mapOf(
                R.id.transport to "Transport",
                R.id.entertainment to "Entertainment",
                R.id.food to "Food",
                R.id.housing to "Housing",
                R.id.pet to "Pet",
                R.id.health to "Health",
                R.id.shopping to "Shopping",
                R.id.miscellaneous to "Miscellaneous"
            )

            for ((viewId, categoryName) in categoriesMap) {
                view.findViewById<LinearLayout>(viewId).setOnClickListener {
                    catgSelected = categoryName
                    bottomSheet.dismiss()
                    findViewById<TextView>(R.id.catg_txt).text = catgSelected
                }
            }

        }

        tickBtn = findViewById(R.id.tick)
        tickBtn.setOnClickListener {
            Vibration.vibrate(this, 100)
            finalExpenseAmt = expenseAmount.text.toString()

            if (finalExpenseAmt != "" && catgSelected != "") {
                lifecycleScope.launch {
                    val db = ExpenseDatabase.getDatabase(applicationContext)
                    val dao = db.expenseDao()
                    val expense =
                        Expense(amount = finalExpenseAmt.toDouble(), name = catgSelected, category = catgSelected)
                    dao.insertExpense(expense)
                    SyncManager.syncIfOnline(applicationContext)
                    finish()
                }
            }
        }
    }
}