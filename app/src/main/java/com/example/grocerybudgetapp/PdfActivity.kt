package com.example.grocerybudgetapp

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.grocerybudgetapp.auth.AuthManager
import com.example.grocerybudgetapp.storage.StorageService
import java.io.File
import java.io.FileOutputStream

class PdfActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        webView = findViewById(R.id.webView)

        val email = AuthManager.getCurrentUserEmail(this)

        if (email.isBlank()) {
            AuthManager.logout(this)
            finish()
            return
        }

        val storage = StorageService(this, email)
        val groceryList = storage.getGroceryList()
        val sharedPref = getSharedPreferences("BUDGET_DATA", MODE_PRIVATE)

        val month = sharedPref.getString("last_month", null)
        val year = sharedPref.getString("last_year", null)

        val budget = if (month != null && year != null) {
            val key = "budget_${month}_${year}"
            sharedPref.getFloat(key, 0f)
        } else {
            0f
        }
        val spent = groceryList.sumOf { it.price }

        val remaining = budget - spent

        val rows = groceryList.joinToString("") { item ->
            """
            <tr>
                <td>${item.name}</td>
                <td>${item.price}</td>
                <td>${item.quantity}</td>
            </tr>
            """
        }

        val htmlContent = """
            <html>
            <head>
                <style>
                    body { font-family: sans-serif; padding: 20px; }
                    h2 { text-align: center; color: #2E7D32; }
                    .summary { margin-bottom: 20px; }
                    table { width: 100%; border-collapse: collapse; }
                    th { background: #2E7D32; color: white; padding: 10px; }
                    td { padding: 10px; text-align: center; }
                    tr:nth-child(even) { background: #f2f2f2; }
                </style>
            </head>
            <body>

            <h2>Grocery Budget Report</h2>

            <div class="summary">
                <p><b>Total Budget:</b> $budget</p>
                <p><b>Total Spent:</b> $spent</p>
                <p><b>Remaining:</b> $remaining</p>
            </div>

            <table border="1">
                <tr>
                    <th>Item</th>
                    <th>Price</th>
                    <th>Quantity</th>
                </tr>
                $rows
            </table>

            </body>
            </html>
        """.trimIndent()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                webView.postDelayed({
                    askFileNameAndSavePdf()
                }, 1500)
            }
        }

        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
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
    private fun askFileNameAndSavePdf() {

        val input = android.widget.EditText(this)
        input.hint = "Enter file name (e.g. Monthly Report)"

        android.app.AlertDialog.Builder(this)
            .setTitle("Save PDF")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->

                val fileName = input.text.toString().trim()

                val finalName = if (fileName.isBlank()) {
                    "Grocery_Report_${System.currentTimeMillis()}"
                } else {
                    fileName.replace(" ", "_")
                }

                savePdfToDocuments(finalName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun savePdfToDocuments(fileName: String) {

        val document = android.graphics.pdf.PdfDocument()

        val width = webView.width
        val height = (webView.contentHeight * webView.scale).toInt()

        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo
            .Builder(width, height, 1)
            .create()

        val page = document.startPage(pageInfo)
        webView.draw(page.canvas)
        document.finishPage(page)

        val resolver = contentResolver

        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.pdf")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Documents/GroceryApp")
        }

        val uri = resolver.insert(
            android.provider.MediaStore.Files.getContentUri("external"),
            contentValues
        )

        if (uri != null) {

            resolver.openOutputStream(uri).use { outputStream ->
                document.writeTo(outputStream!!)
            }

            android.widget.Toast.makeText(
                this,
                "Saved as $fileName.pdf",
                android.widget.Toast.LENGTH_LONG
            ).show()

        } else {
            android.widget.Toast.makeText(this, "Save Failed", android.widget.Toast.LENGTH_LONG).show()
        }

        document.close()

    }
}