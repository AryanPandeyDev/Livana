package com.livana.app.core.data.repository

import com.livana.app.core.common.LivanaResult
import com.livana.app.core.network.StatsApi
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StatsRepositoryImplTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: StatsRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = StatsRepositoryImpl(
            statsApi = testRetrofit(server).create(StatsApi::class.java),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getPlatformStats returns mapped platform stats`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                    {
                      "totalDonated": 50000000000,
                      "totalReleased": 20000000000,
                      "totalPoolsCount": 42,
                      "activePoolsCount": 38,
                      "verifiedNgosCount": 12
                    }
                """.trimIndent(),
            ),
        )

        val result = repository.getPlatformStats()

        assertTrue(result is LivanaResult.Success)
        val stats = (result as LivanaResult.Success).value
        assertEquals(BigInteger.valueOf(50_000_000_000L), stats.totalDonated.atomic)
        assertEquals(BigInteger.valueOf(20_000_000_000L), stats.totalReleased.atomic)
        assertEquals(42L, stats.totalPoolsCount)
        assertEquals(38L, stats.activePoolsCount)
        assertEquals(12L, stats.verifiedNgosCount)

        val request = checkNotNull(server.takeRequest(1, TimeUnit.SECONDS))
        assertEquals("/api/v1/stats", request.path)
    }
}
