package com.example.grocerybudgetapp

import android.content.Intent
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Collections.list
import java.util.Calendar
class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var storage: StorageService
    private lateinit var adapter: GroceryAdapter
    private var groceryList = mutableListOf<GroceryItem>()
    private lateinit var totalBudgetText: TextView
    private lateinit var emptyState: View
    private var totalBudget: Double = 0.0

    private var filteredList = mutableListOf<GroceryItem>()

    var spent: Double = 0.0
    private var searchQuery = ""

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
        val sharedPref = getSharedPreferences("BUDGET_DATA", MODE_PRIVATE)

        val month = sharedPref.getString("last_month", null)
        val year = sharedPref.getString("last_year", null)

        val budget = if (month != null && year != null) {
            val key = "budget_${month}_${year}"
            sharedPref.getFloat(key, 0f)
        } else {
            0f
        }


        totalBudgetText = findViewById(R.id.totalBudget)
        recyclerView = findViewById(R.id.recyclerView)
        emptyState = findViewById(R.id.emptyState)

        val email = AuthManager.getCurrentUserEmail(this)
        storage = StorageService(this, email)
        groceryList = storage.getGroceryList().toMutableList()

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = GroceryAdapter(
            itemList = groceryList,
            storage = storage,
            onDeleteClick = { item -> deleteItem(item) },
            onEditClick = { item, position -> editItem(item, position) },
            overBudgetIndexProvider = { getOverBudgetIndex() }
        )

        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            startActivityForResult(Intent(this, AddItemActivity::class.java), 100)
        }

        findViewById<FloatingActionButton>(R.id.fabBudget).setOnClickListener {
            startActivity(Intent(this, BudgetActivity::class.java))
        }
        loadBudget()
        setupBottomNav()

    }
    private fun loadBudget() {

        val sharedPref = getSharedPreferences("BUDGET_DATA", MODE_PRIVATE)

        val month = sharedPref.getString("last_month", null)
        val year = sharedPref.getString("last_year", null)

        totalBudget = if (month != null && year != null) {
            val key = "budget_${month}_${year}"
            sharedPref.getFloat(key, 0f).toDouble()
        } else {
            0.0
        }

        val tvMonthYear = findViewById<TextView>(R.id.tvMonthYear)

        tvMonthYear.text = if (month != null && year != null) {
            "$month $year"
        } else {
            "No Budget Set"
        }

        totalBudgetText.text = "PKR $totalBudget"
    }

    private fun updateBudgetUI() {

        spent = groceryList.sumOf { it.price }

        val remaining = totalBudget - spent

        findViewById<TextView>(R.id.totalBudget).text =
            "PKR: $totalBudget"

        findViewById<TextView>(R.id.remainingBudget).text =
            "PKR: $remaining"

        val progress = if (totalBudget > 0) {
            ((spent / totalBudget) * 100).toInt()
        } else 0

        findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(
            R.id.budgetProgressBar
        ).progress = progress

        //  ALERT LOGIC (THIS WAS MISSING)
        val warningText = findViewById<TextView>(R.id.txtWarning)

        when {

            totalBudget == 0.0 -> {
                warningText.text = "Set your budget first"
            }

            spent > totalBudget -> {
                warningText.text = "⚠ Budget Exceeded!"
            }

            remaining <= totalBudget * 0.2 -> {
                warningText.text = "⚠ Low Budget Warning!"
            }

            else -> {
                warningText.text = ""
            }

        }
        if (spent > totalBudget && totalBudget > 0) {

            val sharedPref = getSharedPreferences("BUDGET_DATA", MODE_PRIVATE)
            val alreadyWarned = sharedPref.getBoolean("budget_warning_shown", false)

            if (!alreadyWarned) {

                AlertDialog.Builder(this)
                    .setTitle("Budget Exceeded")
                    .setMessage("You have exceeded your budget by Rs: ${(spent - totalBudget)}. Do you want to increase it?")
                    .setPositiveButton("Increase") { _, _ ->
                        startActivity(Intent(this, BudgetActivity::class.java))
                    }
                    .setNegativeButton("Cancel", null)
                    .show()



                sharedPref.edit()
                    .putBoolean("budget_warning_shown", true)
                    .apply()
            }
        }

    }
    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {

                R.id.nav_home -> true

                R.id.nav_budget -> {
                    startActivity(Intent(this, BudgetActivity::class.java))
                    true
                }

                R.id.nav_reports -> {
                    startActivity(Intent(this, ReportsActivity::class.java))
                    true
                }

                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }

                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        groceryList.clear()
        groceryList.addAll(storage.getGroceryList())

        updateBudgetUI()
        loadBudget()
        refreshUI()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.post {
            bottomNav.selectedItemId = R.id.nav_home
            bottomNav.menu.findItem(R.id.nav_home).isChecked = true
        }
    }
    private fun refreshUI() {

        filteredList = if (searchQuery.isEmpty()) {
            groceryList
        } else {
            groceryList.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }.toMutableList()
        }

        adapter = GroceryAdapter(
            itemList = filteredList,
            storage = storage,
            onDeleteClick = { item -> deleteItem(item) },
            onEditClick = { item, position -> editItem(item, position) },
            overBudgetIndexProvider = { getOverBudgetIndex() }
        )

        recyclerView.adapter = adapter

        emptyState.visibility =
            if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun deleteItem(item: GroceryItem) {

        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure?")
            .setPositiveButton("Yes") { _, _ ->

                groceryList.remove(item)
                storage.saveGroceryList(groceryList)

                updateBudgetUI()
                refreshUI()
            }
            .setNegativeButton("No", null)
            .show()
    }
    private fun editItem(item: GroceryItem, position: Int) {

        val intent = Intent(this, AddItemActivity::class.java)

        intent.putExtra("editMode", true)
        intent.putExtra("position", position)

        intent.putExtra("id", item.id)
        intent.putExtra("name", item.name)
        intent.putExtra("price", item.price)
        intent.putExtra("quantity", item.quantity)
        intent.putExtra("category", item.category)
        intent.putExtra("paymentMode", item.paymentMode)
        intent.putExtra("addedAt", item.addedAt)

        startActivityForResult(intent, 200)
    }
    private fun sortList(type: String) {

        when (type) {

            "date" -> groceryList.sortByDescending { it.addedAt }

            "az" -> groceryList.sortBy { it.name.lowercase() }

            "low_high" -> groceryList.sortBy { it.price }

            "high_low" -> groceryList.sortByDescending { it.price }
        }

        storage.saveGroceryList(groceryList)  // IMPORTANT FIX

        refreshUI()
    }

    private fun getOverBudgetIndex(): Int {

        var runningTotal = 0.0

        val sharedPref = getSharedPreferences("BUDGET_DATA", MODE_PRIVATE)

        val month = sharedPref.getString("last_month", null)
        val year = sharedPref.getString("last_year", null)

        val budget = if (month != null && year != null) {
            val key = "budget_${month}_${year}"
            sharedPref.getFloat(key, 0f).toDouble()
        } else {
            0.0
        }

        groceryList.forEachIndexed { index, item ->
            runningTotal += item.price

            if (runningTotal > budget) {
                return index
            }
        }

        return -1
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchView = menu?.findItem(R.id.action_search)?.actionView as? SearchView

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText.orEmpty()
                refreshUI()
                return true
            }
        })

        return true
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {

            groceryList.clear()
            groceryList.addAll(storage.getGroceryList())

            updateBudgetUI()
            refreshUI()
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_reports -> {
                startActivity(Intent(this, ReportsActivity::class.java))
                return true
            }

            R.id.sort_date -> {
                sortList("date")
                return true
            }

            R.id.sort_price_high -> {
                sortList("high_low")
                return true
            }

            R.id.sort_price_low -> {
                sortList("low_high")
                return true
            }

            R.id.sort_name -> {
                sortList("az")
                return true
            }


            R.id.action_logout -> {
                AuthManager.logout(this)
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}