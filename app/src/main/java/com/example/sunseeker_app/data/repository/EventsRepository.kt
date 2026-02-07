package com.example.sunseeker_app.data.repository

import androidx.lifecycle.LiveData
import com.example.sunseeker_app.data.local.EventDao
import com.example.sunseeker_app.data.local.EventEntity
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
    fun getEvents(): LiveData<List<EventEntity>> = eventDao.getAllEvents()
    fun getEventById(id: String): LiveData<EventEntity?> = eventDao.getEventById(id)

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

            eventDao.deleteAll()
            if (events.isNotEmpty()) {
                eventDao.insertAll(events)
            }
        }
    }

    suspend fun joinEvent(eventId: String, userId: String) {
        withContext(Dispatchers.IO) {
            firestore.collection(EVENTS_COLLECTION)
                .document(eventId)
                .update("attendees", FieldValue.arrayUnion(userId))
                .await()
            refreshEvents()
        }
    }

    suspend fun createEvent(event: EventEntity) {
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
