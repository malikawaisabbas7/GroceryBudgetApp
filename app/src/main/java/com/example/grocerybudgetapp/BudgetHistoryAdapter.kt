package com.example.grocerybudgetapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BudgetHistoryAdapter(
    private val list: List<BudgetHistory>
) : RecyclerView.Adapter<BudgetHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val month: TextView = view.findViewById(R.id.tvMonth)
        val year: TextView = view.findViewById(R.id.tvYear)
        val amount: TextView = view.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget_history, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.month.text = item.month
        holder.year.text = item.year
        holder.amount.text = "PKR ${item.amount}"
    }
}