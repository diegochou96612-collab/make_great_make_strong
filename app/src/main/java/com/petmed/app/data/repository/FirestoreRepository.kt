package com.petmed.app.data.repository

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.petmed.app.data.model.MedicationRecord
import com.petmed.app.data.model.Pet
import kotlinx.coroutines.tasks.await

object FirestoreRepository {

    private val db = Firebase.firestore
    private val collection = db.collection("medication_logs")

    suspend fun uploadMedicationLog(record: MedicationRecord, pet: Pet?) {
        val data = hashMapOf(
            "medicationName" to record.medicationName,
            "dateTime" to record.dateTime,
            "species" to (pet?.species ?: "未知"),
            "uploadedAt" to com.google.firebase.Timestamp.now()
        )
        collection.add(data).await()
    }
}
