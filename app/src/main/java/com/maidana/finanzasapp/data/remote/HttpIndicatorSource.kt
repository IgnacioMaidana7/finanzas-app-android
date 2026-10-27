package com.maidana.finanzasapp.data.remote

import com.maidana.finanzasapp.data.repository.IndicatorRemoteSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class HttpIndicatorSource : IndicatorRemoteSource {
    override suspend fun quotes() = withContext(Dispatchers.IO) { IndicatorJson.quotes(get(QUOTES_URL)) }
    override suspend fun inflation() = withContext(Dispatchers.IO) { IndicatorJson.inflation(get(INFLATION_URL)) }

    private suspend fun get(address: String): String = withContext(Dispatchers.IO) {
        val connection = URL(address).openConnection() as HttpsURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "FinanzasApp-Android/1.0")
            check(connection.responseCode == 200) { "Respuesta HTTP ${connection.responseCode}." }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val content = StringBuilder()
                val buffer = CharArray(4096)
                while (true) {
                    ensureActive()
                    val count = reader.read(buffer)
                    if (count < 0) break
                    content.append(buffer, 0, count)
                    check(content.length <= 2_000_000) { "Respuesta demasiado grande." }
                }
                content.toString()
            }
        } finally { connection.disconnect() }
    }

    companion object {
        const val QUOTES_URL = "https://dolarapi.com/v1/dolares"
        const val INFLATION_URL = "https://api.argentinadatos.com/v1/finanzas/indices/inflacion"
    }
}
