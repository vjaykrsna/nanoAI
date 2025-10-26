package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.uiux.domain.QueueJobUseCase
import com.vjaykrsna.nanoai.feature.uiux.ui.shell.ShellUiEvent
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
class ShellViewModelJobQueueTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun queueGeneration_offline_jobQueuedWithPendingUndo() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
      runBlocking {
        fakeRepos.connectivityRepository.updateConnectivity(ConnectivityStatus.OFFLINE)
      }
      val queueJobUseCase = mockk<QueueJobUseCase>(relaxed = true)

      // Mock sub-ViewModels
      val navigationViewModel = mockk<NavigationViewModel>(relaxed = true)
      val connectivityViewModel = mockk<ConnectivityViewModel>(relaxed = true)
      val progressViewModel = mockk<ProgressViewModel>(relaxed = true)
      val themeViewModel = mockk<ThemeViewModel>(relaxed = true)

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
            com.vjaykrsna.nanoai.feature.uiux.presentation.UndoPayload(
              actionId = "queue-${job.jobId}",
              metadata = mapOf("message" to message, "jobId" to job.jobId.toString()),
            )
          )
        }

      val viewModel =
        ShellViewModel(
          fakeRepos.navigationRepository,
          navigationViewModel,
          connectivityViewModel,
          progressViewModel,
          themeViewModel,
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

      viewModel.onEvent(ShellUiEvent.QueueJob(job))
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
      val queueJobUseCase = mockk<QueueJobUseCase>(relaxed = true)

      // Mock sub-ViewModels
      val navigationViewModel = mockk<NavigationViewModel>(relaxed = true)
      val connectivityViewModel = mockk<ConnectivityViewModel>(relaxed = true)
      val progressViewModel = mockk<ProgressViewModel>(relaxed = true)
      val themeViewModel = mockk<ThemeViewModel>(relaxed = true)

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
            com.vjaykrsna.nanoai.feature.uiux.presentation.UndoPayload(
              actionId = "queue-${job.jobId}",
              metadata = mapOf("message" to message, "jobId" to job.jobId.toString()),
            )
          )
        }

      val viewModel =
        ShellViewModel(
          fakeRepos.navigationRepository,
          navigationViewModel,
          connectivityViewModel,
          progressViewModel,
          themeViewModel,
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

      viewModel.onEvent(ShellUiEvent.QueueJob(job))
      advanceUntilIdle()

      val progressJobs = fakeRepos.progressRepository.progressJobs.first()
      val undoPayload = fakeRepos.navigationRepository.undoPayload.first()
      val message = undoPayload?.metadata?.get("message") as? String
      assertThat(message).isEqualTo("Model download retry scheduled")
      assertThat(progressJobs.map { it.jobId }).contains(jobId)
    }
}
