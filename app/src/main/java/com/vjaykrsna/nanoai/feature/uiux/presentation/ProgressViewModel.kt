package com.vjaykrsna.nanoai.feature.uiux.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vjaykrsna.nanoai.core.common.MainImmediateDispatcher
import com.vjaykrsna.nanoai.feature.uiux.domain.JobOperationsUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.ProgressCenterCoordinator
import com.vjaykrsna.nanoai.feature.uiux.domain.QueueJobUseCase
import com.vjaykrsna.nanoai.feature.uiux.domain.UndoActionUseCase
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel responsible for background job progress management and coordination. */
class ProgressViewModel
@Inject
constructor(
  private val progressCoordinator: ProgressCenterCoordinator,
  private val queueJobUseCase: QueueJobUseCase,
  private val jobOperationsUseCase: JobOperationsUseCase,
  private val undoActionUseCase: UndoActionUseCase,
  @MainImmediateDispatcher private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {

  /** Flow of all active progress jobs for display in progress center. */
  val progressJobs: StateFlow<List<ProgressJob>> =
    progressCoordinator.progressJobs.stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = emptyList(),
    )

  /** Queues a generation job (e.g., when offline or model busy). */
  fun queueGeneration(job: ProgressJob) {
    queueJobUseCase.execute(job)
  }

  /** Completes a job, removing it from the progress center. */
  fun completeJob(jobId: UUID) {
    jobOperationsUseCase.completeJob(jobId)
  }

  /** Attempts to retry a failed job via the progress coordinator. */
  fun retryJob(job: ProgressJob) {
    jobOperationsUseCase.retryJob(job.jobId)
  }

  /** Cancels an active job. */
  fun cancelJob(jobId: UUID) {
    viewModelScope.launch(dispatcher) { progressCoordinator.cancelJob(jobId) }
  }

  /** Pauses a running job. */
  fun pauseJob(jobId: UUID) {
    viewModelScope.launch(dispatcher) { progressCoordinator.pauseJob(jobId) }
  }

  /** Resumes a paused job. */
  fun resumeJob(jobId: UUID) {
    viewModelScope.launch(dispatcher) { progressCoordinator.resumeJob(jobId) }
  }

  /** Executes an undo action based on the provided payload. */
  fun undoAction(payload: UndoPayload) {
    undoActionUseCase.execute(payload)
  }
}
