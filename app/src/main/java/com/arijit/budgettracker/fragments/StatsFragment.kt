package com.arijit.budgettracker.fragments

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.arijit.budgettracker.R
import com.arijit.budgettracker.api.CategoryStat
import com.arijit.budgettracker.api.WeeklyOverviewResponse
import com.arijit.budgettracker.models.StatsViewModel
import com.arijit.budgettracker.utils.CurrencyPrefs
import com.arijit.budgettracker.utils.AppRefreshBus
import com.arijit.budgettracker.utils.SyncManager
import kotlinx.coroutines.launch
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import android.widget.PopupWindow
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone
import java.util.Locale

class StatsFragment : Fragment() {
    private lateinit var viewModel: StatsViewModel

    private lateinit var totalAmount: TextView
    private lateinit var weekRange: TextView
    private lateinit var barChartContainer: LinearLayout
    private lateinit var weekLabelsContainer: LinearLayout
    private lateinit var pieChart: PieChart
    private lateinit var legendContainer: LinearLayout
    private lateinit var categoryList: LinearLayout
    private lateinit var distributionTitle: TextView
    private lateinit var topCategoriesTitle: TextView
    private lateinit var btnDatePicker: ImageButton
    private var selectedDayIdx: Int? = null
    private var selectedDateMillis: Long = System.currentTimeMillis()
    private var isDateSelected: Boolean = false
    private var barTooltip: PopupWindow? = null
    private val uiHandler = Handler(Looper.getMainLooper())

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
        weekRange = view.findViewById(R.id.tv_week_range)
        barChartContainer = view.findViewById(R.id.bar_chart_container)
        weekLabelsContainer = view.findViewById(R.id.week_labels_container)
        pieChart = view.findViewById(R.id.pie_chart)
        legendContainer = view.findViewById(R.id.legend_container)
        categoryList = view.findViewById(R.id.category_list)
        distributionTitle = view.findViewById(R.id.tv_distribution_title)
        topCategoriesTitle = view.findViewById(R.id.tv_top_categories_title)
        btnDatePicker = view.findViewById(R.id.btnDatePickerStats)

        viewModel = ViewModelProvider(this)[StatsViewModel::class.java]

        // "Xem tất cả" → switch to History tab
        view.findViewById<View>(R.id.view_all).setOnClickListener {
            (activity as? com.arijit.budgettracker.MainActivity)?.navigateToHistory()
        }

        btnDatePicker.setOnClickListener {
            if (isDateSelected) {
                resetToRealtime()
            } else {
                showDatePicker()
            }
        }

        viewModel.statsData.observe(viewLifecycleOwner) { data ->
            if (data != null) renderAll(data)
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null) {
                totalAmount.text = err
            }
        }

        viewModel.loadRealtime()

        // Global refresh: any trans/category changes should update Stats immediately
        AppRefreshBus.refreshTick.observe(viewLifecycleOwner) {
            if (isDateSelected) viewModel.loadForDate(selectedDateMillis) else viewModel.loadRealtime()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // Push any pending local changes (e.g. from SMS receiver) and reload from server
        lifecycleScope.launch {
            SyncManager.syncIfOnline(requireContext().applicationContext)
            if (isDateSelected) viewModel.loadForDate(selectedDateMillis) else viewModel.loadRealtime()
        }
    }

    private fun renderAll(data: StatsViewModel.StatsScreenData) {
        // Weekly total (week containing selected date)
        val activeIdx = selectedDayIdx ?: computeIdxFromSelectedDate()
        totalAmount.text = CurrencyPrefs.format(data.weeklyTotal)
        weekRange.text = buildWeekRangeText(selectedDateMillis)

        // Monthly titles (month/year containing selected date)
        val (m, y) = monthYearOf(selectedDateMillis)
        distributionTitle.text = "Phân bổ chi tiêu tháng $m/$y"
        topCategoriesTitle.text = "Danh mục chi tiêu nhiều nhất tháng $m/$y"

        // Bar chart
        renderBarChart(data.weeklyDailyBreakdown, activeIdx)

        // Pie chart (monthly for selected month/year)
        renderPieChart(data.monthlyCategoryBreakdown, data.monthlyTotal)

        // Category list (monthly)
        renderCategoryList(data.monthlyCategoryBreakdown)
    }

    private fun renderBarChart(daily: Map<String, Double>, activeIdx: Int) {
        barChartContainer.removeAllViews()
        weekLabelsContainer.removeAllViews()
        barTooltip?.dismiss()

        val maxVal = daily.values.maxOrNull() ?: 1.0
        // English keys must match backend response
        val dayKeys = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dayLabels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        val tf = ResourcesCompat.getFont(requireContext(), R.font.montserrat_regular)

        for ((idx, key) in dayKeys.withIndex()) {
            val amount = daily[key] ?: 0.0
            val heightRatio = if (maxVal > 0) (amount / maxVal) else 0.0
            val barHeight = (heightRatio * 70).toInt().coerceAtLeast(4)

            // Bar
            val bar = View(requireContext())
            val barParams = LinearLayout.LayoutParams(0, dpToPx(barHeight), 1f)
            barParams.marginStart = dpToPx(4)
            barParams.marginEnd = dpToPx(4)
            bar.layoutParams = barParams
            bar.setBackgroundResource(
                if (idx <= activeIdx) R.drawable.bg_bar_active
                else R.drawable.bg_bar_inactive
            )
            bar.setOnClickListener {
                showBarTooltip(
                    anchor = bar,
                    text = "${dayLabels[idx]}: ${CurrencyPrefs.format(amount)}"
                )
            }
            barChartContainer.addView(bar)

            // Label (Vietnamese)
            val label = TextView(requireContext())
            val labelParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            label.layoutParams = labelParams
            label.text = dayLabels[idx]
            label.textSize = 10f
            label.gravity = Gravity.CENTER
            label.typeface = tf
            if (idx == activeIdx) {
                label.setTextColor(Color.parseColor("#1A8754"))
                label.typeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat_bold)
            } else {
                label.setTextColor(Color.parseColor("#AAAAAA"))
            }
            label.setOnClickListener {
                selectedDayIdx = idx
                renderBarChart(daily, idx)
            }
            weekLabelsContainer.addView(label)
        }
    }

    private fun showBarTooltip(anchor: View, text: String) {
        // Dismiss any existing tooltip and cancel pending hides
        uiHandler.removeCallbacksAndMessages(null)
        barTooltip?.dismiss()

        val ctx = requireContext()
        val tv = TextView(ctx).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = ResourcesCompat.getFont(ctx, R.font.montserrat_bold)
            setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6))
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(10).toFloat()
                setColor(Color.parseColor("#1A8754"))
            }
        }

        // Wrap in container to ensure proper measuring
        val content = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(tv)
        }

        content.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val popup = PopupWindow(
            content,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            isOutsideTouchable = true
            isClippingEnabled = true
            elevation = dpToPx(6).toFloat()
        }

        // Center tooltip above the bar
        val xOff = (anchor.width - content.measuredWidth) / 2
        val yOff = -(anchor.height + content.measuredHeight + dpToPx(10))
        popup.showAsDropDown(anchor, xOff, yOff)

        barTooltip = popup

        // Auto-hide after a short delay
        uiHandler.postDelayed({
            barTooltip?.dismiss()
            barTooltip = null
        }, 1600)
    }

    private fun computeTodayIdx(): Int {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Bangkok"))
        cal.firstDayOfWeek = java.util.Calendar.MONDAY
        // Calendar.DAY_OF_WEEK: Sunday=1..Saturday=7. Convert to Mon=0..Sun=6.
        return (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
    }

    private fun computeIdxFromSelectedDate(): Int {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Bangkok"))
        cal.timeInMillis = selectedDateMillis
        cal.firstDayOfWeek = java.util.Calendar.MONDAY
        return (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
    }

    private fun buildWeekRangeText(referenceMillis: Long): String {
        val tz = TimeZone.getTimeZone("Asia/Bangkok")
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = referenceMillis
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 6)
        val end = cal.timeInMillis

        val fmt = SimpleDateFormat("dd/MM", Locale("vi", "VN"))
        fmt.timeZone = tz
        return "T2 ${fmt.format(java.util.Date(start))} - CN ${fmt.format(java.util.Date(end))}"
    }

    private fun monthYearOf(referenceMillis: Long): Pair<Int, Int> {
        val tz = TimeZone.getTimeZone("Asia/Bangkok")
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = referenceMillis
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        return month to year
    }

    private fun showDatePicker() {
        val tz = java.util.TimeZone.getTimeZone("Asia/Bangkok")
        val cal = java.util.Calendar.getInstance(tz)
        cal.timeInMillis = selectedDateMillis

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val picked = java.util.Calendar.getInstance(tz)
                picked.set(year, month, dayOfMonth, 0, 0, 0)
                picked.set(java.util.Calendar.MILLISECOND, 0)
                selectedDateMillis = picked.timeInMillis
                isDateSelected = true
                selectedDayIdx = computeIdxFromSelectedDate()
                btnDatePicker.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                viewModel.loadForDate(selectedDateMillis)
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun resetToRealtime() {
        selectedDateMillis = System.currentTimeMillis()
        isDateSelected = false
        selectedDayIdx = null
        btnDatePicker.setImageResource(android.R.drawable.ic_menu_my_calendar)
        viewModel.loadRealtime()
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

    override fun onDestroyView() {
        super.onDestroyView()
        uiHandler.removeCallbacksAndMessages(null)
        barTooltip?.dismiss()
        barTooltip = null
    }
}
