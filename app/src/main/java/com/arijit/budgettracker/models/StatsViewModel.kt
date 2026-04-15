package com.arijit.budgettracker.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arijit.budgettracker.api.CategoryStat
import com.arijit.budgettracker.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    data class StatsScreenData(
        val weeklyTotal: Double,
        val weeklyPercentChange: Double,
        val weeklyDailyBreakdown: Map<String, Double>,
        val monthlyTotal: Double,
        val monthlyCategoryBreakdown: List<CategoryStat>
    )

    private val _statsData = MutableLiveData<StatsScreenData?>()
    val statsData: LiveData<StatsScreenData?> = _statsData

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadRealtime() {
        loadForDate(System.currentTimeMillis())
    }

    fun loadForDate(referenceTimeMillis: Long) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApiService(getApplication())
                val response = api.getAllTransactions()
                if (response.isSuccessful) {
                    val txs = response.body().orEmpty()

                    val tz = TimeZone.getTimeZone("Asia/Bangkok")

                    // ===== Weekly: week containing selected date =====
                    val weekStart = startOfWeek(referenceTimeMillis, tz)
                    val weekEnd = weekStart + 7L * 24 * 60 * 60 * 1000
                    val prevWeekStart = weekStart - 7L * 24 * 60 * 60 * 1000
                    val prevWeekEnd = weekStart

                    val thisWeekTx = txs.asSequence()
                        .filter { it.type.equals("expense", true) }
                        .filter { it.timeStamp in weekStart until weekEnd }
                        .toList()

                    val prevWeekTx = txs.asSequence()
                        .filter { it.type.equals("expense", true) }
                        .filter { it.timeStamp in prevWeekStart until prevWeekEnd }
                        .toList()

                    val weeklyTotal = thisWeekTx.sumOf { it.amount }
                    val prevWeekTotal = prevWeekTx.sumOf { it.amount }
                    val weeklyPercentChange = if (prevWeekTotal > 0) {
                        ((weeklyTotal - prevWeekTotal) * 100.0 / prevWeekTotal)
                    } else 0.0

                    val dayKeys = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    val daily = linkedMapOf<String, Double>().apply { dayKeys.forEach { put(it, 0.0) } }
                    for (t in thisWeekTx) {
                        val key = dayKeyOf(t.timeStamp, tz)
                        if (daily.containsKey(key)) {
                            daily[key] = (daily[key] ?: 0.0) + t.amount
                        }
                    }

                    // ===== Monthly: month/year containing selected date =====
                    val monthStart = startOfMonth(referenceTimeMillis, tz)
                    val monthEnd = startOfNextMonth(referenceTimeMillis, tz)
                    val monthTx = txs.asSequence()
                        .filter { it.type.equals("expense", true) }
                        .filter { it.timeStamp in monthStart until monthEnd }
                        .toList()

                    val monthlyTotal = monthTx.sumOf { it.amount }
                    val categoryTotals = linkedMapOf<String, Double>()
                    for (t in monthTx) {
                        val name = t.categoryName ?: "Khác"
                        categoryTotals[name] = (categoryTotals[name] ?: 0.0) + t.amount
                    }

                    val monthlyCategoryBreakdown = categoryTotals.entries
                        .sortedByDescending { it.value }
                        .map { (cat, amt) ->
                            val pct = if (monthlyTotal > 0) (amt * 100.0 / monthlyTotal) else 0.0
                            CategoryStat(category = cat, amount = amt, percent = pct)
                        }

                    _statsData.postValue(
                        StatsScreenData(
                            weeklyTotal = weeklyTotal,
                            weeklyPercentChange = weeklyPercentChange,
                            weeklyDailyBreakdown = daily,
                            monthlyTotal = monthlyTotal,
                            monthlyCategoryBreakdown = monthlyCategoryBreakdown
                        )
                    )
                } else {
                    _error.postValue("Không thể tải dữ liệu")
                }
            } catch (e: Exception) {
                _error.postValue("Lỗi kết nối: ${e.message}")
            } finally {
                _loading.postValue(false)
            }
        }
    }

    private fun startOfWeek(referenceTimeMillis: Long, tz: TimeZone): Long {
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = referenceTimeMillis
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun startOfMonth(referenceTimeMillis: Long, tz: TimeZone): Long {
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = referenceTimeMillis
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun startOfNextMonth(referenceTimeMillis: Long, tz: TimeZone): Long {
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = referenceTimeMillis
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MONTH, 1)
        return cal.timeInMillis
    }

    private fun dayKeyOf(timeMillis: Long, tz: TimeZone): String {
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = timeMillis
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> "Sun"
        }
    }
}
