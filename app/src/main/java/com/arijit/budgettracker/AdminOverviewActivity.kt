package com.arijit.budgettracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import com.arijit.budgettracker.api.AdminOverviewResponse
import com.arijit.budgettracker.api.AdminUserDto
import com.arijit.budgettracker.models.AdminViewModel
import com.arijit.budgettracker.utils.TokenManager
import com.google.android.material.navigation.NavigationView

/**
 * Màn hình tổng quan (dashboard) của Admin: thống kê người dùng,
 * biểu đồ đăng ký theo tuần và danh sách người dùng gần đây.
 */
class AdminOverviewActivity : AppCompatActivity() {

    private lateinit var vm: AdminViewModel

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var tvWelcome: TextView
    private lateinit var tvTotalUsers: TextView
    private lateinit var tvTotalChange: TextView
    private lateinit var tvNewUsers: TextView
    private lateinit var tvNewUsersChange: TextView
    private lateinit var tvActiveUsers: TextView
    private lateinit var tvActiveUsersChange: TextView
    private lateinit var barChartContainer: LinearLayout
    private lateinit var barLabelsContainer: LinearLayout
    private lateinit var userListContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_overview)

        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        toolbar = findViewById(R.id.toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        toolbar.setNavigationOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        tvWelcome = findViewById(R.id.tvWelcome)
        tvTotalUsers = findViewById(R.id.tvTotalUsers)
        tvTotalChange = findViewById(R.id.tvTotalChange)
        tvNewUsers = findViewById(R.id.tvNewUsers)
        tvNewUsersChange = findViewById(R.id.tvNewUsersChange)
        tvActiveUsers = findViewById(R.id.tvActiveUsers)
        tvActiveUsersChange = findViewById(R.id.tvActiveUsersChange)
        barChartContainer = findViewById(R.id.barChartContainer)
        barLabelsContainer = findViewById(R.id.barLabelsContainer)
        userListContainer = findViewById(R.id.userListContainer)
        progressBar = findViewById(R.id.progressBar)
        btnLogout = findViewById(R.id.btnLogout)

        val name = TokenManager.getName(this) ?: "Admin"
        tvWelcome.text = "Chào mừng trở lại, $name"

        setupDrawerHeader(name)
        setupDrawerNavigation()

        btnLogout.setOnClickListener { doLogout() }

        vm = ViewModelProvider(this)[AdminViewModel::class.java]
        vm.overview.observe(this) { data -> if (data != null) render(data) }
        vm.loading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        vm.error.observe(this) { err ->
            if (!err.isNullOrBlank()) Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
        }

        vm.load()
    }

    private fun setupDrawerHeader(name: String) {
        val header = navView.getHeaderView(0)
        header.findViewById<TextView>(R.id.navName).text = name
        header.findViewById<TextView>(R.id.navEmail).text =
            TokenManager.getEmail(this) ?: ""
        header.findViewById<TextView>(R.id.navAvatar).text =
            name.firstOrNull()?.uppercaseChar()?.toString() ?: "A"
    }

    // Thiết lập menu điều hướng bên trái (Tổng quan / Người dùng / Danh mục / Đăng xuất)
    private fun setupDrawerNavigation() {
        navView.setCheckedItem(R.id.nav_overview)
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_overview -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_users -> {
                    startActivity(Intent(this, AdminUsersActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_categories -> {
                    startActivity(Intent(this, AdminCategoriesActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_logout -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    doLogout()
                }
            }
            true
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // Đăng xuất: xóa token và quay về màn hình đăng nhập
    private fun doLogout() {
        TokenManager.logout(this)
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    override fun onResume() {
        super.onResume()
        navView.setCheckedItem(R.id.nav_overview)
        if (::vm.isInitialized) vm.load()
    }

    // Đổ dữ liệu thống kê nhận từ server ra các thẻ số liệu và biểu đồ
    private fun render(d: AdminOverviewResponse) {
        tvTotalUsers.text = formatCount(d.totalUsers)
        tvTotalChange.text = formatChange(d.percentChangeVsLastMonth)

        tvNewUsers.text = formatCount(d.newUsersToday)
        applyChange(tvNewUsersChange, d.newUsersChangePercent)

        tvActiveUsers.text = formatCount(d.activeUsersToday)
        applyChange(tvActiveUsersChange, d.activeUsersChangePercent)

        renderBarChart(d.weeklyRegistrations)
        renderRecentUsers(d.recentUsers)
    }

    private fun applyChange(view: TextView, value: Double) {
        val sign = if (value >= 0) "+" else ""
        view.text = "$sign${String.format("%.0f", value)}%"
        view.setTextColor(
            if (value >= 0) Color.parseColor("#22C55E")
            else Color.parseColor("#EF4444")
        )
    }

    private fun formatChange(value: Double): String {
        val arrow = if (value >= 0) "↗" else "↘"
        val sign = if (value >= 0) "+" else ""
        return "$arrow $sign${String.format("%.0f", value)}%"
    }

    private fun formatCount(count: Long): String {
        return when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fk", count / 1_000.0)
            else -> count.toString()
        }
    }

    // Vẽ biểu đồ cột số lượng đăng ký theo từng ngày trong tuần
    private fun renderBarChart(map: Map<String, Long>) {
        barChartContainer.removeAllViews()
        barLabelsContainer.removeAllViews()

        val keys = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val labels = listOf("M", "T", "W", "T", "F", "S", "S")
        val maxValue = (map.values.maxOrNull() ?: 1L).coerceAtLeast(1L)

        for ((idx, key) in keys.withIndex()) {
            val value = map[key] ?: 0L
            val ratio = value.toDouble() / maxValue.toDouble()
            val heightDp = (ratio * 80).toInt().coerceAtLeast(4)

            val column = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            }

            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(20), dp(heightDp))
                setBackgroundResource(R.drawable.bg_admin_bar)
            }
            column.addView(bar)
            barChartContainer.addView(column)

            val label = TextView(this).apply {
                text = labels[idx]
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#AAAAAA"))
                textSize = 10f
                typeface = androidx.core.content.res.ResourcesCompat
                    .getFont(this@AdminOverviewActivity, R.font.montserrat_bold)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            barLabelsContainer.addView(label)
        }
    }

    // Vẽ danh sách người dùng đăng ký gần đây
    private fun renderRecentUsers(list: List<AdminUserDto>) {
        userListContainer.removeAllViews()
        if (list.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Chưa có người dùng"
                setTextColor(Color.parseColor("#999999"))
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, dp(20), 0, dp(20))
            }
            userListContainer.addView(empty)
            return
        }

        val inflater = LayoutInflater.from(this)
        for ((index, u) in list.withIndex()) {
            val item = inflater.inflate(R.layout.item_admin_user, userListContainer, false)
            val displayName = u.name?.takeIf { it.isNotBlank() } ?: u.email
            item.findViewById<TextView>(R.id.tvName).text = displayName
            item.findViewById<TextView>(R.id.tvEmail).text = u.email
            item.findViewById<TextView>(R.id.tvAvatar).text =
                displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

            userListContainer.addView(item)

            if (index < list.size - 1) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    )
                    setBackgroundColor(Color.parseColor("#F5F5F5"))
                }
                userListContainer.addView(divider)
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
