package com.vjaykrsna.nanoai.core.di

import com.vjaykrsna.nanoai.feature.uiux.domain.CommandPaletteActionProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides DataStore-backed UI preference dependencies. */
@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
  @Provides
  @Singleton
  fun provideCommandPaletteActionProvider(): CommandPaletteActionProvider =
    CommandPaletteActionProvider()
}
