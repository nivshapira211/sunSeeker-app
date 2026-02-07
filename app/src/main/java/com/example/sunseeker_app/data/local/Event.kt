package com.example.sunseeker_app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey val eventId: String,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val eventTimestamp: Long,
    val hostId: String,
    val isSunrise: Boolean
)