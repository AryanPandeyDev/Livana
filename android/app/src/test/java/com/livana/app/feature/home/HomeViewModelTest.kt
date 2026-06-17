package com.livana.app.feature.home

import com.livana.app.core.common.BackendErrorCode
import com.livana.app.core.common.DomainError
import com.livana.app.core.common.LivanaResult
import com.livana.app.core.data.repository.PoolRepository
import com.livana.app.core.data.repository.StatsRepository
import com.livana.app.core.model.PagedResult
import com.livana.app.core.model.PlatformStats
import com.livana.app.core.model.PoolSummary
import com.livana.app.core.model.Region
import com.livana.app.core.model.Usdc
import java.math.BigInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `success loads content with stats and featured pools`() = runTest {
        val statsRepository = FakeStatsRepository(LivanaResult.Success(platformStats()))
        val poolRepository = FakePoolRepository(LivanaResult.Success(featuredPage()))

        val viewModel = HomeViewModel(statsRepository, poolRepository)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is HomeUiState.Content)
        val content = state as HomeUiState.Content
        assertEquals(BigInteger.valueOf(50_000_000_000L), content.totalDonated.atomic)
        assertEquals(BigInteger.valueOf(20_000_000_000L), content.totalReleased.atomic)
        assertEquals(38L, content.activePools)
        assertEquals(12L, content.verifiedNgos)
        assertEquals(42L, content.totalPools)
        assertEquals(listOf(featuredPool()), content.featuredPools)
        assertEquals(6, poolRepository.lastSize)
        assertEquals("deployedAt,desc", poolRepository.lastSort)
    }

    @Test
    fun `backend error loads error state`() = runTest {
        val statsRepository = FakeStatsRepository(
            LivanaResult.Failure(
                DomainError.Backend(
                    code = BackendErrorCode.ValidationError,
                    httpStatus = 400,
                    message = "bad request",
                    timestamp = null,
                ),
            ),
        )
        val poolRepository = FakePoolRepository(LivanaResult.Success(featuredPage()))

        val viewModel = HomeViewModel(statsRepository, poolRepository)
        advanceUntilIdle()

        assertTrue(viewModel.state.value is HomeUiState.Error)
    }

    @Test
    fun `unknown error loads error state`() = runTest {
        val statsRepository = FakeStatsRepository(LivanaResult.Success(platformStats()))
        val poolRepository = FakePoolRepository(
            LivanaResult.Failure(DomainError.Unknown(message = "boom")),
        )

        val viewModel = HomeViewModel(statsRepository, poolRepository)
        advanceUntilIdle()

        assertTrue(viewModel.state.value is HomeUiState.Error)
    }

    @Test
    fun `network error loads offline state`() = runTest {
        val statsRepository = FakeStatsRepository(LivanaResult.Success(platformStats()))
        val poolRepository = FakePoolRepository(
            LivanaResult.Failure(DomainError.Network()),
        )

        val viewModel = HomeViewModel(statsRepository, poolRepository)
        advanceUntilIdle()

        assertEquals(HomeUiState.Offline, viewModel.state.value)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeStatsRepository(
    private val result: LivanaResult<PlatformStats>,
) : StatsRepository {
    override suspend fun getPlatformStats(): LivanaResult<PlatformStats> = result
}

private class FakePoolRepository(
    private val result: LivanaResult<PagedResult<PoolSummary>>,
) : PoolRepository {
    var lastSize: Int? = null
        private set
    var lastSort: String? = null
        private set

    override suspend fun getPools(
        region: Region?,
        search: String?,
        page: Int?,
        size: Int?,
        sort: String?,
    ): LivanaResult<PagedResult<PoolSummary>> {
        lastSize = size
        lastSort = sort
        return result
    }
}

private fun platformStats(): PlatformStats = PlatformStats(
    totalDonated = usdc(50_000_000_000L),
    totalReleased = usdc(20_000_000_000L),
    totalPoolsCount = 42L,
    activePoolsCount = 38L,
    verifiedNgosCount = 12L,
)

private fun featuredPage(): PagedResult<PoolSummary> = PagedResult(
    content = listOf(featuredPool()),
    totalElements = 1L,
    totalPages = 1,
    first = true,
    last = true,
    number = 0,
    size = 6,
    numberOfElements = 1,
    empty = false,
)

private fun featuredPool(): PoolSummary = PoolSummary(
    onChainAddress = "0x9f1ac54bef0dd2f6f3462ea0fa94fc62300d3a8e",
    title = "Flood Relief Fund",
    description = "Emergency aid",
    region = Region.SOUTH_ASIA,
    coverImageCid = null,
    targetAmount = usdc(10_000_000_000L),
    totalDonated = usdc(5_000_000_000L),
    totalReleased = usdc(2_000_000_000L),
    isPaused = false,
    deployedAt = "2026-06-07T10:00:00.000+00:00",
)

private fun usdc(atomic: Long): Usdc = Usdc(BigInteger.valueOf(atomic))
