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
import com.arijit.budgettracker.api.AdminCategoryDto
import com.arijit.budgettracker.models.AdminCategoriesViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AdminCategoriesActivity : AppCompatActivity() {

    private lateinit var vm: AdminCategoriesViewModel
    private lateinit var listContainer: LinearLayout
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_categories)

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
        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener { showAddDialog() }

        vm = ViewModelProvider(this)[AdminCategoriesViewModel::class.java]
        vm.items.observe(this) { render(it) }
        vm.loading.observe(this) { progressBar.visibility = if (it) View.VISIBLE else View.GONE }
        vm.error.observe(this) {
            if (!it.isNullOrBlank()) Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
        vm.message.observe(this) {
            if (!it.isNullOrBlank()) {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                vm.clearMessage()
            }
        }

        vm.load()
    }

    private fun render(list: List<AdminCategoryDto>) {
        listContainer.removeAllViews()
        if (list.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Chưa có danh mục mặc định"
                setTextColor(Color.parseColor("#999999"))
                textSize = 13f
                setPadding(0, dp(24), 0, dp(24))
            }
            listContainer.addView(empty)
            return
        }
        val inflater = LayoutInflater.from(this)
        for (c in list) {
            val v = inflater.inflate(R.layout.item_admin_category, listContainer, false)
            v.findViewById<TextView>(R.id.tvName).text = c.name
            v.findViewById<TextView>(R.id.tvNote).text = c.note ?: ""
            v.findViewById<TextView>(R.id.tvIcon).text =
                c.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
            v.findViewById<ImageButton>(R.id.btnDelete).setOnClickListener {
                confirmDelete(c)
            }
            listContainer.addView(v)
        }
    }

    private fun showAddDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etNote = view.findViewById<EditText>(R.id.etNote)

        AlertDialog.Builder(this)
            .setTitle("Thêm danh mục mặc định")
            .setView(view)
            .setPositiveButton("Thêm") { d, _ ->
                val name = etName.text.toString().trim()
                val note = etNote.text.toString().trim().ifEmpty { null }
                if (name.isEmpty()) {
                    Toast.makeText(this, "Tên danh mục không được trống", Toast.LENGTH_SHORT).show()
                } else {
                    vm.create(name, note)
                }
                d.dismiss()
            }
            .setNegativeButton("Hủy") { d, _ -> d.dismiss() }
            .show()
    }

    private fun confirmDelete(c: AdminCategoryDto) {
        AlertDialog.Builder(this)
            .setTitle("Xóa danh mục?")
            .setMessage("Xóa \"${c.name}\"? Hành động này không thể hoàn tác.")
            .setPositiveButton("Xóa") { d, _ -> d.dismiss(); vm.delete(c.id) }
            .setNegativeButton("Hủy") { d, _ -> d.dismiss() }
            .show()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
