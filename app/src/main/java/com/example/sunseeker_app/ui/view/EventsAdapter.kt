package com.example.sunseeker_app.ui.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sunseeker_app.data.local.EventEntity
import com.example.sunseeker_app.databinding.ItemEventBinding

class EventsAdapter(
    private val onJoinClick: (EventEntity) -> Unit,
    private val joinLabel: String = "Join"
) : ListAdapter<EventEntity, EventsAdapter.EventViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position), onJoinClick, joinLabel)
    }

    class EventViewHolder(
        private val binding: ItemEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: EventEntity, onJoinClick: (EventEntity) -> Unit, joinLabel: String) {
            binding.textTitle.text = item.title
            binding.textTime.text = item.time
            binding.buttonJoin.text = joinLabel
            binding.buttonJoin.setOnClickListener { onJoinClick(item) }
        }
    }

    private companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<EventEntity>() {
            override fun areItemsTheSame(oldItem: EventEntity, newItem: EventEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: EventEntity, newItem: EventEntity): Boolean {
                return oldItem == newItem
            }
        }
    }
}
