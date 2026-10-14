package com.example.lab08

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CloudRepository {

    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("tasks")

    // Sube todas las tareas locales y borra las que había en la nube
    suspend fun upload(tasks: List<Task>) {
        val existing = collection.get().await()
        val batch = db.batch()
        existing.documents.forEach { batch.delete(it.reference) }
        tasks.forEach { task ->
            batch.set(
                collection.document(task.id.toString()),
                mapOf(
                    "id" to task.id,
                    "description" to task.description,
                    "isCompleted" to task.isCompleted,
                    "priority" to task.priority.name,
                    "category" to task.category,
                    "createdAt" to task.createdAt,
                    "recurrence" to task.recurrence.name
                )
            )
        }
        batch.commit().await()
    }

    // Descarga las tareas de la nube
    suspend fun download(): List<Task> {
        val snapshot = collection.get().await()
        return snapshot.documents.mapNotNull { doc ->
            val description = doc.getString("description") ?: return@mapNotNull null
            Task(
                id = (doc.getLong("id") ?: 0L).toInt(),
                description = description,
                isCompleted = doc.getBoolean("isCompleted") ?: false,
                priority = runCatching { Priority.valueOf(doc.getString("priority") ?: "MEDIA") }
                    .getOrDefault(Priority.MEDIA),
                category = doc.getString("category") ?: "General",
                createdAt = doc.getLong("createdAt") ?: 0L,
                recurrence = runCatching { Recurrence.valueOf(doc.getString("recurrence") ?: "NINGUNA") }
                    .getOrDefault(Recurrence.NINGUNA)
            )
        }
    }
}