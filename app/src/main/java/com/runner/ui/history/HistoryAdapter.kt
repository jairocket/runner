package com.runner.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.runner.databinding.ItemHistoryRunBinding

class HistoryAdapter(
    private val items: List<RunActivity>,
    private val onItemClick: (RunActivity) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemHistoryRunBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RunActivity) {
            binding.textItemDate.text = item.date
            binding.textItemDistance.text = item.distanceKm
            binding.textItemDuration.text = item.duration
            binding.textItemPace.text = "${item.paceMinKm} min/km"
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryRunBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
