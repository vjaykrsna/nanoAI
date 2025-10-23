package com.vjaykrsna.nanoai.core.data.repository.impl

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.vjaykrsna.nanoai.core.model.InferenceMode
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.jupiter.api.extension.ExtendWith

@RunWith(RobolectricTestRunner::class)
@ExtendWith(MainDispatcherExtension::class)
class InferencePreferenceRepositoryImplTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: InferencePreferenceRepositoryImpl
    private val testDispatcher = MainDispatcherExtension().dispatcher
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { context.preferencesDataStoreFile("test_inference_preferences") }
        )
        repository = InferencePreferenceRepositoryImpl(context, testDispatcher)
    }

    @Test
    fun `observeInferencePreference should return default when no value is set`() = testScope.runTest {
        // When
        val preference = repository.observeInferencePreference().first()

        // Then
        assertThat(preference.mode).isEqualTo(InferenceMode.LOCAL_FIRST)
    }

    @Test
    fun `setInferenceMode should update the value in DataStore`() = testScope.runTest {
        // When
        repository.setInferenceMode(InferenceMode.CLOUD_FIRST)
        val preference = repository.observeInferencePreference().first()

        // Then
        assertThat(preference.mode).isEqualTo(InferenceMode.CLOUD_FIRST)
    }
}
