package com.vjaykrsna.nanoai.core.data.repository.impl

import com.vjaykrsna.nanoai.core.data.db.daos.PersonaSwitchLogDao
import com.vjaykrsna.nanoai.core.data.db.entities.PersonaSwitchLogEntity
import com.vjaykrsna.nanoai.core.domain.model.PersonaSwitchLog
import com.vjaykrsna.nanoai.core.model.PersonaSwitchAction
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(MainDispatcherExtension::class)
class PersonaSwitchLogRepositoryImplTest {

    private lateinit var personaSwitchLogDao: PersonaSwitchLogDao
    private lateinit var repository: PersonaSwitchLogRepositoryImpl
    private val testDispatcher = MainDispatcherExtension().dispatcher

    @BeforeEach
    fun setUp() {
        personaSwitchLogDao = mockk(relaxed = true)
        repository = PersonaSwitchLogRepositoryImpl(
            personaSwitchLogDao = personaSwitchLogDao,
            ioDispatcher = testDispatcher
        )
    }

    private fun createTestLogEntity(threadId: UUID) = PersonaSwitchLogEntity(
        logId = UUID.randomUUID().toString(),
        threadId = threadId.toString(),
        newPersonaId = UUID.randomUUID().toString(),
        actionTaken = PersonaSwitchAction.CONTINUE_THREAD,
        createdAt = Clock.System.now()
    )

    private fun createTestLog(threadId: UUID) = PersonaSwitchLog(
        logId = UUID.randomUUID(),
        threadId = threadId,
        previousPersonaId = null,
        newPersonaId = UUID.randomUUID(),
        actionTaken = PersonaSwitchAction.CONTINUE_THREAD,
        createdAt = Clock.System.now()
    )

    @Test
    fun `logSwitch should call dao to insert a log`() = runTest {
        // Given
        val log = createTestLog(UUID.randomUUID())

        // When
        repository.logSwitch(log)

        // Then
        coVerify { personaSwitchLogDao.insert(any()) }
    }

    @Test
    fun `getSwitchHistory should fetch and map switch history from dao`() = runTest {
        // Given
        val threadId = UUID.randomUUID()
        val entities = listOf(createTestLogEntity(threadId))
        coEvery { personaSwitchLogDao.getByThreadId(threadId.toString()) } returns entities

        // When
        val result = repository.getSwitchHistory(threadId)

        // Then
        assertThat(result).hasSize(1)
    }

    @Test
    fun `getLatestSwitch should fetch and map the latest switch`() = runTest {
        // Given
        val threadId = UUID.randomUUID()
        val entity = createTestLogEntity(threadId)
        coEvery { personaSwitchLogDao.getLatestForThread(threadId.toString()) } returns entity

        // When
        val result = repository.getLatestSwitch(threadId)

        // Then
        assertThat(result).isNotNull()
    }

    @Test
    fun `getLogsByThreadId should return a flow of mapped logs`() = runTest {
        // Given
        val threadId = UUID.randomUUID()
        val entities = listOf(createTestLogEntity(threadId))
        coEvery { personaSwitchLogDao.observeByThreadId(threadId.toString()) } returns flowOf(entities)

        // When
        val result = repository.getLogsByThreadId(threadId).first()

        // Then
        assertThat(result).hasSize(1)
    }
}
