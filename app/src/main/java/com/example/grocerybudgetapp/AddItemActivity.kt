package com.example.grocerybudgetapp

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.grocerybudgetapp.auth.AuthManager
import com.example.grocerybudgetapp.storage.StorageService
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID

class AddItemActivity : AppCompatActivity() {

    private lateinit var etItemName: TextInputEditText
    private lateinit var etItemPrice: TextInputEditText
    private lateinit var etQuantity: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerPaymentMode: Spinner
    private lateinit var tvUnitPrice: TextView
    private lateinit var btnSaveItem: MaterialButton
    private lateinit var storage: StorageService

    private var isEditMode = false
    private var existingId = ""
    private var existingAddedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)

        val email = AuthManager.getCurrentUserEmail(this)

        if (email.isBlank()) {
            finish()
            return
        }

        storage = StorageService(this, email)

        // ── Views ─────────────────────────────────────────────────
        etItemName         = findViewById(R.id.etItemName)
        etItemPrice        = findViewById(R.id.etItemPrice)
        etQuantity         = findViewById(R.id.etQuantity)
        spinnerCategory    = findViewById(R.id.spinnerCategory)
        spinnerPaymentMode = findViewById(R.id.spinnerPaymentMode)
        tvUnitPrice        = findViewById(R.id.tvUnitPrice)
        btnSaveItem        = findViewById(R.id.btnSaveItem)

        // ── Toolbar ───────────────────────────────────────────────
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // ── Payment Mode Spinner ──────────────────────────────────
        spinnerPaymentMode.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            PaymentMode.ALL
        )

        // ── Category Spinner ──────────────────────────────────────
        setupCategorySpinner()

        // FIX 1: Unit price listeners outside edit mode — works for Add & Edit
        etItemPrice.addTextChangedListener { updateUnitPrice() }
        etQuantity.addTextChangedListener  { updateUnitPrice() }

        // ── Edit Mode ─────────────────────────────────────────────
        isEditMode = intent.getBooleanExtra("editMode", false)

        if (isEditMode) {

            existingId      = intent.getStringExtra("id").orEmpty()
            existingAddedAt = intent.getLongExtra("addedAt", System.currentTimeMillis())

            etItemName.setText(intent.getStringExtra("name"))
            etItemPrice.setText(intent.getDoubleExtra("price", 0.0).toString())
            etQuantity.setText(intent.getDoubleExtra("quantity", 1.0).toString())

            // FIX 5: Show unit price immediately after setText
            updateUnitPrice()

            // Restore spinner selections
            val category = intent.getStringExtra("category")
            val payment  = intent.getStringExtra("paymentMode")

            category?.let {
                val index = storage.getCategories().indexOf(it)
                if (index >= 0) spinnerCategory.setSelection(index)
            }

            payment?.let {
                val index = PaymentMode.ALL.indexOf(it)
                if (index >= 0) spinnerPaymentMode.setSelection(index)
            }
        }

        btnSaveItem.setOnClickListener { saveItem() }
    }

    // ── CATEGORY SPINNER ──────────────────────────────────────────

    private fun setupCategorySpinner() {

        val categories  = storage.getCategories()
        val updatedList = categories.toMutableList()
        updatedList.add("+ Add Category")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            updatedList
        )

        spinnerCategory.adapter = adapter

        spinnerCategory.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = parent.getItemAtPosition(position).toString()
                if (selected == "+ Add Category") {
                    showAddCategoryDialog()
                    spinnerCategory.setSelection(0)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        })
    }

    // ── ADD CATEGORY DIALOG ───────────────────────────────────────

    private fun showAddCategoryDialog() {

        val input = EditText(this)

        AlertDialog.Builder(this)
            .setTitle("Add Category")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val newCategory = input.text.toString().trim()
                if (newCategory.isNotEmpty()) {
                    val list = storage.getCategories()
                    if (!list.contains(newCategory)) {
                        list.add(newCategory)
                        storage.saveCategories(list)
                    }
                    setupCategorySpinner()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── UNIT PRICE DISPLAY ────────────────────────────────────────

    private fun updateUnitPrice() {
        val price = etItemPrice.text?.toString()?.toDoubleOrNull() ?: 0.0
        val qty   = etQuantity.text?.toString()?.toDoubleOrNull()  ?: 1.0

        if (price > 0 && qty > 0) {
            val unitPrice = price / qty
            tvUnitPrice.visibility = View.VISIBLE
            tvUnitPrice.text = "Unit Price: %.2f".format(unitPrice)
        } else {
            tvUnitPrice.visibility = View.GONE
        }
    }

    // ── SAVE ITEM ─────────────────────────────────────────────────

    private fun saveItem() {

        val sharedPref = getSharedPreferences("BUDGET_DATA", MODE_PRIVATE)

        val month = sharedPref.getString("last_month", null)
        val year  = sharedPref.getString("last_year",  null)

        if (month == null || year == null) {
            Snackbar.make(btnSaveItem, "⚠ Please set budget first", Snackbar.LENGTH_LONG).show()
            return
        }

        val key = "budget_${month}_${year}"

        // FIX 2: Check key existence BEFORE reading value
        if (!sharedPref.contains(key)) {
            Snackbar.make(btnSaveItem, "⚠ No budget found for selected month/year", Snackbar.LENGTH_LONG).show()
            return
        }

        val budget = sharedPref.getFloat(key, 0f)

        if (budget <= 0f) {
            Snackbar.make(btnSaveItem, "⚠ Please set a valid budget first", Snackbar.LENGTH_LONG).show()
            return
        }

        // ── Input Validation ──────────────────────────────────────
        val name  = etItemName.text?.toString()?.trim().orEmpty()
        val price = etItemPrice.text?.toString()?.toDoubleOrNull()
        val qty   = etQuantity.text?.toString()?.toDoubleOrNull()

        if (name.isBlank()) {
            etItemName.error = "Enter item name"
            return
        }

        if (price == null || price <= 0) {
            etItemPrice.error = "Enter valid price"
            return
        }

        if (qty == null || qty <= 0) {
            etQuantity.error = "Enter valid quantity"
            return
        }

        // FIX 3: Safe category — guard against "+ Add Category" position
        val categories  = storage.getCategories()
        val selectedPos = spinnerCategory.selectedItemPosition
        val category    = if (selectedPos < categories.size) categories[selectedPos] else "Other"

        val paymentMode = PaymentMode.ALL[spinnerPaymentMode.selectedItemPosition]

        // FIX 6: Budget exceed warning (FR-13 from SRS)
        val existingItems = storage.getGroceryList()
        val totalSpent    = existingItems
            .filter { if (isEditMode) it.id != existingId else true }
            .sumOf { it.price * it.quantity }
        val newCost = price * qty

        if (totalSpent + newCost > budget) {
            Snackbar.make(
                btnSaveItem,
                "⚠ Warning: This item exceeds your budget!",
                Snackbar.LENGTH_LONG
            ).show()
            // Still allow saving — user is just warned
        }

        // ── Build Item ────────────────────────────────────────────
        val id      = if (isEditMode) existingId else UUID.randomUUID().toString()
        val addedAt = if (isEditMode) existingAddedAt else System.currentTimeMillis()

        val newItem = GroceryItem(
            id          = id,
            name        = name,
            price       = price,
            quantity    = qty,
            category    = category,
            paymentMode = paymentMode,
            addedAt     = addedAt
        )

        // ── Save to Storage ───────────────────────────────────────
        val list = storage.getGroceryList()

        if (isEditMode) {
            val index = list.indexOfFirst { it.id == id }
            if (index != -1) list[index] = newItem
        } else {
            list.add(newItem)
        }

        storage.saveGroceryList(list)

        setResult(Activity.RESULT_OK)
        Snackbar.make(btnSaveItem, "✅ Item saved successfully", Snackbar.LENGTH_SHORT).show()
        finish()
    }

    // ── NAVIGATION ────────────────────────────────────────────────

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}