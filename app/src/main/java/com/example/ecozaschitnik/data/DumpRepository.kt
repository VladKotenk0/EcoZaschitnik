package com.example.ecozaschitnik.data

import com.example.ecozaschitnik.DumpPoint
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class DumpRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
) {

    suspend fun getAll(): List<DumpPoint> {
        val snapshot = db.collection(COLLECTION).get().await()
        return snapshot.documents.mapNotNull { doc ->
            val lat = doc.getDouble("latitude") ?: return@mapNotNull null
            val lon = doc.getDouble("longitude") ?: return@mapNotNull null
            val photoUrl = doc.readPhotoUrl()
            DumpPoint(
                id = doc.id,
                lat = lat,
                lon = lon,
                title = doc.getString("name") ?: "Свалка",
                description = doc.getString("userDescription") ?: "",
                reportText = doc.getString("reportText") ?: "",
                coordinatesText = doc.getString("coordinatesText") ?: "",
                photoUrl = photoUrl,
                hasPhoto = doc.getBoolean("hasPhoto") == true || !photoUrl.isNullOrBlank(),
                timestamp = doc.getLong("timestamp"),
            )
        }
    }

    suspend fun saveReport(
        name: String,
        userDescription: String,
        aiReport: String,
        lat: Double,
        lon: Double,
        coordinatesText: String,
        hasPhoto: Boolean = false,
    ) {
        val data = hashMapOf(
            "name" to name,
            "userDescription" to userDescription,
            "reportText" to aiReport,
            "latitude" to lat,
            "longitude" to lon,
            "coordinatesText" to coordinatesText,
            "hasPhoto" to hasPhoto,
            "timestamp" to System.currentTimeMillis(),
        )
        db.collection(COLLECTION).add(data).await()
    }

    suspend fun addPoint(lat: Double, lon: Double, title: String, description: String) {
        val data = hashMapOf(
            "name" to title,
            "userDescription" to description,
            "reportText" to "",
            "latitude" to lat,
            "longitude" to lon,
            "coordinatesText" to "",
            "hasPhoto" to false,
            "timestamp" to System.currentTimeMillis(),
        )
        db.collection(COLLECTION).add(data).await()
    }

    suspend fun delete(id: String) {
        val photoUrl = db.collection(COLLECTION).document(id).get().await().readPhotoUrl()
        db.collection(COLLECTION).document(id).delete().await()
        if (!photoUrl.isNullOrBlank()) {
            try {
                storage.getReferenceFromUrl(photoUrl).delete().await()
            } catch (_: Exception) {
                // Фото могло быть уже удалено или ссылка устарела
            }
        }
    }

    companion object {
        private const val COLLECTION = "dumps"
    }

    private fun DocumentSnapshot.readPhotoUrl(): String? {
        val candidates = listOf("photoUrl", "photoURL", "imageUrl", "photo")
        for (field in candidates) {
            val value = getString(field)?.trim()
            if (!value.isNullOrBlank()) return value
        }
        return null
    }
}
