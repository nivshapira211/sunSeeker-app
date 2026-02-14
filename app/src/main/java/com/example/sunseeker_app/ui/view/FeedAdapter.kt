package com.example.sunseeker_app.ui.view

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sunseeker_app.R
import com.example.sunseeker_app.data.model.Event
import com.example.sunseeker_app.databinding.ItemEventBinding
import com.example.sunseeker_app.databinding.ItemSectionHeaderBinding
import com.google.android.material.color.MaterialColors

/**
 * Multi-view-type adapter for the feed: regular event cards + expandable section headers.
 */
class FeedAdapter(
    private val onJoinClick: (Event) -> Unit,
    private val onItemClick: (Event) -> Unit,
    private val onSectionToggle: (String) -> Unit,
    private val currentUserId: String? = null
) : ListAdapter<FeedItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private const val TYPE_EVENT = 0
        private const val TYPE_HEADER = 1

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FeedItem>() {
            override fun areItemsTheSame(old: FeedItem, new: FeedItem): Boolean {
                return when {
                    old is FeedItem.EventItem && new is FeedItem.EventItem -> old.id == new.id
                    old is FeedItem.SectionHeader && new is FeedItem.SectionHeader -> old.id == new.id
                    else -> false
                }
            }

            override fun areContentsTheSame(old: FeedItem, new: FeedItem): Boolean = old == new
        }
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is FeedItem.EventItem -> TYPE_EVENT
        is FeedItem.SectionHeader -> TYPE_HEADER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> SectionHeaderViewHolder(
                ItemSectionHeaderBinding.inflate(inflater, parent, false)
            )
            else -> EventViewHolder(
                ItemEventBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is FeedItem.EventItem -> (holder as EventViewHolder).bind(item.event, onJoinClick, onItemClick, currentUserId)
            is FeedItem.SectionHeader -> (holder as SectionHeaderViewHolder).bind(item, onSectionToggle)
        }
    }

    // ── Event card ViewHolder ───────────────────────────────────────────

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

            val isJoined = currentUserId != null && item.attendeeIds.contains(currentUserId)
            binding.buttonJoin.text = binding.root.context.getString(
                if (isJoined) R.string.action_leave else R.string.action_join
            )

            val primaryColor = MaterialColors.getColor(
                binding.root, androidx.appcompat.R.attr.colorPrimary
            )
            val errorColor = MaterialColors.getColor(
                binding.root, androidx.appcompat.R.attr.colorError
            )
            binding.buttonJoin.backgroundTintList = ColorStateList.valueOf(
                if (isJoined) errorColor else primaryColor
            )

            binding.buttonJoin.setOnClickListener { onJoinClick(item) }
            binding.root.setOnClickListener { onItemClick(item) }

            if (item.imageUrl.isNotBlank()) {
                Glide.with(binding.root.context)
                    .load(item.imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.event_placeholder)
                    .into(binding.imageEvent)
            } else {
                binding.imageEvent.setImageResource(R.drawable.event_placeholder)
            }
        }
    }

    // ── Section header ViewHolder ────────────────────────────────────────

    class SectionHeaderViewHolder(
        private val binding: ItemSectionHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: FeedItem.SectionHeader, onToggle: (String) -> Unit) {
            binding.textSectionTitle.text = header.title
            binding.textSectionCount.text = "(${header.count})"

            // Rotate chevron: 0° = collapsed (pointing right), 180° = expanded (pointing up)
            binding.iconExpand.animate()
                .rotation(if (header.isExpanded) 180f else 0f)
                .setDuration(200)
                .start()

            binding.root.setOnClickListener { onToggle(header.title) }
        }
    }
}
