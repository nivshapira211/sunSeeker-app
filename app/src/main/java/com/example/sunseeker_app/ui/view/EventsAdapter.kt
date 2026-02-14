package com.example.sunseeker_app.ui.view

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sunseeker_app.R
import com.example.sunseeker_app.data.local.EventEntity
import com.example.sunseeker_app.databinding.ItemEventBinding

class EventsAdapter(
    private val onJoinClick: (EventEntity) -> Unit,
    private val onItemClick: (EventEntity) -> Unit,
    private val currentUserId: String? = null
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
        holder.bind(getItem(position), onJoinClick, onItemClick, currentUserId)
    }

    class EventViewHolder(
        private val binding: ItemEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: EventEntity,
            onJoinClick: (EventEntity) -> Unit,
            onItemClick: (EventEntity) -> Unit,
            currentUserId: String?
        ) {
            binding.textTitle.text = item.title
            binding.textTime.text = item.time

            // Toggle Join/Leave based on attendance
            val isJoined = currentUserId != null && item.attendeeIds.contains(currentUserId)
            binding.buttonJoin.text = binding.root.context.getString(
                if (isJoined) R.string.action_leave else R.string.action_join
            )

            val primaryColor = com.google.android.material.color.MaterialColors.getColor(
                binding.root, androidx.appcompat.R.attr.colorPrimary
            )
            val errorColor = com.google.android.material.color.MaterialColors.getColor(
                binding.root, androidx.appcompat.R.attr.colorError
            )
            binding.buttonJoin.backgroundTintList = ColorStateList.valueOf(
                if (isJoined) errorColor else primaryColor
            )

            binding.buttonJoin.setOnClickListener { onJoinClick(item) }
            binding.root.setOnClickListener { onItemClick(item) }

            // Load event image
            if (item.imageUrl.isNotBlank()) {
                com.bumptech.glide.Glide.with(binding.root.context)
                    .load(item.imageUrl)
                    .centerCrop()
                    .placeholder(com.example.sunseeker_app.R.drawable.event_placeholder)
                    .into(binding.imageEvent)
            } else {
                binding.imageEvent.setImageResource(com.example.sunseeker_app.R.drawable.event_placeholder)
            }
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
