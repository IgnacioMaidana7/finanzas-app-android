package com.maidana.finanzasapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.maidana.finanzasapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class CloudSummary(
    val month: String,
    val income: Long,
    val expense: Long,
    val net: Long,
    val transactionCount: Int
)

interface SummaryRepository {
    suspend fun fetch(month: String): CloudSummary
}

class SummaryUnavailableException(message: String) : Exception(message)

class CloudSummaryRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val baseUrl: String = BuildConfig.SUMMARY_BASE_URL
) : SummaryRepository {
    override suspend fun fetch(month: String): CloudSummary {
        require(Regex("20\\d{2}-(0[1-9]|1[0-2])").matches(month))
        check(baseUrl.matches(Regex("https://[a-z0-9-]+(?:\\.[a-z0-9-]+)*\\.run\\.app"))) {
            "El resumen no está disponible."
        }
        val user = auth.currentUser ?: error("Iniciá sesión para consultar el resumen.")
        for (attempt in 0..1) {
            val token = user.getIdToken(attempt == 1).await().token
                ?: error("No se pudo verificar la sesión.")
            check(auth.currentUser?.uid == user.uid) { "La sesión cambió. Volvé a consultar." }
            val (status, body) = withContext(Dispatchers.IO) {
                val connection = URL("$baseUrl/v1/summary?month=$month").openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "GET"
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 10000
                    connection.readTimeout = 20000
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.setRequestProperty("Accept", "application/json")
                    val code = connection.responseCode
                    val stream = if (code == 200) connection.inputStream else connection.errorStream
                    val bytes = stream?.use {
                        val buffer = ByteArray(65537)
                        var total = 0
                        while (total < buffer.size) {
                            val read = it.read(buffer, total, buffer.size - total)
                            if (read == -1) break
                            total += read
                        }
                        buffer.copyOf(total)
                    } ?: byteArrayOf()
                    check(bytes.size <= 65536) { "Respuesta inválida del servicio." }
                    code to bytes.toString(Charsets.UTF_8)
                } finally {
                    connection.disconnect()
                }
            }
            check(auth.currentUser?.uid == user.uid) { "La sesión cambió. Volvé a consultar." }
            if (status == 401 && attempt == 0) continue
            if (status != 200) {
                val code = runCatching { JSONObject(body).getJSONObject("error").getString("code") }.getOrNull()
                throw SummaryUnavailableException(when {
                    status == 401 -> "La sesión venció. Volvé a iniciar sesión."
                    code == "SUMMARY_TOO_LARGE" -> "Este mes tiene demasiados movimientos para el resumen."
                    status == 422 -> "Hay movimientos que requieren revisión."
                    else -> "No se pudo consultar el resumen. Intentá nuevamente."
                })
            }
            val json = JSONObject(body)
            check(json.getString("month") == month && json.getString("timeZone") == "UTC" &&
                json.getString("unit") == "ARS_INTEGER") { "Respuesta inválida del servicio." }
            return CloudSummary(month, json.getString("income").toLong(), json.getString("expense").toLong(),
                json.getString("net").toLong(), json.getInt("transactionCount"))
        }
        error("No se pudo verificar la sesión.")
    }
}
