package com.arijit.budgettracker

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.arijit.budgettracker.db.Expense
import com.arijit.budgettracker.db.ExpenseDatabase
import com.arijit.budgettracker.utils.SyncManager
import com.arijit.budgettracker.utils.Vibration
import com.arijit.budgettracker.utils.ViewPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ViewPagerAdapter
    private lateinit var headerTxt: TextView
    private lateinit var settings: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewPager = findViewById(R.id.pager)
        adapter = ViewPagerAdapter(this)
        headerTxt = findViewById(R.id.header_txt)
        settings = findViewById(R.id.settings)

        settings.setOnClickListener {
            Vibration.vibrate(this, 50)
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }

        viewPager.adapter = adapter

        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        val tabIcons = arrayOf(R.drawable.home, R.drawable.history, R.drawable.stats)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.setIcon(tabIcons[position])
        }.attach()

        // === FAKE DATA: Xoá block này sau khi test xong ===
        val prefs = getSharedPreferences("fake_data_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("fake_data_inserted", false)) {
            lifecycleScope.launch {
                val dao = ExpenseDatabase.getDatabase(applicationContext).expenseDao()
                val categories = listOf("Transport", "Entertainment", "Food", "Housing", "Pet", "Health", "Shopping", "Miscellaneous")
                val amounts = listOf(
                    listOf(50.0, 30.0, 120.0),       // Day 0 (6 days ago)
                    listOf(80.0, 45.0),                // Day 1
                    listOf(200.0, 60.0, 35.0, 90.0),  // Day 2
                    listOf(15.0, 150.0),               // Day 3
                    listOf(70.0, 25.0, 110.0),         // Day 4
                    listOf(40.0, 180.0, 55.0),         // Day 5 (yesterday)
                    listOf(95.0, 65.0, 130.0, 20.0)    // Day 6 (today)
                )
                for (dayOffset in 0..6) {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -(6 - dayOffset))
                    cal.set(Calendar.HOUR_OF_DAY, 10)
                    val dayAmounts = amounts[dayOffset]
                    for ((i, amount) in dayAmounts.withIndex()) {
                        val category = categories[((dayOffset * 3) + i) % categories.size]
                        dao.insertExpense(
                            Expense(
                                amount = amount,
                                category = category,
                                timeStamp = cal.timeInMillis + (i * 3600000L)
                            )
                        )
                    }
                }
                prefs.edit().putBoolean("fake_data_inserted", true).apply()
            }
        }
        // === END FAKE DATA ===

        // Sync unsynced expenses when app opens
        lifecycleScope.launch {
            SyncManager.syncIfOnline(applicationContext)
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                headerTxt.animate()
                    .alpha(0f)
                    .setDuration(100)
                    .withEndAction {
                        headerTxt.text = when (tab?.position) {
                            0 -> "Home"
                            1 -> "History"
                            2 -> "Stats"
                            else -> "Home"
                        }
                        headerTxt.animate()
                            .alpha(1f)
                            .setDuration(100)
                            .start()
                    }.start()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
}