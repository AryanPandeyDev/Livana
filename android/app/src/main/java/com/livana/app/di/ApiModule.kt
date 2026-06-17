package com.livana.app.di

import com.livana.app.core.network.DonationApi
import com.livana.app.core.network.PoolApi
import com.livana.app.core.network.ProofApi
import com.livana.app.core.network.StatsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides
    @Singleton
    fun provideStatsApi(retrofit: Retrofit): StatsApi = retrofit.create(StatsApi::class.java)

    @Provides
    @Singleton
    fun providePoolApi(retrofit: Retrofit): PoolApi = retrofit.create(PoolApi::class.java)

    @Provides
    @Singleton
    fun provideDonationApi(retrofit: Retrofit): DonationApi = retrofit.create(DonationApi::class.java)

    @Provides
    @Singleton
    fun provideProofApi(retrofit: Retrofit): ProofApi = retrofit.create(ProofApi::class.java)
}

