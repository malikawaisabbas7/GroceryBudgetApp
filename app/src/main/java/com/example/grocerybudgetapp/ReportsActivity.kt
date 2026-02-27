package com.example.grocerybudgetapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
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

class ReportsActivity : AppCompatActivity() {

    private lateinit var storage: StorageService
    private var list = listOf<GroceryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)
        storage = StorageService(this)
        list = storage.getGroceryList()

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val totalSpent = list.sumOf { it.price }
        val totalBudget = storage.getTotalBudget()
        findViewById<TextView>(R.id.tvTotalSpent).text = getString(R.string.currency_format, "%,d".format(totalSpent.toInt()))
        findViewById<TextView>(R.id.tvTotalBudgetReport).text = getString(R.string.currency_format, "%,d".format(totalBudget.toInt()))

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
            val estimatedRemaining = totalBudget - avgSpending
            findViewById<TextView>(R.id.tvPrediction).text = getString(R.string.predicted_spending) + ": PKR " + "%,d".format(avgSpending.toInt()) + "\n" +
                getString(R.string.estimated_remaining) + ": PKR " + "%,d".format(estimatedRemaining.toInt())

            val topCategory = byCategory.maxByOrNull { it.value.sumOf { i -> i.price } }?.key ?: "-"
            val topPayment = byMode.maxByOrNull { it.value.sumOf { i -> i.price } }?.key ?: "-"
            findViewById<TextView>(R.id.tvHabits).text = getString(R.string.most_spent_category, topCategory) + "\n" + getString(R.string.most_used_payment, topPayment)

            setupWeeklyChart(chartWeekly, list)
            setupMonthlyChart(chartMonthly, list)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_reports, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_export) {
            showExportOptions()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showExportOptions() {
        val options = arrayOf(getString(R.string.export_csv), getString(R.string.export_pdf), getString(R.string.share_report))
        AlertDialog.Builder(this)
            .setTitle(R.string.export_report)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportCsv()
                    1 -> exportPdf()
                    2 -> shareReport()
                }
            }
            .show()
    }

    private fun getReportCsvContent(): String {
        val header = "Name,Price,Quantity,Unit Price,Category,Payment Mode,Date\n"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val rows = list.joinToString("\n") { item ->
            "${item.name},${item.price},${item.quantity},${item.unitPrice},${item.category},${item.paymentMode},${dateFormat.format(item.addedAt)}"
        }
        return header + rows + "\n\nTotal Spent," + list.sumOf { it.price } + ",,,,,\n"
    }

    private fun exportCsv() {
        val content = getReportCsvContent()
        val file = File(cacheDir, "grocery_report_${System.currentTimeMillis()}.csv")
        file.writeText(content)
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        startActivity(Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }

    private fun exportPdf() {
        val file = File(cacheDir, "grocery_report_${System.currentTimeMillis()}.pdf")
        val doc = android.graphics.pdf.PdfDocument()
        try {
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(300, 400, 1).create()
            val page = doc.startPage(pageInfo)
            val paint = android.graphics.Paint().apply { textSize = 10f }

            var y = 20f
            getReportCsvContent().lines().take(40).forEach { line ->
                page.canvas.drawText(line, 20f, y, paint)
                y += 12f
            }

            doc.finishPage(page)

            java.io.FileOutputStream(file).use { output ->
                doc.writeTo(output)
            }
        } finally {
            doc.close() // Always close the document
        }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )

        startActivity(Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }

    private fun shareReport() {
        val content = getReportCsvContent()
        startActivity(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.expense_summary))
            putExtra(Intent.EXTRA_TEXT, content)
        })
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
}
