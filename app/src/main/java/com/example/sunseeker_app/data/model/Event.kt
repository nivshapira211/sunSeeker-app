package com.example.sunseeker_app.data.model

data class Event(
    val id: String,
    val title: String,
    val location: String,
    val time: String,
    val description: String,
    val imageUrl: String,
    val participantsCount: Int,
    val attendeeIds: List<String>,
    val attendeeNames: Map<String, String> = emptyMap(),
    val creatorId: String
)

