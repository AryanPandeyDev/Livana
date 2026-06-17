package com.livana.app.core.network.mapper

import com.livana.app.core.model.PlatformStats
import com.livana.app.core.model.Usdc
import com.livana.app.core.network.dto.PlatformStatsDto
import java.math.BigInteger

fun PlatformStatsDto.toDomain(): PlatformStats = PlatformStats(
    totalDonated = totalDonated.toUsdc(),
    totalReleased = totalReleased.toUsdc(),
    totalPoolsCount = totalPoolsCount,
    activePoolsCount = activePoolsCount,
    verifiedNgosCount = verifiedNgosCount,
)

internal fun Long.toUsdc(): Usdc = Usdc(BigInteger.valueOf(this))
