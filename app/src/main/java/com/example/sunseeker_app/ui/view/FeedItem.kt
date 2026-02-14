package com.example.sunseeker_app.ui.view

import com.example.sunseeker_app.data.model.Event

/**
 * Sealed class representing items in the feed RecyclerView.
 * Enables a multi-view-type adapter with section headers.
 */
sealed class FeedItem {
    /** A regular event card. */
    data class EventItem(val event: Event) : FeedItem() {
        val id: String get() = event.id
    }

    /** A collapsible section header (e.g. "Past Events"). */
    data class SectionHeader(
        val title: String,
        val count: Int,
        val isExpanded: Boolean
    ) : FeedItem() {
        val id: String get() = "header_$title"
    }
}
