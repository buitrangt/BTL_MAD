package com.arijit.budgettracker

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.arijit.budgettracker.api.AnomalyDto
import com.arijit.budgettracker.api.BudgetSuggestionDto
import com.arijit.budgettracker.api.ClassificationDto
import com.arijit.budgettracker.api.InsightsSummaryDto
import com.arijit.budgettracker.api.PredictionDto
import com.arijit.budgettracker.models.InsightsViewModel
import com.arijit.budgettracker.utils.CurrencyPrefs

/**
 * Activity hiển thị các phân tích và dự báo từ AI.
 * Thuộc luồng chức năng: AI phân tích.
 * Chịu trách nhiệm:
 * 1. Dự báo chi tiêu trong tháng.
 * 2. Cảnh báo chi tiêu bất thường.
 * 3. Phân loại mức chi tiêu (Tiết kiệm, Bình thường, Lãng phí).
 * 4. Gợi ý thông minh (Nhận xét chung từ AI).
 * 5. Gợi ý ngân sách cho các danh mục.
 */
class InsightsActivity : AppCompatActivity() {

    private lateinit var vm: InsightsViewModel

    // Prediction
    private lateinit var tvPredicted: TextView
    private lateinit var tvPredictedNote: TextView

    // Alert
    private lateinit var alertBadgeTitle: TextView
    private lateinit var alertBadgeDesc: TextView
    private lateinit var alertAmount: TextView

    // Category pills
    private lateinit var pillSaving: TextView
    private lateinit var pillNormal: TextView
    private lateinit var pillWasteful: TextView
    private lateinit var pillSavingLabel: TextView
    private lateinit var pillNormalLabel: TextView
    private lateinit var pillWastefulLabel: TextView
    private lateinit var tvCategoryDescription: TextView

    // Suggestions
    private lateinit var budgetList: LinearLayout

    // AI narrative
    private lateinit var tvAiNarrative: TextView

    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_insights)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sb.left, sb.top, sb.right, sb.bottom)
            insets
        }

        // bind views
        tvPredicted = findViewById(R.id.tvPredicted)
        tvPredictedNote = findViewById(R.id.tvPredictedNote)

        alertBadgeTitle = findViewById(R.id.alertBadgeTitle)
        alertBadgeDesc = findViewById(R.id.alertBadgeDesc)
        alertAmount = findViewById(R.id.alertAmount)

        pillSaving = findViewById(R.id.pillSaving)
        pillNormal = findViewById(R.id.pillNormal)
        pillWasteful = findViewById(R.id.pillWasteful)
        pillSavingLabel = findViewById(R.id.pillSavingLabel)
        pillNormalLabel = findViewById(R.id.pillNormalLabel)
        pillWastefulLabel = findViewById(R.id.pillWastefulLabel)
        tvCategoryDescription = findViewById(R.id.tvCategoryDescription)

        budgetList = findViewById(R.id.budgetList)
        tvAiNarrative = findViewById(R.id.tvAiNarrative)
        progressBar = findViewById(R.id.progressBar)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnRefresh).setOnClickListener { vm.refresh() }

        vm = ViewModelProvider(this)[InsightsViewModel::class.java]
        vm.summary.observe(this) { data ->
            if (data != null) render(data)
        }
        vm.loading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        vm.error.observe(this) { err ->
            if (!err.isNullOrBlank()) {
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
            }
        }

        vm.load()
    }

    private fun render(data: InsightsSummaryDto) {
        renderPrediction(data.prediction)
        renderAlert(data.anomalies)
        renderClassification(data.classification)
        renderAiNarrative(data.aiNarrative)
        renderBudgets(data.budgetSuggestions)
    }

    // 1. Hiển thị dự báo chi tiêu trong tháng
    private fun renderPrediction(p: PredictionDto?) {
        if (p == null) {
            tvPredicted.text = "—"
            tvPredictedNote.text = "Chưa đủ dữ liệu để dự đoán"
            return
        }
        tvPredicted.text = CurrencyPrefs.format(p.predictedAmount)
        tvPredictedNote.text = "Dự kiến chi đến cuối tháng (hiện ${CurrencyPrefs.format(p.currentAmount)})"
    }

    // 2. Hiển thị cảnh báo chi tiêu bất thường
    private fun renderAlert(list: List<AnomalyDto>) {
        if (list.isEmpty()) {
            alertBadgeTitle.text = "Không có bất thường"
            alertBadgeDesc.text = "Chi tiêu trong mức ổn định"
            alertAmount.text = "+0đ"
            alertAmount.setTextColor(Color.parseColor("#22C55E"))
            return
        }
        // Pick the most severe anomaly (sorted by percent desc on backend)
        val top = list.first()
        alertBadgeTitle.text = "Cảnh báo: ${top.categoryName}"
        alertBadgeDesc.text = "Tăng ${String.format("%.0f", top.percentIncrease)}% so với trung bình"
        alertAmount.text = "+${CurrencyPrefs.format(top.amountDiff)}"
        alertAmount.setTextColor(Color.parseColor("#E53935"))
    }

    // 3. Hiển thị phân loại mức chi tiêu hiện tại
    private fun renderClassification(c: ClassificationDto?) {
        if (c == null) {
            tvCategoryDescription.text = "Chưa có dữ liệu phân loại."
            return
        }

        val activeLevel = when (c.level) {
            "SAVING" -> "saving"
            "NORMAL" -> "normal"
            "WASTEFUL", "OVERSPENT" -> "wasteful"
            else -> "none"
        }

        val activeBg = R.drawable.bg_category_pill_active
        val inactiveBg = R.drawable.bg_category_pill_inactive
        val activeText = Color.WHITE
        val inactiveText = Color.parseColor("#888888")
        val activeLabel = Color.parseColor("#22C55E")
        val inactiveLabel = Color.parseColor("#999999")

        // Reset all to inactive
        pillSaving.setBackgroundResource(inactiveBg)
        pillSaving.setTextColor(inactiveText)
        pillSavingLabel.setTextColor(inactiveLabel)

        pillNormal.setBackgroundResource(inactiveBg)
        pillNormal.setTextColor(inactiveText)
        pillNormalLabel.setTextColor(inactiveLabel)

        pillWasteful.setBackgroundResource(inactiveBg)
        pillWasteful.setTextColor(inactiveText)
        pillWastefulLabel.setTextColor(inactiveLabel)

        when (activeLevel) {
            "saving" -> {
                pillSaving.setBackgroundResource(activeBg)
                pillSaving.setTextColor(activeText)
                pillSavingLabel.setTextColor(activeLabel)
            }
            "normal" -> {
                pillNormal.setBackgroundResource(activeBg)
                pillNormal.setTextColor(activeText)
                pillNormalLabel.setTextColor(activeLabel)
            }
            "wasteful" -> {
                pillWasteful.setBackgroundResource(activeBg)
                pillWasteful.setTextColor(activeText)
                pillWastefulLabel.setTextColor(activeLabel)
            }
        }

        tvCategoryDescription.text = c.note ?: "—"
    }

    // 5. Hiển thị danh sách gợi ý ngân sách
    private fun renderBudgets(list: List<BudgetSuggestionDto>) {
        budgetList.removeAllViews()
        if (list.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Chưa có gợi ý ngân sách (cần thêm dữ liệu)"
                setTextColor(Color.parseColor("#888888"))
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 30, 0, 30)
            }
            budgetList.addView(empty)
            return
        }

        for ((index, b) in list.withIndex()) {
            val row = RelativeLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, dp(12), 0, dp(12))
            }

            val name = TextView(this).apply {
                text = b.categoryName
                textSize = 13f
                setTextColor(Color.parseColor("#1A1A1A"))
                typeface = androidx.core.content.res.ResourcesCompat
                    .getFont(this@InsightsActivity, R.font.montserrat_regular)
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_START)
                    addRule(RelativeLayout.CENTER_VERTICAL)
                }
            }
            row.addView(name)

            val amount = TextView(this).apply {
                text = "Đề xuất: ${CurrencyPrefs.format(b.suggestedAmount)}/tháng"
                textSize = 12f
                setTextColor(Color.parseColor("#888888"))
                typeface = androidx.core.content.res.ResourcesCompat
                    .getFont(this@InsightsActivity, R.font.montserrat_regular)
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                    addRule(RelativeLayout.CENTER_VERTICAL)
                }
            }
            row.addView(amount)

            budgetList.addView(row)

            // Divider (except after last item)
            if (index < list.size - 1) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                    )
                    setBackgroundColor(Color.parseColor("#F5F5F5"))
                }
                budgetList.addView(divider)
            }
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    // 4. Hiển thị gợi ý thông minh / nhận xét chung từ AI
    private fun renderAiNarrative(text: String?) {
        tvAiNarrative.text = text?.takeIf { it.isNotBlank() } ?: "Chưa cấu hình AI hoặc chưa đủ dữ liệu."
    }
}
