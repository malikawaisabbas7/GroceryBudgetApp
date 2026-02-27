package com.example.grocerybudgetapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.grocerybudgetapp.R
import com.example.grocerybudgetapp.GroceryItem

class GroceryAdapter(
    private val itemList: List<GroceryItem>,
    private val onDeleteClick: (GroceryItem) -> Unit,
    private val onEditClick: (GroceryItem, Int) -> Unit
) : RecyclerView.Adapter<GroceryAdapter.GroceryViewHolder>() {

    class GroceryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: TextView = view.findViewById(R.id.itemName)
        val itemPrice: TextView = view.findViewById(R.id.itemPrice)
        val itemDetails: TextView = view.findViewById(R.id.itemDetails)
        val editBtn: ImageButton = view.findViewById(R.id.editBtn)
        val deleteBtn: ImageButton = view.findViewById(R.id.deleteBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroceryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_grocery, parent, false)
        return GroceryViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroceryViewHolder, position: Int) {
        val item = itemList[position]
        val ctx = holder.itemView.context
        holder.itemName.text = item.name
        holder.itemPrice.text = ctx.getString(R.string.currency_format, "%,d".format(item.price.toInt()))
        holder.itemDetails.text = ctx.getString(R.string.quantity) + ": " + "%,.0f".format(item.quantity) +
            " · Unit: PKR " + "%,.0f".format(item.unitPrice) + " · " + item.paymentMode + " · " + item.category
        holder.deleteBtn.setOnClickListener { onDeleteClick(item) }
        holder.editBtn.setOnClickListener { onEditClick(item, position) }
    }

    override fun getItemCount() = itemList.size
}
