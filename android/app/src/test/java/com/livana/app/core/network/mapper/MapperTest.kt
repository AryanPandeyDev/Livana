package com.livana.app.core.network.mapper

import com.livana.app.core.model.Region
import com.livana.app.core.network.dto.PlatformStatsDto
import com.livana.app.core.network.dto.PoolSummaryDto
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapperTest {
    @Test
    fun `platform stats mapper preserves USDC atomic units`() {
        val domain = PlatformStatsDto(
            totalDonated = 50_000_000_000L,
            totalReleased = 20_000_000_000L,
            totalPoolsCount = 42L,
            activePoolsCount = 38L,
            verifiedNgosCount = 12L,
        ).toDomain()

        assertEquals(BigInteger.valueOf(50_000_000_000L), domain.totalDonated.atomic)
        assertEquals(BigInteger.valueOf(20_000_000_000L), domain.totalReleased.atomic)
        assertEquals(42L, domain.totalPoolsCount)
        assertEquals(38L, domain.activePoolsCount)
        assertEquals(12L, domain.verifiedNgosCount)
    }

    @Test
    fun `pool summary mapper preserves USDC atomic units and nullable cover cid`() {
        val domain = PoolSummaryDto(
            onChainAddress = "0x9f1ac54bef0dd2f6f3462ea0fa94fc62300d3a8e",
            title = "Flood Relief Fund",
            description = "Emergency aid",
            region = "South Asia",
            coverImageCid = null,
            targetAmount = 10_000_000_000L,
            totalDonated = 5_000_000_000L,
            totalReleased = 2_000_000_000L,
            isPaused = true,
            deployedAt = "2026-06-07T10:00:00.000+00:00",
        ).toDomain()

        assertEquals("0x9f1ac54bef0dd2f6f3462ea0fa94fc62300d3a8e", domain.onChainAddress)
        assertEquals(Region.SOUTH_ASIA, domain.region)
        assertNull(domain.coverImageCid)
        assertEquals(BigInteger.valueOf(10_000_000_000L), domain.targetAmount.atomic)
        assertEquals(BigInteger.valueOf(5_000_000_000L), domain.totalDonated.atomic)
        assertEquals(BigInteger.valueOf(2_000_000_000L), domain.totalReleased.atomic)
        assertTrue(domain.isPaused)
    }
}
