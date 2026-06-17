package com.livana.app.core.data.repository

import com.livana.app.core.common.BackendErrorCode
import com.livana.app.core.common.DomainError
import com.livana.app.core.common.LivanaResult
import com.livana.app.core.model.Region
import com.livana.app.core.network.PoolApi
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PoolRepositoryImplTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: PoolRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repository = PoolRepositoryImpl(
            poolApi = testRetrofit(server).create(PoolApi::class.java),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getPools returns mapped paged pool summaries`() = runBlocking {
        server.enqueue(
            jsonResponse(
                """
                    {
                      "content": [
                        {
                          "onChainAddress": "0x9f1ac54bef0dd2f6f3462ea0fa94fc62300d3a8e",
                          "title": "Flood Relief Fund",
                          "description": "Emergency aid for flood-affected communities",
                          "region": "South Asia",
                          "coverImageCid": "QmXyz",
                          "targetAmount": 10000000000,
                          "totalDonated": 5000000000,
                          "totalReleased": 2000000000,
                          "isPaused": false,
                          "deployedAt": "2026-06-07T10:00:00.000+00:00"
                        }
                      ],
                      "totalElements": 42,
                      "totalPages": 3,
                      "first": false,
                      "last": false,
                      "number": 1,
                      "size": 10,
                      "numberOfElements": 1,
                      "empty": false
                    }
                """.trimIndent(),
            ),
        )

        val result = repository.getPools(
            region = Region.SOUTH_ASIA,
            search = "flood",
            page = 1,
            size = 10,
            sort = "deployedAt,desc",
        )

        assertTrue(result is LivanaResult.Success)
        val page = (result as LivanaResult.Success).value
        assertEquals(42L, page.totalElements)
        assertEquals(3, page.totalPages)
        assertFalse(page.first)
        assertFalse(page.last)
        assertEquals(1, page.number)
        assertEquals(10, page.size)
        assertEquals(1, page.numberOfElements)
        assertFalse(page.empty)

        val pool = page.content.single()
        assertEquals("0x9f1ac54bef0dd2f6f3462ea0fa94fc62300d3a8e", pool.onChainAddress)
        assertEquals("Flood Relief Fund", pool.title)
        assertEquals(Region.SOUTH_ASIA, pool.region)
        assertEquals("QmXyz", pool.coverImageCid)
        assertEquals(BigInteger.valueOf(10_000_000_000L), pool.targetAmount.atomic)
        assertEquals(BigInteger.valueOf(5_000_000_000L), pool.totalDonated.atomic)
        assertEquals(BigInteger.valueOf(2_000_000_000L), pool.totalReleased.atomic)
        assertFalse(pool.isPaused)
        assertEquals("2026-06-07T10:00:00.000+00:00", pool.deployedAt)

        val request = checkNotNull(server.takeRequest(1, TimeUnit.SECONDS))
        val url = checkNotNull(request.requestUrl)
        assertEquals("/api/v1/pools", url.encodedPath)
        assertEquals("South Asia", url.queryParameter("region"))
        assertEquals("flood", url.queryParameter("search"))
        assertEquals("1", url.queryParameter("page"))
        assertEquals("10", url.queryParameter("size"))
        assertEquals("deployedAt,desc", url.queryParameter("sort"))
    }

    @Test
    fun `getPools maps backend error response`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                        {
                          "errorCode": "VALIDATION_ERROR",
                          "message": "invalid region",
                          "timestamp": "2026-06-07T15:30:00.000+00:00"
                        }
                    """.trimIndent(),
                ),
        )

        val result = repository.getPools(region = Region.SOUTH_ASIA)

        assertTrue(result is LivanaResult.Failure)
        val error = (result as LivanaResult.Failure).error
        assertTrue(error is DomainError.Backend)
        val backendError = error as DomainError.Backend
        assertEquals(BackendErrorCode.ValidationError, backendError.code)
        assertEquals(400, backendError.httpStatus)
        assertEquals("invalid region", backendError.message)
        assertEquals("2026-06-07T15:30:00.000+00:00", backendError.timestamp)
    }
}
