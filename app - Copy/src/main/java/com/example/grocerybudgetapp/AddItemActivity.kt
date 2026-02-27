package com.example.grocerybudgetapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.grocerybudgetapp.storage.StorageService
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.appbar.MaterialToolbar
import java.util.UUID
import androidx.core.widget.addTextChangedListener

class AddItemActivity : AppCompatActivity() {

    private lateinit var etItemName: TextInputEditText
    private lateinit var etItemPrice: TextInputEditText
    private lateinit var etQuantity: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerPaymentMode: Spinner
    private lateinit var tvUnitPrice: TextView
    private lateinit var tvUnitPriceTip: TextView
    private lateinit var btnSaveItem: MaterialButton
    private lateinit var storage: StorageService

    private var isEditMode = false
    private var editIndex = -1
    private var existingId = ""
    private var existingAddedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)

        // Initialize views
        storage = StorageService(this)
        etItemName = findViewById(R.id.etItemName)
        etItemPrice = findViewById(R.id.etItemPrice)
        etQuantity = findViewById(R.id.etQuantity)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerPaymentMode = findViewById(R.id.spinnerPaymentMode)
        tvUnitPrice = findViewById(R.id.tvUnitPrice)
        tvUnitPriceTip = findViewById(R.id.tvUnitPriceTip)
        btnSaveItem = findViewById(R.id.btnSaveItem)

        // Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Spinners
        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ItemCategory.ALL)
        spinnerPaymentMode.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, PaymentMode.ALL)

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {}
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinnerPaymentMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {}
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Edit mode
        isEditMode = intent.getBooleanExtra("edit", false)
        if (!isEditMode) etQuantity.setText("1")

        if (isEditMode) {
            editIndex = intent.getIntExtra("editIndex", -1)
            existingId = intent.getStringExtra("itemId").orEmpty()
            existingAddedAt = intent.getLongExtra("itemAddedAt", System.currentTimeMillis())
            supportActionBar?.title = getString(R.string.edit_item)

            etItemName.setText(intent.getStringExtra("itemName"))
            etItemPrice.setText(intent.getDoubleExtra("itemPrice", 0.0).toString())
            etQuantity.setText(intent.getDoubleExtra("itemQuantity", 1.0).toString())

            val mode = intent.getStringExtra("paymentMode") ?: PaymentMode.CASH
            spinnerPaymentMode.setSelection(PaymentMode.ALL.indexOf(mode).coerceAtLeast(0))

            val cat = intent.getStringExtra("itemCategory") ?: ItemCategory.FOOD
            spinnerCategory.setSelection(ItemCategory.ALL.indexOf(cat).coerceAtLeast(0))
        }

        // Dynamic unit price updates
        etItemPrice.addTextChangedListener { updateUnitPricePreview() }
        etQuantity.addTextChangedListener { updateUnitPricePreview() }
        etItemName.addTextChangedListener { updateUnitPricePreview() }

        // Save button
        btnSaveItem.setOnClickListener { saveItem() }
    }

    // Class-level function
    private fun updateUnitPricePreview() {
        val price = etItemPrice.text?.toString()?.toDoubleOrNull() ?: 0.0
        val qty = etQuantity.text?.toString()?.toDoubleOrNull() ?: 1.0

        if (qty > 0 && price >= 0) {
            tvUnitPrice.visibility = View.VISIBLE
            tvUnitPrice.text = getString(R.string.unit_price, "%,.2f".format(price / qty))
            showUnitPriceTip(etItemName.text?.toString()?.trim().orEmpty(), price / qty)
        } else {
            tvUnitPrice.visibility = View.GONE
            tvUnitPriceTip.visibility = View.GONE
        }
    }

    // Class-level function
    private fun showUnitPriceTip(name: String, currentUnitPrice: Double) {
        if (name.isBlank()) {
            tvUnitPriceTip.visibility = View.GONE
            return
        }

        val existing = storage.getGroceryList()
            .filter { it.name.equals(name, ignoreCase = true) && it.id != existingId }
        val cheaper = existing.filter { it.unitPrice < currentUnitPrice }
            .minByOrNull { it.unitPrice }

        if (cheaper != null) {
            tvUnitPriceTip.visibility = View.VISIBLE
            tvUnitPriceTip.text = getString(
                R.string.unit_price_tip,
                "%,.0f".format(cheaper.quantity) + " " + name,
                "%,.0f".format(cheaper.unitPrice)
            )
        } else {
            tvUnitPriceTip.visibility = View.GONE
        }
    }

    private fun saveItem() {
        val name = etItemName.text?.toString()?.trim().orEmpty()
        val priceStr = etItemPrice.text?.toString()?.trim().orEmpty()
        val qtyStr = etQuantity.text?.toString()?.trim().orEmpty()

        if (name.isEmpty()) {
            etItemName.error = getString(R.string.error_name_required)
            etItemName.requestFocus()
            return
        }
        val price = priceStr.toDoubleOrNull()
        if (price == null || price < 0) {
            etItemPrice.error = getString(R.string.error_price_invalid)
            etItemPrice.requestFocus()
            return
        }
        val quantity = qtyStr.toDoubleOrNull()
        if (quantity == null || quantity <= 0) {
            etQuantity.error = getString(R.string.error_quantity_invalid)
            etQuantity.requestFocus()
            return
        }

        val category = ItemCategory.ALL[spinnerCategory.selectedItemPosition]
        val paymentMode = PaymentMode.ALL[spinnerPaymentMode.selectedItemPosition]
        val id = if (isEditMode) existingId else UUID.randomUUID().toString()
        val addedAt = if (isEditMode) existingAddedAt else System.currentTimeMillis()

        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra("itemName", name)
            putExtra("itemPrice", price)
            putExtra("itemQuantity", quantity)
            putExtra("paymentMode", paymentMode)
            putExtra("itemCategory", category)
            putExtra("itemId", id)
            putExtra("edit", isEditMode)
            putExtra("editIndex", editIndex)
            putExtra("itemAddedAt", addedAt)
        })
        finish()
    }
}