package com.maidana.finanzasapp

import com.maidana.finanzasapp.data.repository.*
import com.maidana.finanzasapp.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth

class IndicatorRepositoryTest {
    private class Cache : IndicatorCache {
        var quotes: IndicatorSnapshot<DollarQuote>? = null
        var inflation: IndicatorSnapshot<MonthlyInflation>? = null
        override fun readQuotes() = quotes
        override fun readInflation() = inflation
        override fun writeQuotes(snapshot: IndicatorSnapshot<DollarQuote>) { quotes = snapshot }
        override fun writeInflation(snapshot: IndicatorSnapshot<MonthlyInflation>) { inflation = snapshot }
    }
    private class Remote : IndicatorRemoteSource {
        var calls = 0; var fail = false; var cancel = false
        override suspend fun quotes(): List<DollarQuote> {
            calls++
            if (cancel) throw CancellationException()
            check(!fail)
            return listOf(DollarQuote("oficial", "Oficial", null, BigDecimal("1200"), Instant.EPOCH))
        }
        override suspend fun inflation() = listOf(MonthlyInflation(YearMonth.of(2026, 1), BigDecimal("2")))
    }
    @Test fun repeatedAndConcurrentReadsUseOneRequestWithinThreeHours() = runTest {
        val remote = Remote(); val cache = Cache(); var now = 1000L
        val repository = CachedIndicatorRepository(remote, cache) { now }
        val a = async { repository.quotes() }; val b = async { repository.quotes() }
        a.await(); b.await(); now += 60_000; repository.quotes()
        assertEquals(1, remote.calls)
        now += CachedIndicatorRepository.QUOTE_TTL
        repository.quotes(); assertEquals(2, remote.calls)
    }
    @Test fun expiredDataIsPreservedAndMarkedStaleWhenNetworkFails() = runTest {
        val remote = Remote(); val cache = Cache(); var now = 1000L
        val repository = CachedIndicatorRepository(remote, cache) { now }
        val original = repository.quotes(); now += CachedIndicatorRepository.QUOTE_TTL
        remote.fail = true
        val fallback = repository.quotes()
        assertTrue(fallback.stale); assertNotNull(fallback.notice)
        assertEquals(original.fetchedAt, fallback.fetchedAt)
        repository.quotes(); assertEquals(2, remote.calls)
    }
    @Test fun noCacheFailsAndRapidRetryDoesNotHitNetwork() = runTest {
        val remote = Remote().apply { fail = true }
        val repository = CachedIndicatorRepository(remote, Cache()) { 1000L }
        assertTrue(runCatching { repository.quotes() }.isFailure)
        assertTrue(runCatching { repository.quotes() }.isFailure)
        assertEquals(1, remote.calls)
    }
    @Test fun cancellationIsNeverConvertedIntoStaleSuccess() = runTest {
        val remote = Remote().apply { cancel = true }
        try { CachedIndicatorRepository(remote, Cache()) { 1000L }.quotes(); fail("Cancellation swallowed") }
        catch (_: CancellationException) { /* Expected. */ }
    }
}
