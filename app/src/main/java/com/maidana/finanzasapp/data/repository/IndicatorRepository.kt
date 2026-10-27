package com.maidana.finanzasapp.data.repository

import com.maidana.finanzasapp.domain.DollarQuote
import com.maidana.finanzasapp.domain.IndicatorSnapshot
import com.maidana.finanzasapp.domain.MonthlyInflation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface IndicatorRepository {
    suspend fun quotes(): IndicatorSnapshot<DollarQuote>
    suspend fun inflation(): IndicatorSnapshot<MonthlyInflation>
}

interface IndicatorRemoteSource {
    suspend fun quotes(): List<DollarQuote>
    suspend fun inflation(): List<MonthlyInflation>
}

interface IndicatorCache {
    fun readQuotes(): IndicatorSnapshot<DollarQuote>?
    fun readInflation(): IndicatorSnapshot<MonthlyInflation>?
    fun writeQuotes(snapshot: IndicatorSnapshot<DollarQuote>)
    fun writeInflation(snapshot: IndicatorSnapshot<MonthlyInflation>)
}

class CachedIndicatorRepository(
    private val remote: IndicatorRemoteSource,
    private val cache: IndicatorCache,
    private val now: () -> Long = System::currentTimeMillis
) : IndicatorRepository {
    private val quoteLock = Mutex()
    private val inflationLock = Mutex()
    private var quoteAttempt: Long? = null
    private var inflationAttempt: Long? = null

    override suspend fun quotes(): IndicatorSnapshot<DollarQuote> = quoteLock.withLock {
        val old = cache.readQuotes()
        load(old, QUOTE_TTL, quoteAttempt, { quoteAttempt = it }, remote::quotes, cache::writeQuotes)
    }

    override suspend fun inflation(): IndicatorSnapshot<MonthlyInflation> = inflationLock.withLock {
        val old = cache.readInflation()
        load(old, INFLATION_TTL, inflationAttempt, { inflationAttempt = it }, remote::inflation, cache::writeInflation)
    }

    private suspend fun <T> load(
        old: IndicatorSnapshot<T>?, ttl: Long, lastAttempt: Long?, markAttempt: (Long) -> Unit,
        fetch: suspend () -> List<T>, persist: (IndicatorSnapshot<T>) -> Unit
    ): IndicatorSnapshot<T> {
        val time = now()
        if (old != null && time - old.fetchedAt in 0 until ttl) return old
        // A retry button cannot turn a disconnected screen into a request loop.
        if (lastAttempt != null && time - lastAttempt in 0 until RETRY_DELAY) {
            return old?.copy(stale = true, notice = OFFLINE_NOTICE)
                ?: throw IllegalStateException("Esperá un minuto antes de reintentar la consulta.")
        }
        markAttempt(time)
        return try {
            val values = fetch()
            require(values.isNotEmpty()) { "El proveedor no devolvió datos disponibles." }
            IndicatorSnapshot(values, time).also(persist)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            old?.copy(stale = true, notice = OFFLINE_NOTICE)
                ?: throw IllegalStateException("No se pudieron obtener los indicadores. Verificá tu conexión y reintentá.", e)
        }
    }

    companion object {
        const val QUOTE_TTL = 3 * 60 * 60 * 1000L
        const val INFLATION_TTL = 24 * 60 * 60 * 1000L
        const val RETRY_DELAY = 60_000L
        private const val OFFLINE_NOTICE = "No se pudo actualizar. Se muestra la última referencia guardada."
    }
}
