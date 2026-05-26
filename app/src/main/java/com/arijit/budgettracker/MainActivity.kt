package com.arijit.budgettracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.arijit.budgettracker.db.TransactionDatabase
import com.arijit.budgettracker.db.SmsTemplate
import com.arijit.budgettracker.sms.SmsNotificationHelper
import com.arijit.budgettracker.utils.SyncManager
import com.arijit.budgettracker.utils.Vibration
import com.arijit.budgettracker.utils.ViewPagerAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.ImageButton
import androidx.core.widget.ImageViewCompat

class MainActivity : BaseActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ViewPagerAdapter
    private lateinit var headerTxt: TextView
    private lateinit var navHome: ImageButton
    private lateinit var navHistory: ImageButton
    private lateinit var navAdd: ImageButton
    private lateinit var navStats: ImageButton
    private lateinit var navProfile: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setupChatFab()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewPager = findViewById(R.id.pager)
        adapter = ViewPagerAdapter(this)
        headerTxt = findViewById(R.id.header_txt)

        viewPager.adapter = adapter

        navHome = findViewById(R.id.nav_home)
        navHistory = findViewById(R.id.nav_history)
        navAdd = findViewById(R.id.nav_add)
        navStats = findViewById(R.id.nav_stats)
        navProfile = findViewById(R.id.nav_profile)

        navAdd.setOnClickListener {
            Vibration.vibrate(this, 50)
            startActivity(Intent(this, AddTransActivity::class.java))
        }

        navHome.setOnClickListener { viewPager.currentItem = 0 }
        navHistory.setOnClickListener { viewPager.currentItem = 1 }
        navStats.setOnClickListener { viewPager.currentItem = 2 }
        navProfile.setOnClickListener { viewPager.currentItem = 3 }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                headerTxt.text = when (position) {
                    0 -> "Home"
                    1 -> "History"
                    2 -> "Stats"
                    3 -> ""
                    else -> "Home"
                }
                updateNavSelection(position)
            }
        })

        updateNavSelection(0)

        // Sync unsynced expenses when app opens
        lifecycleScope.launch {
            SyncManager.syncIfOnline(applicationContext)
        }

        // Request SMS + notification permissions
        requestSmsPermissions()

        // Setup notification channel
        SmsNotificationHelper.createChannel(this)

        // Seed default SMS templates
        lifecycleScope.launch(Dispatchers.IO) { seedSmsTemplates() }
    }

    private fun requestSmsPermissions() {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.RECEIVE_SMS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (perms.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, perms.toTypedArray(), 1001)
        }
    }

    private suspend fun seedSmsTemplates() {
        val dao = TransactionDatabase.getDatabase(this).smsTemplateDao()
        val existing = dao.getActiveTemplates()
        if (existing.isNotEmpty()) return

        val templates = listOf(
            SmsTemplate(
                id = 1L,
                senderPattern = "Vietcombank",
                amountRegex = """([0-9]{1,3}(?:[.,][0-9]{3})+)\s*(?:VND|VNĐ|đ)?""",
                type = "DEBIT",
                bankName = "Vietcombank"
            ),
            SmsTemplate(
                id = 2L,
                senderPattern = "Techcombank",
                amountRegex = """([0-9]{1,3}(?:[.,][0-9]{3})+)\s*(?:VND|VNĐ|đ)?""",
                type = "DEBIT",
                bankName = "Techcombank"
            ),
            SmsTemplate(
                id = 3L,
                senderPattern = "MBBank",
                amountRegex = """([0-9]{1,3}(?:[.,][0-9]{3})+)\s*(?:VND|VNĐ|đ)?""",
                type = "DEBIT",
                bankName = "MB Bank"
            ),
            SmsTemplate(
                id = 4L,
                senderPattern = "BIDV",
                amountRegex = """([0-9]{1,3}(?:[.,][0-9]{3})+)\s*(?:VND|VNĐ|đ)?""",
                type = "DEBIT",
                bankName = "BIDV"
            ),
            SmsTemplate(
                id = 5L,
                senderPattern = "VPBank",
                amountRegex = """([0-9]{1,3}(?:[.,][0-9]{3})+)\s*(?:VND|VNĐ|đ)?""",
                type = "DEBIT",
                bankName = "VPBank"
            ),
            SmsTemplate(
                id = 6L,
                senderPattern = "TEST",
                amountRegex = """([0-9]{1,3}(?:[.,][0-9]{3})+)\s*(?:VND|VNĐ|đ)?""",
                type = "DEBIT",
                bankName = "Test Bank"
            )
        )
        dao.upsertAll(templates)
    }

    fun navigateToHistory() {
        viewPager.currentItem = 1
    }

    private fun updateNavSelection(position: Int) {
        navHome.isSelected = position == 0
        navHistory.isSelected = position == 1
        navStats.isSelected = position == 2
        navProfile.isSelected = position == 3

        ImageViewCompat.setImageTintList(navHome, navHome.imageTintList)
        ImageViewCompat.setImageTintList(navHistory, navHistory.imageTintList)
        ImageViewCompat.setImageTintList(navStats, navStats.imageTintList)
        ImageViewCompat.setImageTintList(navProfile, navProfile.imageTintList)
    }
}