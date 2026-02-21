package com.example.sunseeker_app.ui.view

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sunseeker_app.R
import com.example.sunseeker_app.data.model.Event
import com.example.sunseeker_app.databinding.ItemEventBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventsAdapter(
    private val onJoinClick: (Event) -> Unit,
    private val onItemClick: (Event) -> Unit,
    private val currentUserId: String? = null
) : ListAdapter<Event, EventsAdapter.EventViewHolder>(DiffCallback) {

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
            item: Event,
            onJoinClick: (Event) -> Unit,
            onItemClick: (Event) -> Unit,
            currentUserId: String?
        ) {
            binding.textTitle.text = item.title
            binding.textTime.text = item.time

            val timeFormat = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())
            val eventTime = try { timeFormat.parse(item.time) } catch (_: Exception) { null }
            val isPast = eventTime != null && eventTime.before(Date())

            if (isPast) {
                binding.buttonJoin.visibility = View.GONE
            } else {
                binding.buttonJoin.visibility = View.VISIBLE

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
            }

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
        val DiffCallback = object : DiffUtil.ItemCallback<Event>() {
            override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
                return oldItem == newItem
            }
        }
    }
}
