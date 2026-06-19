package com.livana.app.di

import com.livana.app.core.chain.wallet.ReownWalletConnector
import com.livana.app.core.chain.wallet.WalletConnector
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WalletModule {
    @Binds
    @Singleton
    abstract fun bindWalletConnector(impl: ReownWalletConnector): WalletConnector
}
