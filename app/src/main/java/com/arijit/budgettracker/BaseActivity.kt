package com.arijit.budgettracker;

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
        // Hàm này để các Activity con gọi sau khi setContentView
        fun setupChatFab() {
        findViewById<View>(R.id.fabFinChat)?.setOnClickListener {
        startActivity(Intent(this, FinChatActivity::class.java))
        }
        }
        }