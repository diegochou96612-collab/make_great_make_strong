package com.petmed.app.data.repository

import com.petmed.app.data.model.Hospital
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class HospitalRepository {

    private val client = OkHttpClient()

    private val dataUrl =
        "https://data.taipei/api/dataset/01bcb5ee-7c18-41fa-86d4-4e75daee1f94/resource/40d79051-1839-4d00-855f-be88f1e06caf/download"

    suspend fun fetchHospitals(): List<Hospital> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(dataUrl).build()
        val body = client.newCall(request).execute().use { response ->
            response.body?.string() ?: ""
        }
        parseCSV(body)
    }

    private fun parseCSV(csv: String): List<Hospital> {
        return csv.lines()
            .drop(1)
            .filter { it.isNotBlank() }
            .mapIndexedNotNull { index, line ->
                val cols = parseLine(line)
                if (cols.size < 4) return@mapIndexedNotNull null
                Hospital(
                    id = index + 1,
                    name = cols[1].trim(),
                    phone = cols[3].trim(),
                    address = cols[2].trim(),
                    district = extractDistrict(cols[2].trim())
                )
            }
    }

    private fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }

    private fun extractDistrict(address: String): String {
        val idx = address.indexOf('區')
        return if (idx > 0) address.substring(0, idx + 1) else ""
    }
}
