package com.livana.app.core.data.repository

import com.livana.app.core.common.LivanaResult
import com.livana.app.core.model.PlatformStats

interface StatsRepository {
    suspend fun getPlatformStats(): LivanaResult<PlatformStats>
}
