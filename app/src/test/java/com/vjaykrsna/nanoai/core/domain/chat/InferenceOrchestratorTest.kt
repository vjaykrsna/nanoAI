package com.vjaykrsna.nanoai.core.domain.chat

import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.domain.library.ModelCatalogRepository
import com.vjaykrsna.nanoai.core.domain.model.ApiProviderConfig
import com.vjaykrsna.nanoai.core.domain.model.InferencePreference
import com.vjaykrsna.nanoai.core.domain.model.ModelPackage
import com.vjaykrsna.nanoai.core.domain.model.library.DeliveryType
import com.vjaykrsna.nanoai.core.domain.model.library.InstallState
import com.vjaykrsna.nanoai.core.domain.model.library.ProviderType
import com.vjaykrsna.nanoai.core.domain.repository.ApiProviderConfigRepository
import com.vjaykrsna.nanoai.core.domain.repository.InferencePreferenceRepository
import com.vjaykrsna.nanoai.core.model.APIType
import com.vjaykrsna.nanoai.core.model.InferenceMode
import com.vjaykrsna.nanoai.core.model.MessageSource
import com.vjaykrsna.nanoai.core.network.CloudGatewayClient
import com.vjaykrsna.nanoai.core.network.CloudGatewayResult
import com.vjaykrsna.nanoai.core.network.ConnectivityStatusProvider
import com.vjaykrsna.nanoai.core.network.dto.CompletionChoiceDto
import com.vjaykrsna.nanoai.core.network.dto.CompletionChoiceMessageDto
import com.vjaykrsna.nanoai.core.network.dto.CompletionResponseDto
import com.vjaykrsna.nanoai.core.network.dto.CompletionResponseRole
import com.vjaykrsna.nanoai.core.runtime.LocalGenerationResult
import com.vjaykrsna.nanoai.core.runtime.LocalModelRuntime
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import com.vjaykrsna.nanoai.testing.assertRecoverableError
import com.vjaykrsna.nanoai.testing.assertSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MainDispatcherExtension::class)
class InferenceOrchestratorTest {

  private lateinit var orchestrator: InferenceOrchestrator
  private lateinit var modelCatalogRepository: ModelCatalogRepository
  private lateinit var apiProviderConfigRepository: ApiProviderConfigRepository
  private lateinit var inferencePreferenceRepository: InferencePreferenceRepository
  private lateinit var localModelRuntime: LocalModelRuntime
  private lateinit var cloudGatewayClient: CloudGatewayClient
  private lateinit var connectivityStatusProvider: ConnectivityStatusProvider

  private val testLocalModel =
    ModelPackage(
      modelId = "local-model-1",
      displayName = "Local Model",
      version = "1.0",
      providerType = ProviderType.MEDIA_PIPE,
      deliveryType = DeliveryType.LOCAL_ARCHIVE,
      minAppVersion = 1,
      sizeBytes = 1000L,
      capabilities = emptySet(),
      installState = InstallState.INSTALLED,
      manifestUrl = "url",
      createdAt = Instant.fromEpochMilliseconds(0),
      updatedAt = Instant.fromEpochMilliseconds(0),
      description = "A local model",
    )

  private val testCloudModel =
    ModelPackage(
      modelId = "cloud-model-1",
      displayName = "Cloud Model",
      version = "1.0",
      providerType = ProviderType.CLOUD_API,
      deliveryType = DeliveryType.CLOUD_FALLBACK,
      minAppVersion = 1,
      sizeBytes = 0L,
      capabilities = emptySet(),
      installState = InstallState.INSTALLED,
      manifestUrl = "",
      createdAt = Instant.fromEpochMilliseconds(0),
      updatedAt = Instant.fromEpochMilliseconds(0),
      description = "A cloud model",
    )

  private val testProvider =
    ApiProviderConfig(
      providerId = "openai",
      providerName = "OpenAI",
      baseUrl = "https://api.openai.com",
      apiType = APIType.OPENAI_COMPATIBLE,
      isEnabled = true,
    )

  @BeforeEach
  fun setup() {
    modelCatalogRepository = mockk(relaxed = true)
    apiProviderConfigRepository = mockk(relaxed = true)
    inferencePreferenceRepository = mockk(relaxed = true)
    localModelRuntime = mockk(relaxed = true)
    cloudGatewayClient = mockk(relaxed = true)
    connectivityStatusProvider = mockk(relaxed = true)

    orchestrator =
      InferenceOrchestrator(
        modelCatalogRepository,
        apiProviderConfigRepository,
        inferencePreferenceRepository,
        localModelRuntime,
        cloudGatewayClient,
        connectivityStatusProvider,
      )

    // Default mocks
    every { inferencePreferenceRepository.observeInferencePreference() } returns
      flowOf(InferencePreference(mode = InferenceMode.LOCAL_FIRST))
    coEvery { connectivityStatusProvider.isOnline() } returns true
    coEvery { modelCatalogRepository.getInstalledModels() } returns listOf(testLocalModel)
  }

  @Test
  fun `isOnline delegates to connectivity provider`() = runTest {
    coEvery { connectivityStatusProvider.isOnline() } returns true
    assertTrue(orchestrator.isOnline())

    coEvery { connectivityStatusProvider.isOnline() } returns false
    assertFalse(orchestrator.isOnline())
  }

  @Test
  fun `hasLocalModelAvailable returns true when local model exists and is ready`() = runTest {
    coEvery { modelCatalogRepository.getInstalledModels() } returns listOf(testLocalModel)
    coEvery { localModelRuntime.hasReadyModel(any()) } returns true

    assertTrue(orchestrator.hasLocalModelAvailable())
  }

  @Test
  fun `hasLocalModelAvailable returns false when no local models installed`() = runTest {
    coEvery { modelCatalogRepository.getInstalledModels() } returns emptyList()

    assertFalse(orchestrator.hasLocalModelAvailable())
  }

  @Test
  fun `hasLocalModelAvailable returns false when local models exist but not ready`() = runTest {
    coEvery { modelCatalogRepository.getInstalledModels() } returns listOf(testLocalModel)
    coEvery { localModelRuntime.hasReadyModel(any()) } returns false

    assertFalse(orchestrator.hasLocalModelAvailable())
  }

  @Test
  fun `generateResponse uses local model when LOCAL_FIRST and online`() = runTest {
    // Setup
    val prompt = "Hello"
    val expectedText = "Local response"

    coEvery { localModelRuntime.isModelReady(testLocalModel.modelId) } returns true
    coEvery { localModelRuntime.generate(any()) } returns
      NanoAIResult.success(
        LocalGenerationResult(text = expectedText, latencyMs = 100, metadata = emptyMap())
      )

    // Act
    val result = orchestrator.generateResponse(prompt, null)

    // Assert
    val successData = result.assertSuccess()
    assertEquals(expectedText, successData.text)
    assertEquals(MessageSource.LOCAL_MODEL, successData.source)

    coVerify { localModelRuntime.generate(any()) }
    coVerify(exactly = 0) { cloudGatewayClient.createCompletion(any(), any()) }
  }

  @Test
  fun `generateResponse falls back to cloud when local fails and online`() = runTest {
    // Setup
    val prompt = "Hello"
    val expectedText = "Cloud response"

    coEvery { localModelRuntime.isModelReady(testLocalModel.modelId) } returns true
    coEvery { localModelRuntime.generate(any()) } returns NanoAIResult.recoverable("Local error")

    coEvery { apiProviderConfigRepository.getEnabledProviders() } returns listOf(testProvider)
    coEvery { modelCatalogRepository.getAllModels() } returns listOf(testCloudModel)

    coEvery { cloudGatewayClient.createCompletion(any(), any()) } returns
      CloudGatewayResult.Success(
        CompletionResponseDto(
          id = "id",
          created = 123,
          model = "cloud-model-1",
          choices =
            listOf(
              CompletionChoiceDto(
                index = 0,
                message =
                  CompletionChoiceMessageDto(
                    role = CompletionResponseRole.ASSISTANT,
                    content = expectedText,
                  ),
                finishReason = "stop",
              )
            ),
          usage = null,
        ),
        latencyMs = 200,
      )

    // Act
    val result = orchestrator.generateResponse(prompt, null)

    // Assert
    val successData = result.assertSuccess()
    assertEquals(expectedText, successData.text)
    assertEquals(MessageSource.CLOUD_API, successData.source)

    coVerify { localModelRuntime.generate(any()) }
    coVerify { cloudGatewayClient.createCompletion(any(), any()) }
  }

  @Test
  fun `generateResponse uses cloud when CLOUD_FIRST`() = runTest {
    every { inferencePreferenceRepository.observeInferencePreference() } returns
      flowOf(InferencePreference(mode = InferenceMode.CLOUD_FIRST))

    val prompt = "Hello"
    val expectedText = "Cloud response"

    coEvery { apiProviderConfigRepository.getEnabledProviders() } returns listOf(testProvider)
    coEvery { modelCatalogRepository.getAllModels() } returns listOf(testCloudModel)

    coEvery { cloudGatewayClient.createCompletion(any(), any()) } returns
      CloudGatewayResult.Success(
        CompletionResponseDto(
          id = "id",
          created = 123,
          model = "cloud-model-1",
          choices =
            listOf(
              CompletionChoiceDto(
                index = 0,
                message =
                  CompletionChoiceMessageDto(
                    role = CompletionResponseRole.ASSISTANT,
                    content = expectedText,
                  ),
                finishReason = "stop",
              )
            ),
          usage = null,
        ),
        latencyMs = 200,
      )

    // Act
    val result = orchestrator.generateResponse(prompt, null)

    // Assert
    val successData = result.assertSuccess()
    assertEquals(expectedText, successData.text)
    assertEquals(MessageSource.CLOUD_API, successData.source)

    // Local should not be called because we preferred cloud and cloud succeeded
    coVerify(exactly = 0) { localModelRuntime.generate(any()) }
  }

  @Test
  fun `generateResponse returns offline error when offline and CLOUD_FIRST and no local models`() =
    runTest {
      every { inferencePreferenceRepository.observeInferencePreference() } returns
        flowOf(InferencePreference(mode = InferenceMode.CLOUD_FIRST))
      coEvery { connectivityStatusProvider.isOnline() } returns false
      coEvery { modelCatalogRepository.getInstalledModels() } returns emptyList()

      val result = orchestrator.generateResponse("Hello", null)

      result.assertRecoverableError()
      assertEquals("OFFLINE", (result as NanoAIResult.RecoverableError).telemetryId)
    }

  @Test
  fun `generateResponse forces local when offline even if CLOUD_FIRST`() = runTest {
    every { inferencePreferenceRepository.observeInferencePreference() } returns
      flowOf(InferencePreference(mode = InferenceMode.CLOUD_FIRST))
    coEvery { connectivityStatusProvider.isOnline() } returns false

    val expectedText = "Local response"
    coEvery { localModelRuntime.isModelReady(testLocalModel.modelId) } returns true
    coEvery { localModelRuntime.generate(any()) } returns
      NanoAIResult.success(
        LocalGenerationResult(text = expectedText, latencyMs = 100, metadata = emptyMap())
      )

    val result = orchestrator.generateResponse("Hello", null)

    val successData = result.assertSuccess()
    assertEquals(expectedText, successData.text)
    assertEquals(MessageSource.LOCAL_MODEL, successData.source)
  }
}
