package com.livana.app.di

import com.livana.app.core.data.repository.DonationRepository
import com.livana.app.core.data.repository.DonationRepositoryImpl
import com.livana.app.core.data.repository.PoolRepository
import com.livana.app.core.data.repository.PoolRepositoryImpl
import com.livana.app.core.data.repository.ProofRepository
import com.livana.app.core.data.repository.ProofRepositoryImpl
import com.livana.app.core.data.repository.ReputationRepository
import com.livana.app.core.data.repository.ReputationRepositoryImpl
import com.livana.app.core.data.repository.StatsRepository
import com.livana.app.core.data.repository.StatsRepositoryImpl
import com.livana.app.core.data.repository.UserRepository
import com.livana.app.core.data.repository.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository

    @Binds
    @Singleton
    abstract fun bindPoolRepository(impl: PoolRepositoryImpl): PoolRepository

    @Binds
    @Singleton
    abstract fun bindDonationRepository(impl: DonationRepositoryImpl): DonationRepository

    @Binds
    @Singleton
    abstract fun bindProofRepository(impl: ProofRepositoryImpl): ProofRepository

    @Binds
    @Singleton
    abstract fun bindReputationRepository(impl: ReputationRepositoryImpl): ReputationRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}

