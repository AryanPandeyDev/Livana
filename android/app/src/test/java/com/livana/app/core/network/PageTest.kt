package com.livana.app.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageTest {
    @Test
    fun `deserializes spring page envelope`() {
        val page = Json { ignoreUnknownKeys = true }.decodeFromString<Page<TestPool>>(
            """
                {
                  "content": [
                    { "id": "pool-1", "title": "Relief fund" },
                    { "id": "pool-2", "title": "Clinic rebuild" }
                  ],
                  "pageable": {
                    "pageNumber": 0,
                    "pageSize": 20,
                    "sort": { "sorted": true, "unsorted": false, "empty": false },
                    "offset": 0,
                    "paged": true,
                    "unpaged": false
                  },
                  "totalPages": 5,
                  "totalElements": 97,
                  "last": false,
                  "first": true,
                  "size": 20,
                  "number": 0,
                  "numberOfElements": 2,
                  "empty": false
                }
            """.trimIndent(),
        )

        assertEquals(2, page.content.size)
        assertEquals(TestPool(id = "pool-1", title = "Relief fund"), page.content.first())
        assertEquals(97L, page.totalElements)
        assertEquals(5, page.totalPages)
        assertTrue(page.first)
        assertFalse(page.last)
        assertEquals(0, page.number)
        assertEquals(20, page.size)
        assertEquals(2, page.numberOfElements)
        assertFalse(page.empty)
    }
}

@Serializable
private data class TestPool(
    val id: String,
    val title: String,
)
