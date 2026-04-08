package com.arijit.budgettracker

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.arijit.budgettracker.utils.SyncManager
import com.arijit.budgettracker.utils.Vibration
import com.arijit.budgettracker.utils.ViewPagerAdapter
import kotlinx.coroutines.launch
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