package com.maidana.finanzasapp.data.local

import android.content.Context
import com.maidana.finanzasapp.data.remote.IndicatorJson
import com.maidana.finanzasapp.data.repository.IndicatorCache
import com.maidana.finanzasapp.domain.DollarQuote
import com.maidana.finanzasapp.domain.IndicatorSnapshot
import com.maidana.finanzasapp.domain.MonthlyInflation

/** Public economic series only. Never stores a UID, receipt, balance or draft. */
class PreferencesIndicatorCache(context: Context) : IndicatorCache {
    private val preferences = context.applicationContext.getSharedPreferences("public_indicators_v1", Context.MODE_PRIVATE)
    override fun readQuotes() = read("quotes", IndicatorJson::quotes)
    override fun readInflation() = read("inflation", IndicatorJson::inflation)
    override fun writeQuotes(snapshot: IndicatorSnapshot<DollarQuote>) = write("quotes", snapshot.fetchedAt, IndicatorJson.encodeQuotes(snapshot.values))
    override fun writeInflation(snapshot: IndicatorSnapshot<MonthlyInflation>) = write("inflation", snapshot.fetchedAt, IndicatorJson.encodeInflation(snapshot.values))

    private fun <T> read(key: String, decode: (String) -> List<T>): IndicatorSnapshot<T>? = try {
        preferences.getString(key, null)?.let { json ->
            val date = preferences.getLong("${key}_fetched", 0)
            if (date > 0) IndicatorSnapshot(decode(json), date) else null
        }
    } catch (_: Exception) { null }

    private fun write(key: String, date: Long, json: String) {
        preferences.edit().putString(key, json).putLong("${key}_fetched", date).apply()
    }
}
