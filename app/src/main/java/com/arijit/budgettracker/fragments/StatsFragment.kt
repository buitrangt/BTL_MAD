package com.arijit.budgettracker.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.arijit.budgettracker.R
import com.arijit.budgettracker.api.CategoryStat
import com.arijit.budgettracker.api.WeeklyOverviewResponse
import com.arijit.budgettracker.models.StatsViewModel
import com.arijit.budgettracker.utils.CurrencyPrefs
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.util.Locale

class StatsFragment : Fragment() {
    private lateinit var viewModel: StatsViewModel

    private lateinit var totalAmount: TextView
    private lateinit var badgePercent: TextView
    private lateinit var barChartContainer: LinearLayout
    private lateinit var weekLabelsContainer: LinearLayout
    private lateinit var pieChart: PieChart
    private lateinit var legendContainer: LinearLayout
    private lateinit var categoryList: LinearLayout

    private val CHART_COLORS = intArrayOf(
        Color.parseColor("#1A8754"),
        Color.parseColor("#E53935"),
        Color.parseColor("#1976D2"),
        Color.parseColor("#FF9800"),
        Color.parseColor("#9C27B0"),
        Color.parseColor("#00BCD4"),
        Color.parseColor("#795548"),
        Color.parseColor("#607D8B")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)

        totalAmount = view.findViewById(R.id.total_amount)
        badgePercent = view.findViewById(R.id.badge_percent)
        barChartContainer = view.findViewById(R.id.bar_chart_container)
        weekLabelsContainer = view.findViewById(R.id.week_labels_container)
        pieChart = view.findViewById(R.id.pie_chart)
        legendContainer = view.findViewById(R.id.legend_container)
        categoryList = view.findViewById(R.id.category_list)

        viewModel = ViewModelProvider(this)[StatsViewModel::class.java]

        viewModel.weeklyOverview.observe(viewLifecycleOwner) { data ->
            if (data != null) renderAll(data)
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null) {
                totalAmount.text = err
            }
        }

        viewModel.loadWeeklyOverview()

        return view
    }

    private fun renderAll(data: WeeklyOverviewResponse) {
        // Total amount
        totalAmount.text = CurrencyPrefs.format(data.totalAmount)

        // Badge percent
        val pct = data.percentChange
        if (pct >= 0) {
            badgePercent.text = "↗ +${String.format("%.1f", pct)}%"
            badgePercent.setTextColor(Color.parseColor("#E53935"))
            badgePercent.setBackgroundResource(R.drawable.bg_badge_percent)
        } else {
            badgePercent.text = "↘ ${String.format("%.1f", pct)}%"
            badgePercent.setTextColor(Color.parseColor("#1A8754"))
            badgePercent.setBackgroundResource(R.drawable.bg_badge_percent_green)
        }

        // Bar chart
        renderBarChart(data.dailyBreakdown)

        // Pie chart
        renderPieChart(data.categoryBreakdown, data.totalAmount)

        // Category list
        renderCategoryList(data.categoryBreakdown)
    }

    private fun renderBarChart(daily: Map<String, Double>) {
        barChartContainer.removeAllViews()
        weekLabelsContainer.removeAllViews()

        val maxVal = daily.values.maxOrNull() ?: 1.0
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val tf = ResourcesCompat.getFont(requireContext(), R.font.montserrat_regular)

        // Find today's day
        val todayCal = java.util.Calendar.getInstance()
        val todayDayFormat = java.text.SimpleDateFormat("EEE", Locale.ENGLISH)
        val todayLabel = todayDayFormat.format(todayCal.time)

        for (day in days) {
            val amount = daily[day] ?: 0.0
            val heightRatio = if (maxVal > 0) (amount / maxVal) else 0.0
            val barHeight = (heightRatio * 70).toInt().coerceAtLeast(4)

            // Bar
            val bar = View(requireContext())
            val barParams = LinearLayout.LayoutParams(0, dpToPx(barHeight), 1f)
            barParams.marginStart = dpToPx(4)
            barParams.marginEnd = dpToPx(4)
            bar.layoutParams = barParams
            bar.setBackgroundResource(
                if (day == todayLabel || isPastDay(day, todayLabel))
                    R.drawable.bg_bar_active
                else R.drawable.bg_bar_inactive
            )
            barChartContainer.addView(bar)

            // Label
            val label = TextView(requireContext())
            val labelParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            label.layoutParams = labelParams
            label.text = day
            label.textSize = 10f
            label.gravity = Gravity.CENTER
            label.typeface = tf
            if (day == todayLabel) {
                label.setTextColor(Color.parseColor("#1A8754"))
                label.typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_bold)
            } else {
                label.setTextColor(Color.parseColor("#AAAAAA"))
            }
            weekLabelsContainer.addView(label)
        }
    }

    private fun isPastDay(day: String, today: String): Boolean {
        val order = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dayIdx = order.indexOf(day)
        val todayIdx = order.indexOf(today)
        return dayIdx < todayIdx
    }

    private fun renderPieChart(
        categories: List<CategoryStat>,
        total: Double
    ) {
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        for ((i, cat) in categories.withIndex()) {
            if (cat.percent > 0) {
                entries.add(PieEntry(cat.percent.toFloat(), cat.category))
                colors.add(CHART_COLORS[i % CHART_COLORS.size])
            }
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.sliceSpace = 2f
        dataSet.setDrawValues(false)

        pieChart.data = PieData(dataSet)
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.setDrawHoleEnabled(true)
        pieChart.holeRadius = 65f
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setTransparentCircleAlpha(0)
        pieChart.setCenterText("Total\n${CurrencyPrefs.format(total)}")
        pieChart.setCenterTextSize(11f)
        pieChart.setCenterTextColor(Color.parseColor("#1A1A1A"))
        pieChart.setCenterTextTypeface(ResourcesCompat.getFont(requireContext(), R.font.montserrat_bold))
        pieChart.setDrawEntryLabels(false)
        pieChart.legend.isEnabled = false
        pieChart.animateY(800)
        pieChart.invalidate()

        // Custom legend
        legendContainer.removeAllViews()
        for ((i, cat) in categories.withIndex()) {
            if (cat.percent <= 0) continue
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dpToPx(10) }
            }

            // Dot
            val dot = View(requireContext()).apply {
                val size = dpToPx(8)
                layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = dpToPx(8) }
                setBackgroundColor(CHART_COLORS[i % CHART_COLORS.size])
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(CHART_COLORS[i % CHART_COLORS.size])
                }
            }
            row.addView(dot)

            // Name
            val name = TextView(requireContext()).apply {
                text = cat.category
                textSize = 12f
                setTextColor(Color.parseColor("#1A1A1A"))
                typeface = ResourcesCompat.getFont(context, R.font.montserrat_regular)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(name)

            // Percent
            val pct = TextView(requireContext()).apply {
                text = "${String.format("%.0f", cat.percent)}%"
                textSize = 12f
                setTextColor(Color.parseColor("#1A1A1A"))
                typeface = ResourcesCompat.getFont(context, R.font.montserrat_bold)
            }
            row.addView(pct)

            legendContainer.addView(row)
        }
    }

    private fun renderCategoryList(
        categories: List<CategoryStat>
    ) {
        categoryList.removeAllViews()
        val colorNames = listOf("green", "red", "blue", "orange", "purple", "cyan", "brown", "gray")

        for ((i, cat) in categories.withIndex()) {
            if (cat.amount <= 0) continue
            val item = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_category_stat, categoryList, false)

            item.findViewById<TextView>(R.id.category_name).text = cat.category
            item.findViewById<TextView>(R.id.category_amount).text = "-${CurrencyPrefs.format(cat.amount)}"

            val progressBar = item.findViewById<View>(R.id.progress_fill)
            val params = progressBar.layoutParams as LinearLayout.LayoutParams
            params.weight = cat.percent.toFloat()
            progressBar.layoutParams = params
            progressBar.setBackgroundColor(CHART_COLORS[i % CHART_COLORS.size])

            val spacer = item.findViewById<View>(R.id.progress_spacer)
            val spacerParams = spacer.layoutParams as LinearLayout.LayoutParams
            spacerParams.weight = (100 - cat.percent).toFloat()
            spacer.layoutParams = spacerParams

            categoryList.addView(item)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
