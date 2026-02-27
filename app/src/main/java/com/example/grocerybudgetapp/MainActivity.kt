package com.example.grocerybudgetapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.grocerybudgetapp.adapter.GroceryAdapter
import com.example.grocerybudgetapp.auth.AuthManager
import com.example.grocerybudgetapp.storage.StorageService
import com.example.grocerybudgetapp.util.BudgetNotificationHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GroceryAdapter
    private lateinit var storage: StorageService

    private lateinit var totalBudgetText: TextView
    private lateinit var remainingBudgetText: TextView
    private lateinit var budgetProgressBar: LinearProgressIndicator
    private lateinit var tvStats: TextView
    private lateinit var emptyState: View

    private var totalBudget: Double = 0.0
    private var remainingBudget: Double = 0.0
    private var lastRemainingBudget: Double = 0.0
    private var lastAlertLevel: Int = 0
    private var groceryList = mutableListOf<GroceryItem>()
    private var displayList = mutableListOf<GroceryItem>()
    private var searchQuery = ""
    private var sortMode = SortMode.DATE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!AuthManager.isLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        totalBudgetText = findViewById(R.id.totalBudget)
        remainingBudgetText = findViewById(R.id.remainingBudget)
        budgetProgressBar = findViewById(R.id.budgetProgressBar)
        tvStats = findViewById(R.id.tvStats)
        recyclerView = findViewById(R.id.recyclerView)
        emptyState = findViewById(R.id.emptyState)

        storage = StorageService(this)
        totalBudget = storage.getTotalBudget()
        groceryList = storage.getGroceryList()
        lastRemainingBudget = totalBudget - groceryList.sumOf { it.price }
        BudgetNotificationHelper.ensureChannel(this)

        adapter = GroceryAdapter(
            itemList = displayList,
            onDeleteClick = { item -> showDeleteConfirmation(item) },
            onEditClick = { item, position ->
                val actualIndex = groceryList.indexOf(item)
                if (actualIndex < 0) return@GroceryAdapter
                val intent = Intent(this, AddItemActivity::class.java).apply {
                    putExtra("edit", true)
                    putExtra("editIndex", actualIndex)
                    putExtra("itemId", item.id)
                    putExtra("itemName", item.name)
                    putExtra("itemPrice", item.price)
                    putExtra("itemQuantity", item.quantity)
                    putExtra("paymentMode", item.paymentMode)
                    putExtra("itemCategory", item.category)
                    putExtra("itemAddedAt", item.addedAt)
                }
                startActivityForResult(intent, 102)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

// ✅ NOW call refresh AFTER adapter is ready
        refreshDisplayList()
        updateBudget()

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            startActivityForResult(Intent(this, AddItemActivity::class.java), 100)
        }

        findViewById<FloatingActionButton>(R.id.fabBudget).setOnClickListener {
            startActivityForResult(Intent(this, BudgetActivity::class.java), 101)
        }
    }

    private fun showDeleteConfirmation(item: GroceryItem) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(getString(R.string.delete_confirm_message, item.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.remove) { _, _ ->
                val displayPosition = displayList.indexOf(item)
                groceryList.remove(item)
                storage.saveGroceryList(groceryList)
                refreshDisplayList()
                updateBudget()
                if (displayPosition >= 0) adapter.notifyItemRemoved(displayPosition)
                else adapter.notifyDataSetChanged()
            }
            .show()
    }

    private fun refreshDisplayList() {
        displayList.clear()
        val filtered = if (searchQuery.isBlank()) groceryList
        else groceryList.filter { it.name.contains(searchQuery, ignoreCase = true) }
        displayList.addAll(
            when (sortMode) {
                SortMode.NAME -> filtered.sortedBy { it.name.lowercase() }
                SortMode.PRICE_LOW -> filtered.sortedBy { it.price }
                SortMode.PRICE_HIGH -> filtered.sortedByDescending { it.price }
                SortMode.DATE -> filtered.sortedByDescending { it.addedAt }
            }
        )
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }

    private fun updateBudget() {
        val spent = groceryList.sumOf { it.price }
        remainingBudget = totalBudget - spent

        totalBudgetText.text = getString(R.string.currency_format, formatAmount(totalBudget.toInt()))
        remainingBudgetText.text = getString(R.string.currency_format, formatAmount(remainingBudget.toInt()))

        budgetProgressBar.max = 100
        budgetProgressBar.progress = if (totalBudget > 0) {
            (spent / totalBudget * 100).toInt().coerceIn(0, 100)
        } else 0

        val usagePercent = if (totalBudget > 0) (spent / totalBudget * 100).toInt() else 0
        when {
            totalBudget <= 0 -> remainingBudgetText.setTextColor(Color.GRAY)
            remainingBudget < 0 -> {
                remainingBudgetText.setTextColor(resources.getColor(R.color.budget_critical, theme))
                if (lastRemainingBudget >= 0) {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.budget_exceeded_title)
                        .setMessage(getString(R.string.budget_exceeded_message, formatAmount(-remainingBudget.toInt())))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                    BudgetNotificationHelper.showBudgetExceededNotification(this, formatAmount(-remainingBudget.toInt()))
                }
                lastAlertLevel = 3
            }
            usagePercent >= 90 -> {
                remainingBudgetText.setTextColor(resources.getColor(R.color.budget_critical, theme))
                if (lastAlertLevel < 3) {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.alert_90_title)
                        .setMessage(getString(R.string.alert_90_message, formatAmount(remainingBudget.toInt())))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                    lastAlertLevel = 2
                }
            }
            usagePercent >= 75 -> {
                remainingBudgetText.setTextColor(resources.getColor(R.color.budget_warning, theme))
                if (lastAlertLevel < 2) {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.alert_75_title)
                        .setMessage(getString(R.string.alert_75_message, formatAmount(remainingBudget.toInt())))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                    lastAlertLevel = 1
                }
            }
            remainingBudget < totalBudget * 0.20 -> {
                remainingBudgetText.setTextColor(resources.getColor(R.color.budget_warning, theme))
                if (usagePercent < 75) lastAlertLevel = 0
            }
            else -> {
                remainingBudgetText.setTextColor(resources.getColor(R.color.budget_healthy, theme))
                if (usagePercent < 75) lastAlertLevel = 0
            }
        }
        lastRemainingBudget = remainingBudget

        tvStats.text = getString(R.string.items_count, groceryList.size, formatAmount(spent.toInt()))
        emptyState.visibility = if (displayList.isEmpty()) View.VISIBLE else View.GONE

        if (totalBudget > 0 && remainingBudget >= 0 && remainingBudget < totalBudget * 0.20) {
            Snackbar.make(
                findViewById(android.R.id.content),
                getString(R.string.budget_warning_message, remainingBudget.toInt()),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun formatAmount(amount: Int): String = "%,d".format(amount)

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        searchView?.queryHint = getString(R.string.search_hint)
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText?.trim().orEmpty()
                refreshDisplayList()
                updateBudget()
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_reports -> {
                startActivity(Intent(this, ReportsActivity::class.java))
                return true
            }
            R.id.action_logout -> {
                AuthManager.logout(this)
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
                return true
            }
            R.id.sort_name -> { sortMode = SortMode.NAME; refreshDisplayList(); return true }
            R.id.sort_price_low -> { sortMode = SortMode.PRICE_LOW; refreshDisplayList(); return true }
            R.id.sort_price_high -> { sortMode = SortMode.PRICE_HIGH; refreshDisplayList(); return true }
            R.id.sort_date -> { sortMode = SortMode.DATE; refreshDisplayList(); return true }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return

        when (requestCode) {
            100 -> {
                val name = data.getStringExtra("itemName") ?: return
                val price = data.getDoubleExtra("itemPrice", 0.0)
                val quantity = data.getDoubleExtra("itemQuantity", 1.0)
                val paymentMode = data.getStringExtra("paymentMode") ?: PaymentMode.CASH
                val category = data.getStringExtra("itemCategory") ?: ItemCategory.FOOD
                val id = data.getStringExtra("itemId").orEmpty()
                val addedAt = data.getLongExtra("itemAddedAt", System.currentTimeMillis())
                groceryList.add(GroceryItem(name, price, quantity, paymentMode, category, id, addedAt))
                storage.saveGroceryList(groceryList)
                refreshDisplayList()
                updateBudget()
                adapter.notifyItemInserted(displayList.indexOf(groceryList.last()).coerceAtLeast(0))
            }
            102 -> {
                val index = data.getIntExtra("editIndex", -1)
                if (index !in groceryList.indices) return
                val name = data.getStringExtra("itemName") ?: return
                val price = data.getDoubleExtra("itemPrice", 0.0)
                val quantity = data.getDoubleExtra("itemQuantity", 1.0)
                val paymentMode = data.getStringExtra("paymentMode") ?: PaymentMode.CASH
                val category = data.getStringExtra("itemCategory") ?: ItemCategory.FOOD
                val id = data.getStringExtra("itemId").orEmpty()
                val addedAt = data.getLongExtra("itemAddedAt", System.currentTimeMillis())
                groceryList[index] = GroceryItem(name, price, quantity, paymentMode, category, id, addedAt)
                storage.saveGroceryList(groceryList)
                refreshDisplayList()
                updateBudget()
                adapter.notifyDataSetChanged()
            }
            101 -> {
                totalBudget = data.getDoubleExtra("totalBudget", 0.0)
                storage.saveTotalBudget(totalBudget)
                updateBudget()
            }
        }
    }

    private enum class SortMode { NAME, PRICE_LOW, PRICE_HIGH, DATE }
}
