package com.example.grocerybudgetapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.grocerybudgetapp.storage.StorageService
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.grocerybudgetapp.auth.AuthManager
import com.example.grocerybudgetapp.util.DateUtils
import java.io.FileOutputStream

class ReportsActivity : AppCompatActivity() {

    private var totalBudget: Double = 0.0
    private lateinit var storage: StorageService
    private var list = listOf<GroceryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)
        val email = AuthManager.getCurrentUserEmail(this)
        storage = StorageService(this, email)
        list = storage.getGroceryList()
        totalBudget = getCurrentBudget().toDouble()

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val totalSpent = list.sumOf { it.price }
        val budget = getCurrentBudget()
        findViewById<TextView>(R.id.tvTotalSpent).text = getString(R.string.currency_format, "%,d".format(totalSpent.toInt()))
        findViewById<TextView>(R.id.tvTotalBudgetReport)
            .text = getString(R.string.currency_format, "%,d".format(budget.toInt()))

        val containerByMode = findViewById<LinearLayout>(R.id.containerByPaymentMode)
        val containerByCategory = findViewById<LinearLayout>(R.id.containerByCategory)
        val emptyView = findViewById<TextView>(R.id.tvNoData)
        val scrollView = findViewById<View>(R.id.scrollReports)
        val chartWeekly = findViewById<BarChart>(R.id.chartWeekly)
        val chartMonthly = findViewById<BarChart>(R.id.chartMonthly)

        if (list.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            scrollView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            scrollView.visibility = View.VISIBLE

            val byMode = list.groupBy { it.paymentMode }
            val maxByMode = byMode.values.maxOfOrNull { it.sumOf { i -> i.price } } ?: 1.0
            byMode.forEach { (mode, items) ->
                val sum = items.sumOf { it.price }
                val row = LayoutInflater.from(this).inflate(R.layout.row_report_payment_mode, containerByMode, false)
                row.findViewById<TextView>(R.id.rowModeName).text = mode
                row.findViewById<TextView>(R.id.rowModeAmount).text = getString(R.string.currency_format, "%,d".format(sum.toInt()))
                val bar = row.findViewById<LinearProgressIndicator>(R.id.rowModeBar)
                bar.max = 100
                bar.progress = if (maxByMode > 0) (sum / maxByMode * 100).toInt().coerceIn(0, 100) else 0
                containerByMode.addView(row)
            }

            val byCategory = list.groupBy { it.category }
            val maxByCat = byCategory.values.maxOfOrNull { it.sumOf { i -> i.price } } ?: 1.0
            byCategory.forEach { (cat, items) ->
                val sum = items.sumOf { it.price }
                val row = LayoutInflater.from(this).inflate(R.layout.row_report_payment_mode, containerByCategory, false)
                row.findViewById<TextView>(R.id.rowModeName).text = cat
                row.findViewById<TextView>(R.id.rowModeAmount).text = getString(R.string.currency_format, "%,d".format(sum.toInt()))
                val bar = row.findViewById<LinearProgressIndicator>(R.id.rowModeBar)
                bar.max = 100
                bar.progress = if (maxByCat > 0) (sum / maxByCat * 100).toInt().coerceIn(0, 100) else 0
                containerByCategory.addView(row)
            }

            val cal = Calendar.getInstance(Locale.getDefault())
            val thisMonthStart = getMonthStartMillis(cal, 0)
            val thisMonthEnd = System.currentTimeMillis() + 86400000L
            val lastMonthStart = getMonthStartMillis(cal, 1)
            val lastMonthEnd = thisMonthStart
            fun inRange(ts: Long, start: Long, end: Long) = (ts.takeIf { it > 0 } ?: System.currentTimeMillis()) in start until end
            val thisMonthSpent = list.filter { inRange(it.addedAt, thisMonthStart, thisMonthEnd) }.sumOf { it.price }
            val lastMonthSpent = list.filter { inRange(it.addedAt, lastMonthStart, lastMonthEnd) }.sumOf { it.price }
            val change = if (lastMonthSpent > 0) ((thisMonthSpent - lastMonthSpent) / lastMonthSpent * 100).toInt() else 0
            findViewById<TextView>(R.id.tvMonthlyComparison).text = getString(R.string.this_month) + ": PKR " + "%,d".format(thisMonthSpent.toInt()) + "\n" +
                getString(R.string.last_month) + ": PKR " + "%,d".format(lastMonthSpent.toInt()) + "\n" +
                getString(R.string.change) + ": " + (if (change >= 0) "+" else "") + change + "%"

            val monthlyTotals = (0 until 6).map { monthsAgo ->
                val start = getMonthStartMillis(cal, monthsAgo)
                val end = if (monthsAgo == 0) thisMonthEnd else getMonthStartMillis(cal, monthsAgo - 1)
                list.filter { inRange(it.addedAt, start, end) }.sumOf { it.price }
            }
            val avgSpending = monthlyTotals.filter { it > 0 }.let { if (it.isEmpty()) 0.0 else it.average() }
            val estimatedRemaining = (totalBudget - avgSpending).coerceAtLeast(0.0)
            findViewById<TextView>(R.id.tvPrediction).text = getString(R.string.predicted_spending) + ": PKR " + "%,d".format(avgSpending.toInt()) + "\n" +
                getString(R.string.estimated_remaining) + ": PKR " + "%,d".format(estimatedRemaining.toInt())

            val topCategory = byCategory.maxByOrNull { it.value.sumOf { i -> i.price } }?.key ?: "-"
            val topPayment = byMode.maxByOrNull { it.value.sumOf { i -> i.price } }?.key ?: "-"
            findViewById<TextView>(R.id.tvHabits).text = getString(R.string.most_spent_category, topCategory) + "\n" + getString(R.string.most_used_payment, topPayment)

            setupWeeklyChart(chartWeekly, list)
            setupMonthlyChart(chartMonthly, list)
        }
    }

    private fun getCurrentBudget(): Float {

        val sharedPref = getSharedPreferences("BUDGET_DATA", MODE_PRIVATE)

        val month = sharedPref.getString("last_month", null)
        val year = sharedPref.getString("last_year", null)

        return if (month != null && year != null) {
            val key = "budget_${month}_${year}"
            sharedPref.getFloat(key, 0f)
        } else {
            0f
        }
    }
    private fun getMonthStartMillis(cal: Calendar, monthsAgo: Int): Long {
        val c = cal.clone() as Calendar
        c.add(Calendar.MONTH, -monthsAgo)
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun getWeekStartMillis(cal: Calendar, weeksAgo: Int): Long {
        val c = cal.clone() as Calendar
        c.add(Calendar.WEEK_OF_YEAR, -weeksAgo)
        c.set(Calendar.DAY_OF_WEEK, c.firstDayOfWeek)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun setupWeeklyChart(chart: BarChart, list: List<GroceryItem>) {
        val cal = Calendar.getInstance(Locale.getDefault())
        val weeks = 4
        val weekLabels = mutableListOf<String>()
        val entries = mutableListOf<BarEntry>()
        for (i in weeks - 1 downTo 0) {
            val start = getWeekStartMillis(cal, i)
            val end = if (i == 0) System.currentTimeMillis() + 86400000L else getWeekStartMillis(cal, i - 1)
            val sum = list.filter { (it.addedAt.takeIf { t -> t > 0 } ?: System.currentTimeMillis()) in start until end }.sumOf { it.price }
            weekLabels.add("W${weeks - i}")
            entries.add(BarEntry(weeks - 1 - i.toFloat(), sum.toFloat()))
        }
        val set = BarDataSet(entries, getString(R.string.total_spent)).apply {
            color = Color.parseColor("#0D9488")
            valueFormatter = object : ValueFormatter() { override fun getFormattedValue(value: Float) = "%,.0f".format(value) }
        }
        chart.data = BarData(set)
        chart.xAxis.apply { position = XAxis.XAxisPosition.BOTTOM; setDrawGridLines(false); valueFormatter = IndexAxisValueFormatter(weekLabels) }
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setFitBars(true)
        chart.invalidate()
    }

    private fun setupMonthlyChart(chart: BarChart, list: List<GroceryItem>) {
        val cal = Calendar.getInstance(Locale.getDefault())
        val months = 6
        val monthLabels = mutableListOf<String>()
        val entries = mutableListOf<BarEntry>()
        for (i in months - 1 downTo 0) {
            val start = getMonthStartMillis(cal, i)
            val end = if (i == 0) System.currentTimeMillis() + 86400000L else getMonthStartMillis(cal, i - 1)
            val sum = list.filter { (it.addedAt.takeIf { t -> t > 0 } ?: System.currentTimeMillis()) in start until end }.sumOf { it.price }
            val labelCal = Calendar.getInstance(Locale.getDefault()).apply { timeInMillis = start }
            monthLabels.add(SimpleDateFormat("MMM", Locale.getDefault()).format(labelCal.time))
            entries.add(BarEntry(months - 1 - i.toFloat(), sum.toFloat()))
        }
        val set = BarDataSet(entries, getString(R.string.total_spent)).apply {
            color = Color.parseColor("#F59E0B")
            valueFormatter = object : ValueFormatter() { override fun getFormattedValue(value: Float) = "%,.0f".format(value) }
        }
        chart.data = BarData(set)
        chart.xAxis.apply { position = XAxis.XAxisPosition.BOTTOM; setDrawGridLines(false); valueFormatter = IndexAxisValueFormatter(monthLabels) }
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setFitBars(true)
        chart.invalidate()
    }
    private fun getFormattedData(list: List<GroceryItem>): List<List<String>> {

        val data = mutableListOf<List<String>>()

        // HEADER (same as PDF)
        data.add(
            listOf(
                "Name",
                "Category",
                "Qty",
                "Unit Price",
                "Total Price",
                "Payment",
                "Date"
            )
        )

        // ROWS
        list.forEach { item ->

            data.add(
                listOf(
                    item.name,
                    item.category,
                    "%.0f".format(item.quantity),
                    "%.2f".format(item.price / item.quantity),
                    "%.2f".format(item.price),
                    item.paymentMode,
                    DateUtils.formatDate(item.addedAt)
                )
            )
        }

        return data
    }

}
