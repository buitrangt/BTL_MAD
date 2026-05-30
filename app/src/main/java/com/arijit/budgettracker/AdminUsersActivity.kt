package com.arijit.budgettracker

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.arijit.budgettracker.api.AdminUserDto
import com.arijit.budgettracker.models.AdminUsersViewModel
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Màn hình Admin xem danh sách người dùng và khóa/mở khóa tài khoản.
 */
class AdminUsersActivity : AppCompatActivity() {

    private lateinit var vm: AdminUsersViewModel
    private lateinit var listContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var etSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_users)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        listContainer = findViewById(R.id.listContainer)
        progressBar = findViewById(R.id.progressBar)
        etSearch = findViewById(R.id.etSearch)

        findViewById<ImageButton>(R.id.btnSearch).setOnClickListener {
            vm.load(etSearch.text.toString().trim())
        }
        etSearch.setOnEditorActionListener { _, _, _ ->
            vm.load(etSearch.text.toString().trim())
            true
        }

        // Lắng nghe dữ liệu từ ViewModel để cập nhật giao diện
        vm = ViewModelProvider(this)[AdminUsersViewModel::class.java]
        vm.users.observe(this) { render(it) }
        vm.loading.observe(this) { progressBar.visibility = if (it) View.VISIBLE else View.GONE }
        vm.error.observe(this) {
            if (!it.isNullOrBlank()) Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
        vm.lockResult.observe(this) {
            if (it != null) {
                val msg = if (it.second) "Đã khóa" else "Đã mở khóa"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

        vm.load()
    }

    // Vẽ danh sách người dùng ra màn hình; mỗi dòng có công tắc khóa/mở khóa
    private fun render(list: List<AdminUserDto>) {
        listContainer.removeAllViews()
        if (list.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Không có người dùng"
                setTextColor(Color.parseColor("#999999"))
                textSize = 13f
                setPadding(0, dp(24), 0, dp(24))
            }
            listContainer.addView(empty)
            return
        }
        val inflater = LayoutInflater.from(this)
        for (u in list) {
            val v = inflater.inflate(R.layout.item_admin_user_manage, listContainer, false)
            val displayName = u.name?.takeIf { it.isNotBlank() } ?: u.email
            v.findViewById<TextView>(R.id.tvName).text = displayName
            v.findViewById<TextView>(R.id.tvEmail).text = u.email
            v.findViewById<TextView>(R.id.tvRole).text = (u.role ?: "USER").uppercase()
            v.findViewById<TextView>(R.id.tvAvatar).text =
                displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

            val sw = v.findViewById<SwitchMaterial>(R.id.swLock)
            sw.setOnCheckedChangeListener(null)
            sw.isChecked = u.locked == true
            sw.text = if (sw.isChecked) "Đã khóa" else "Hoạt động"
            sw.setOnCheckedChangeListener { _, checked ->
                if (checked == (u.locked == true)) return@setOnCheckedChangeListener
                confirmLock(u, checked) { confirmed ->
                    if (confirmed) {
                        sw.text = if (checked) "Đã khóa" else "Hoạt động"
                        vm.setLocked(u.id, checked)
                    } else {
                        sw.setOnCheckedChangeListener(null)
                        sw.isChecked = !checked
                        sw.setOnCheckedChangeListener { _, _ -> }
                        vm.load(etSearch.text.toString().trim())
                    }
                }
            }
            listContainer.addView(v)
        }
    }

    // Hiện hộp thoại xác nhận trước khi khóa/mở khóa tài khoản
    private fun confirmLock(u: AdminUserDto, lock: Boolean, onResult: (Boolean) -> Unit) {
        val title = if (lock) "Khóa tài khoản?" else "Mở khóa tài khoản?"
        val msg = if (lock)
            "Khóa ${u.email}? Tài khoản này sẽ không thể đăng nhập."
        else
            "Mở khóa ${u.email}?"
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("Xác nhận") { d, _ -> d.dismiss(); onResult(true) }
            .setNegativeButton("Hủy") { d, _ -> d.dismiss(); onResult(false) }
            .setCancelable(false)
            .show()
    }

    // Đổi đơn vị dp sang pixel
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
