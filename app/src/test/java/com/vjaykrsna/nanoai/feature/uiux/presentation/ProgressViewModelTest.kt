package com.vjaykrsna.nanoai.feature.uiux.presentation

import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.feature.uiux.domain.JobOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.QueueJobUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.UndoActionUseCase
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import io.mockk.coEvery
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension(dispatcher)

  @Test
  fun queueGeneration_offline_queuesJob() =
    runTest(dispatcher) {
      val fakeRepos = createFakeRepositories()
      runBlocking {
        fakeRepos.connectivityRepository.updateConnectivity(ConnectivityStatus.OFFLINE)
      }
      val progressCoordinator = createFakeProgressCenterCoordinator()
      val queueJobUseCase = mockk<QueueJobUseCase>(relaxed = true)
      val jobOperationsUseCase = mockk<JobOperationsUseCase>(relaxed = true)
      val undoActionUseCase = mockk<UndoActionUseCase>(relaxed = true)

      // Set up queue job use case to actually call repository
      coEvery { queueJobUseCase.execute(any()) } coAnswers
        {
          val job = firstArg<ProgressJob>()
          fakeRepos.progressRepository.queueJob(job)
        }

      val viewModel =
        ProgressViewModel(
          progressCoordinator,
          queueJobUseCase,
          jobOperationsUseCase,
          undoActionUseCase,
          dispatcher,
        )

      val job =
        ProgressJob(
          jobId = UUID.randomUUID(),
          type = JobType.IMAGE_GENERATION,
          status = JobStatus.PENDING,
          progress = 0f,
          canRetry = true,
          queuedAt = java.time.Instant.now(),
          subtitle = "Test job",
        )

      viewModel.queueGeneration(job)
      advanceUntilIdle()

      val jobs = viewModel.progressJobs.first { it.isNotEmpty() }
      assertThat(jobs).hasSize(1)
      assertThat(jobs.first().jobId).isEqualTo(job.jobId)
    }
}
