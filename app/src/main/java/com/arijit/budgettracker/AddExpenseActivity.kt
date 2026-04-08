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
        startActivity(Intent(this, AddTransActivity::class.java))
        finish()
    }
}