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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val eventDao: EventDao
) {
    /**
     * Returns all events as domain models.
     * Mapping from EventEntity → Event happens here so the UI layer never sees Room entities.
     */
    fun getEvents(): LiveData<List<Event>> = eventDao.getAllEvents().map { entities ->
        entities.map { it.toDomain() }
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

                EventEntity(
                    id = id,
                    title = title,
                    location = location,
                    time = time,
                    description = description,
                    imageUrl = imageUrl,
                    participantsCount = participantsCount,
                    attendeeIds = attendeeIds,
                    creatorId = creatorId
                )
            }

            eventDao.replaceAll(events)
        }
    }

    suspend fun joinEvent(eventId: String, userId: String) {
        withContext(Dispatchers.IO) {
            val eventRef = firestore.collection(EVENTS_COLLECTION).document(eventId)
            eventRef.update("attendees", FieldValue.arrayUnion(userId)).await()
            eventRef.update("participantsCount", FieldValue.increment(1)).await()
            refreshEvents()
        }
    }

    suspend fun leaveEvent(eventId: String, userId: String) {
        withContext(Dispatchers.IO) {
            val eventRef = firestore.collection(EVENTS_COLLECTION).document(eventId)
            eventRef.update("attendees", FieldValue.arrayRemove(userId)).await()
            eventRef.update("participantsCount", FieldValue.increment(-1)).await()
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
                "creatorId" to event.creatorId
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
    creatorId = creatorId
)
