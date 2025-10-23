package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.uiux.domain.ConnectivityOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.JobOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.NavigationOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.QueueJobUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.SettingsOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.UndoActionUseCase
import com.vjaykrsna.nanoai.feature.uiux.state.ConnectivityStatus
import com.vjaykrsna.nanoai.feature.uiux.state.JobStatus
import com.vjaykrsna.nanoai.feature.uiux.state.JobType
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.*
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

private fun jobLabel(job: ProgressJob): String =
  when (job.type) {
    JobType.IMAGE_GENERATION -> "Image generation"
    JobType.AUDIO_RECORDING -> "Audio recording"
    JobType.MODEL_DOWNLOAD -> "Model download"
    JobType.TEXT_GENERATION -> "Text generation"
    JobType.TRANSLATION -> "Translation"
    JobType.OTHER -> "Background task"
  }

@OptIn(ExperimentalCoroutinesApi::class)
class ShellViewModelJobManagementTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun queueGeneration_offline_jobQueuedWithPendingUndo() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
      runBlocking {
        fakeRepos.connectivityRepository.updateConnectivity(ConnectivityStatus.OFFLINE)
      }
      val actionProvider = createFakeCommandPaletteActionProvider()
      val progressCoordinator = createFakeProgressCenterCoordinator()
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)
      val connectivityOperationsUseCase = mockk<ConnectivityOperationsUseCase>(relaxed = true)
      val queueJobUseCase = mockk<QueueJobUseCase>(relaxed = true)
      val jobOperationsUseCase = mockk<JobOperationsUseCase>(relaxed = true)
      val undoActionUseCase = mockk<UndoActionUseCase>(relaxed = true)
      val settingsOperationsUseCase = mockk<SettingsOperationsUseCase>(relaxed = true)

      // Set up queue job use case to actually call repository
      coEvery { queueJobUseCase.execute(any()) } coAnswers
        {
          val job = firstArg<ProgressJob>()
          fakeRepos.progressRepository.queueJob(job)
          val message =
            when {
              job.status == JobStatus.FAILED && job.canRetry -> "${jobLabel(job)} retry scheduled"
              fakeRepos.connectivityRepository.connectivityBannerState.first().status !=
                ConnectivityStatus.ONLINE -> "${jobLabel(job)} queued for reconnect"
              job.status == JobStatus.PENDING -> "${jobLabel(job)} queued"
              else -> "${jobLabel(job)} updated"
            }
          fakeRepos.navigationRepository.recordUndoPayload(
            com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload(
              actionId = "queue-${job.jobId}",
              metadata = mapOf("message" to message, "jobId" to job.jobId.toString()),
            )
          )
        }

      val viewModel =
        ShellViewModel(
          fakeRepos.navigationRepository,
          fakeRepos.connectivityRepository,
          fakeRepos.themeRepository,
          fakeRepos.progressRepository,
          fakeRepos.userProfileRepository,
          actionProvider,
          progressCoordinator,
          navigationOperationsUseCase,
          connectivityOperationsUseCase,
          queueJobUseCase,
          jobOperationsUseCase,
          undoActionUseCase,
          settingsOperationsUseCase,
          dispatcher,
        )

      val jobId = UUID.randomUUID()
      val job =
        ProgressJob(
          jobId = jobId,
          type = JobType.IMAGE_GENERATION,
          status = JobStatus.PENDING,
          progress = 0f,
          eta = Duration.ofSeconds(90),
          canRetry = true,
          queuedAt = Instant.parse("2025-10-06T00:00:00Z"),
        )

      viewModel.queueGeneration(job)
      advanceUntilIdle()

      val progressJobs = fakeRepos.progressRepository.progressJobs.first()
      assertThat(progressJobs.map { it.jobId }).contains(jobId)
      val undoPayload = fakeRepos.navigationRepository.undoPayload.first()
      assertThat(undoPayload).isNotNull()
      val message = undoPayload?.metadata?.get("message") as? String
      assertThat(message).isEqualTo("Image generation queued for reconnect")
    }

  @Test
  fun queueGeneration_retryableFailure_setsRetryMessage() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
      val actionProvider = createFakeCommandPaletteActionProvider()
      val progressCoordinator = createFakeProgressCenterCoordinator()
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)
      val connectivityOperationsUseCase = mockk<ConnectivityOperationsUseCase>(relaxed = true)
      val queueJobUseCase = mockk<QueueJobUseCase>(relaxed = true)
      val jobOperationsUseCase = mockk<JobOperationsUseCase>(relaxed = true)
      val undoActionUseCase = mockk<UndoActionUseCase>(relaxed = true)
      val settingsOperationsUseCase = mockk<SettingsOperationsUseCase>(relaxed = true)

      // Set up queue job use case to actually call repository
      coEvery { queueJobUseCase.execute(any()) } coAnswers
        {
          val job = firstArg<ProgressJob>()
          fakeRepos.progressRepository.queueJob(job)
          val message =
            when {
              job.status == JobStatus.FAILED && job.canRetry -> "${jobLabel(job)} retry scheduled"
              fakeRepos.connectivityRepository.connectivityBannerState.first().status !=
                ConnectivityStatus.ONLINE -> "${jobLabel(job)} queued for reconnect"
              job.status == JobStatus.PENDING -> "${jobLabel(job)} queued"
              else -> "${jobLabel(job)} updated"
            }
          fakeRepos.navigationRepository.recordUndoPayload(
            com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload(
              actionId = "queue-${job.jobId}",
              metadata = mapOf("message" to message, "jobId" to job.jobId.toString()),
            )
          )
        }

      val viewModel =
        ShellViewModel(
          fakeRepos.navigationRepository,
          fakeRepos.connectivityRepository,
          fakeRepos.themeRepository,
          fakeRepos.progressRepository,
          fakeRepos.userProfileRepository,
          actionProvider,
          progressCoordinator,
          navigationOperationsUseCase,
          connectivityOperationsUseCase,
          queueJobUseCase,
          jobOperationsUseCase,
          undoActionUseCase,
          settingsOperationsUseCase,
          dispatcher,
        )

      val jobId = UUID.randomUUID()
      val job =
        ProgressJob(
          jobId = jobId,
          type = JobType.MODEL_DOWNLOAD,
          status = JobStatus.FAILED,
          progress = 0f,
          eta = Duration.ofSeconds(60),
          canRetry = true,
          queuedAt = Instant.parse("2025-10-06T01:00:00Z"),
        )

      viewModel.queueGeneration(job)
      advanceUntilIdle()

      val progressJobs = fakeRepos.progressRepository.progressJobs.first()
      val undoPayload = fakeRepos.navigationRepository.undoPayload.first()
      val message = undoPayload?.metadata?.get("message") as? String
      assertThat(message).isEqualTo("Model download retry scheduled")
      assertThat(progressJobs.map { it.jobId }).contains(jobId)
    }

  @Test
  fun undoAction_clearsPendingJobAndUndoPayload() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
      val actionProvider = createFakeCommandPaletteActionProvider()
      val progressCoordinator = createFakeProgressCenterCoordinator()
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)
      val connectivityOperationsUseCase = mockk<ConnectivityOperationsUseCase>(relaxed = true)
      val queueJobUseCase = mockk<QueueJobUseCase>()
      coEvery { queueJobUseCase.execute(any()) } coAnswers
        {
          val job = firstArg<ProgressJob>()
          fakeRepos.progressRepository.queueJob(job)
          val message =
            when {
              job.status == JobStatus.FAILED && job.canRetry -> "${jobLabel(job)} retry scheduled"
              fakeRepos.connectivityRepository.connectivityBannerState.first().status !=
                ConnectivityStatus.ONLINE -> "${jobLabel(job)} queued for reconnect"
              job.status == JobStatus.PENDING -> "${jobLabel(job)} queued"
              else -> "${jobLabel(job)} updated"
            }
          fakeRepos.navigationRepository.recordUndoPayload(
            com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload(
              actionId = "queue-${job.jobId}",
              metadata = mapOf("message" to message, "jobId" to job.jobId.toString()),
            )
          )
        }
      val jobOperationsUseCase = mockk<JobOperationsUseCase>(relaxed = true)
      val undoActionUseCase = mockk<UndoActionUseCase>()
      coEvery { undoActionUseCase.execute(any()) } coAnswers
        {
          val payload = firstArg<com.vjaykrsna.nanoai.feature.uiux.state.UndoPayload>()
          if (payload.actionId.startsWith("queue-")) {
            val jobIdString = requireNotNull(payload.metadata["jobId"] as? String) { "UndoPayload metadata missing or invalid jobId" }
            val jobId = UUID.fromString(jobIdString)
            fakeRepos.progressRepository.completeJob(jobId)
          }
          fakeRepos.navigationRepository.recordUndoPayload(null)
        }
      val settingsOperationsUseCase = mockk<SettingsOperationsUseCase>(relaxed = true)
      val viewModel =
        ShellViewModel(
          fakeRepos.navigationRepository,
          fakeRepos.connectivityRepository,
          fakeRepos.themeRepository,
          fakeRepos.progressRepository,
          fakeRepos.userProfileRepository,
          actionProvider,
          progressCoordinator,
          navigationOperationsUseCase,
          connectivityOperationsUseCase,
          queueJobUseCase,
          jobOperationsUseCase,
          undoActionUseCase,
          settingsOperationsUseCase,
          dispatcher,
        )

      val jobId = UUID.randomUUID()
      val job =
        ProgressJob(
          jobId = jobId,
          type = JobType.IMAGE_GENERATION,
          status = JobStatus.PENDING,
          progress = 0f,
          eta = Duration.ofSeconds(120),
          canRetry = true,
          queuedAt = Instant.parse("2025-10-06T02:00:00Z"),
        )

      viewModel.queueGeneration(job)
      advanceUntilIdle()

      val progressJobs = fakeRepos.progressRepository.progressJobs.first()
      val payload =
        requireNotNull(
          (fakeRepos.navigationRepository as FakeNavigationRepository).undoPayloadFlow.value
        )

      viewModel.undoAction(payload)
      advanceUntilIdle()

      val clearedProgressJobs = fakeRepos.progressRepository.progressJobs.first()
      val clearedUndoPayload =
        (fakeRepos.navigationRepository as FakeNavigationRepository).undoPayloadFlow.value
      assertThat(clearedUndoPayload).isNull()
      assertThat(clearedProgressJobs).isEmpty()
    }

  @Test
  fun completeJob_removesJobAndClearsUndo() =
    runTest(dispatcher) {
      val jobId = UUID.randomUUID()
      val fakeRepos = createFakeRepositories()
      // Pre-populate the progress repository with a job
      runBlocking {
        fakeRepos.progressRepository.queueJob(
          ProgressJob(
            jobId = jobId,
            type = JobType.MODEL_DOWNLOAD,
            status = JobStatus.RUNNING,
            progress = 0.5f,
            eta = Duration.ofSeconds(30),
            canRetry = false,
            queuedAt = Instant.parse("2025-10-06T00:00:00Z"),
          )
        )
      }
      val actionProvider = createFakeCommandPaletteActionProvider()
      val progressCoordinator = createFakeProgressCenterCoordinator()
      val navigationOperationsUseCase = mockk<NavigationOperationsUseCase>(relaxed = true)
      val connectivityOperationsUseCase = mockk<ConnectivityOperationsUseCase>(relaxed = true)
      val queueJobUseCase = mockk<QueueJobUseCase>(relaxed = true)
      val jobOperationsUseCase = mockk<JobOperationsUseCase>(relaxed = true)
      val undoActionUseCase = mockk<UndoActionUseCase>(relaxed = true)
      val settingsOperationsUseCase = mockk<SettingsOperationsUseCase>(relaxed = true)

      // Set up job operations use case to actually call repository
      every { jobOperationsUseCase.completeJob(any()) } answers
        {
          runBlocking { fakeRepos.progressRepository.completeJob(firstArg()) }
        }

      val viewModel =
        ShellViewModel(
          fakeRepos.navigationRepository,
          fakeRepos.connectivityRepository,
          fakeRepos.themeRepository,
          fakeRepos.progressRepository,
          fakeRepos.userProfileRepository,
          actionProvider,
          progressCoordinator,
          navigationOperationsUseCase,
          connectivityOperationsUseCase,
          queueJobUseCase,
          jobOperationsUseCase,
          undoActionUseCase,
          settingsOperationsUseCase,
          dispatcher,
        )

      viewModel.completeJob(jobId)
      advanceUntilIdle()

      val progressJobs = fakeRepos.progressRepository.progressJobs.first()
      assertThat(progressJobs).isEmpty()
    }
}
