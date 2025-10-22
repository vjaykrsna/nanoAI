package com.vjaykrsna.nanoai.feature.library.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.core.data.db.NanoAIDatabase
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.feature.library.data.catalog.ModelCatalogSource
import com.vjaykrsna.nanoai.feature.library.data.impl.ModelCatalogRepositoryImpl
import com.vjaykrsna.nanoai.feature.library.domain.RefreshModelCatalogUseCase
import com.vjaykrsna.nanoai.feature.library.model.InstallState
import com.vjaykrsna.nanoai.feature.library.model.ProviderType
import com.vjaykrsna.nanoai.model.catalog.DeliveryType
import com.vjaykrsna.nanoai.model.leap.LeapModelRemoteDataSource
import com.vjaykrsna.nanoai.testing.assertSuccess
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ModelCatalogOfflineTest {

  private lateinit var context: Context
  private lateinit var database: NanoAIDatabase
  private lateinit var repository: ModelCatalogRepositoryImpl
  private lateinit var useCase: RefreshModelCatalogUseCase
  private lateinit var failingSource: FailingModelCatalogSource
  private lateinit var dispatcher: TestDispatcher
  private lateinit var mockWebServer: MockWebServer
  private lateinit var leapModelRemoteDataSource: LeapModelRemoteDataSource

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    dispatcher = StandardTestDispatcher()
    Dispatchers.setMain(dispatcher)
    mockWebServer = MockWebServer().apply { start() }
    database =
      Room.inMemoryDatabaseBuilder(context, NanoAIDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    leapModelRemoteDataSource = mockk(relaxed = true)
    repository =
      ModelCatalogRepositoryImpl(
        database.modelPackageReadDao(),
        database.modelPackageWriteDao(),
        database.chatThreadDao(),
        leapModelRemoteDataSource,
        context,
        Clock.System,
      )
    failingSource = FailingModelCatalogSource(IOException("Device farm offline: HTTP 503"))
    useCase = RefreshModelCatalogUseCase(failingSource, repository)
  }

  @After
  fun tearDown() {
    database.close()
    mockWebServer.shutdown()
    Dispatchers.resetMain()
  }

  @Test
  fun offlineRefresh_preservesCachedCatalogAndSignalsFallbackSuccess() =
    runTest(dispatcher) {
      val cached = sampleModel("cached-model")
      repository.replaceCatalog(listOf(cached))

      val result = useCase()

      result.assertSuccess()
      assertThat(repository.getAllModels()).containsExactly(cached)
      assertThat(failingSource.fetchAttempts).isEqualTo(1)
      advanceUntilIdle()
      val status =
        repository.observeRefreshStatus().first { candidate ->
          candidate.lastFallbackReason != null
        }
      assertThat(status.lastFallbackReason).isEqualTo("IOException")
      assertThat(status.lastFallbackCachedCount).isEqualTo(1)
    }

  private fun sampleModel(id: String): ModelPackage =
    ModelPackage(
      modelId = id,
      displayName = "Sample $id",
      version = "1.0.0",
      providerType = ProviderType.CLOUD_API,
      deliveryType = DeliveryType.CLOUD_FALLBACK,
      minAppVersion = 1,
      sizeBytes = 1_024,
      capabilities = setOf("text"),
      installState = InstallState.NOT_INSTALLED,
      downloadTaskId = null,
      manifestUrl = "${mockWebServer.url("/")}$id.manifest",
      checksumSha256 = "checksum-$id",
      signature = null,
      createdAt = Instant.parse("2025-10-10T00:00:00Z"),
      updatedAt = Instant.parse("2025-10-10T00:00:00Z"),
    )

  private class FailingModelCatalogSource(private val error: IOException) : ModelCatalogSource {
    var fetchAttempts: Int = 0
      private set

    override suspend fun fetchCatalog(): List<ModelPackage> {
      fetchAttempts += 1
      throw error
    }
  }
}
