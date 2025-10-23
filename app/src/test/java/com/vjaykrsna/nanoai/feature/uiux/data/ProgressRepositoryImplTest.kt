package com.vjaykrsna.nanoai.feature.uiux.data

import com.vjaykrsna.nanoai.feature.uiux.state.JobStatus
import com.vjaykrsna.nanoai.feature.uiux.state.JobType
import com.vjaykrsna.nanoai.feature.uiux.state.ProgressJob
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant
import java.util.UUID

class ProgressRepositoryImplTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    private lateinit var repository: ProgressRepositoryImpl

    @BeforeEach
    fun setUp() {
        repository = ProgressRepositoryImpl(mainDispatcherExtension.dispatcher)
    }

    private fun createTestJob() = ProgressJob(
        jobId = UUID.randomUUID(),
        type = JobType.OTHER,
        status = JobStatus.PENDING,
        progress = 0f,
        queuedAt = Instant.now()
    )

    @Test
    fun `queueJob should add a new job to the list`() = runTest {
        // Given
        val job = createTestJob()

        // When
        repository.queueJob(job)
        val jobs = repository.progressJobs.first()

        // Then
        assertThat(jobs).hasSize(1)
        assertThat(jobs[0].jobId).isEqualTo(job.jobId)
    }

    @Test
    fun `completeJob should remove a job from the list`() = runTest {
        // Given
        val job = createTestJob()
        repository.queueJob(job)

        // When
        repository.completeJob(job.jobId)
        val jobs = repository.progressJobs.first()

        // Then
        assertThat(jobs).isEmpty()
    }
}
