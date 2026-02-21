package com.example.sunseeker_app.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.sunseeker_app.data.local.EventDao
import com.example.sunseeker_app.data.local.EventEntity
import com.example.sunseeker_app.data.model.Event
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val eventDao: EventDao
) {
    private val timeFormat = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())

    /**
     * Returns all events as domain models, sorted by event time (earliest first).
     * Mapping from EventEntity → Event happens here so the UI layer never sees Room entities.
     */
    fun getEvents(): LiveData<List<Event>> = eventDao.getAllEvents().map { entities ->
        entities.map { it.toDomain() }.sortedBy { event ->
            try { timeFormat.parse(event.time)?.time } catch (_: Exception) { Long.MAX_VALUE }
        }
    }

    fun getEventById(id: String): LiveData<Event?> = eventDao.getEventById(id).map { entity ->
        entity?.toDomain()
    }

    suspend fun refreshEvents() {
        withContext(Dispatchers.IO) {
            val snapshot = firestore.collection(EVENTS_COLLECTION).get().await()
            val events = snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val title = doc.getString("title") ?: return@mapNotNull null
                val location = doc.getString("location") ?: return@mapNotNull null
                val time = doc.getString("time") ?: return@mapNotNull null
                val description = doc.getString("description") ?: ""
                val imageUrl = doc.getString("imageUrl") ?: ""
                val participantsCount = doc.getLong("participantsCount")?.toInt() ?: 0
                val creatorId = doc.getString("creatorId") ?: ""
                val attendeeIds = (doc.get("attendees") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?: emptyList()
<<<<<<< HEAD
                val attendeeNames = (doc.get("attendeeNames") as? Map<*, *>)
                    ?.mapNotNull { (k, v) -> if (k is String && v is String) k to v else null }
                    ?.toMap()
                    ?: emptyMap()
=======
                val sunType = doc.getString("sunType") ?: ""
>>>>>>> c60bdf0 (add event type)

                EventEntity(
                    id = id,
                    title = title,
                    location = location,
                    time = time,
                    description = description,
                    imageUrl = imageUrl,
                    participantsCount = participantsCount,
                    attendeeIds = attendeeIds,
<<<<<<< HEAD
                    attendeeNames = attendeeNames,
                    creatorId = creatorId
=======
                    creatorId = creatorId,
                    sunType = sunType
>>>>>>> c60bdf0 (add event type)
                )
            }

            eventDao.replaceAll(events)
        }
    }

    suspend fun joinEvent(eventId: String, userId: String, displayName: String?) {
        withContext(Dispatchers.IO) {
            val eventRef = firestore.collection(EVENTS_COLLECTION).document(eventId)
            eventRef.update("attendees", FieldValue.arrayUnion(userId)).await()
            eventRef.update("participantsCount", FieldValue.increment(1)).await()
            val name = displayName ?: "User"
            eventRef.update("attendeeNames.$userId", name).await()
            refreshEvents()
        }
    }

    suspend fun leaveEvent(eventId: String, userId: String) {
        withContext(Dispatchers.IO) {
            val eventRef = firestore.collection(EVENTS_COLLECTION).document(eventId)
            eventRef.update("attendees", FieldValue.arrayRemove(userId)).await()
            eventRef.update("participantsCount", FieldValue.increment(-1)).await()
            eventRef.update("attendeeNames.$userId", FieldValue.delete()).await()
            refreshEvents()
        }
    }

    suspend fun createEvent(event: Event) {
        withContext(Dispatchers.IO) {
            val data = mapOf(
                "id" to event.id,
                "title" to event.title,
                "location" to event.location,
                "time" to event.time,
                "description" to event.description,
                "imageUrl" to event.imageUrl,
                "participantsCount" to event.participantsCount,
                "attendees" to event.attendeeIds,
                "creatorId" to event.creatorId,
                "sunType" to event.sunType
            )
            firestore.collection(EVENTS_COLLECTION)
                .document(event.id)
                .set(data)
                .await()
            refreshEvents()
        }
    }

    suspend fun updateEvent(eventId: String, updates: Map<String, Any?>) {
        withContext(Dispatchers.IO) {
            firestore.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update(updates)
                .await()
            refreshEvents()
        }
    }

    suspend fun deleteEvent(eventId: String) {
        withContext(Dispatchers.IO) {
            firestore.collection(EVENTS_COLLECTION)
                .document(eventId)
                .delete()
                .await()
            refreshEvents()
        }
    }

    companion object {
        private const val EVENTS_COLLECTION = "events"
    }
}

/** Maps a Room entity to the domain model. */
private fun EventEntity.toDomain() = Event(
    id = id,
    title = title,
    location = location,
    time = time,
    description = description,
    imageUrl = imageUrl,
    participantsCount = participantsCount,
    attendeeIds = attendeeIds,
<<<<<<< HEAD
    attendeeNames = attendeeNames,
    creatorId = creatorId
=======
    creatorId = creatorId,
    sunType = sunType
>>>>>>> c60bdf0 (add event type)
)
