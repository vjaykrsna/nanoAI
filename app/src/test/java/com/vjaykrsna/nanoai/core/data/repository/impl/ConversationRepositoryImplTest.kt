package com.vjaykrsna.nanoai.core.data.repository.impl

import com.vjaykrsna.nanoai.core.data.db.daos.ChatThreadDao
import com.vjaykrsna.nanoai.core.data.db.daos.MessageDao
import com.vjaykrsna.nanoai.core.data.db.entities.ChatThreadEntity
import com.vjaykrsna.nanoai.core.data.db.entities.MessageEntity
import com.vjaykrsna.nanoai.core.model.Role
import com.vjaykrsna.nanoai.core.model.MessageSource
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(MainDispatcherExtension::class)
class ConversationRepositoryImplTest {

    private lateinit var chatThreadDao: ChatThreadDao
    private lateinit var messageDao: MessageDao
    private lateinit var clock: Clock
    private lateinit var repository: ConversationRepositoryImpl
    private val testDispatcher = MainDispatcherExtension().dispatcher

    @BeforeEach
    fun setUp() {
        chatThreadDao = mockk(relaxed = true)
        messageDao = mockk(relaxed = true)
        clock = mockk(relaxed = true)
        repository = ConversationRepositoryImpl(
            chatThreadDao = chatThreadDao,
            messageDao = messageDao,
            clock = clock,
            ioDispatcher = testDispatcher
        )
    }

    private fun createTestThreadEntity(id: UUID) = ChatThreadEntity(
        threadId = id.toString(),
        title = "Test Thread",
        personaId = null,
        activeModelId = "test-model",
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
        isArchived = false
    )

    private fun createTestMessageEntity(threadId: UUID) = MessageEntity(
        messageId = UUID.randomUUID().toString(),
        threadId = threadId.toString(),
        role = Role.USER,
        text = "Test message",
        source = MessageSource.LOCAL_MODEL,
        createdAt = Clock.System.now()
    )

    @Test
    fun `getThread should fetch and map a thread from the dao`() = runTest {
        // Given
        val threadId = UUID.randomUUID()
        val entity = createTestThreadEntity(threadId)
        coEvery { chatThreadDao.getById(threadId.toString()) } returns entity

        // When
        val result = repository.getThread(threadId)

        // Then
        assertThat(result).isNotNull()
        assertThat(result?.threadId).isEqualTo(threadId)
    }

    @Test
    fun `getAllThreads should fetch and map all active threads`() = runTest {
        // Given
        val entities = listOf(createTestThreadEntity(UUID.randomUUID()))
        coEvery { chatThreadDao.getAllActive() } returns entities

        // When
        val result = repository.getAllThreads()

        // Then
        assertThat(result).hasSize(1)
    }

    @Test
    fun `createThread should insert a new thread via the dao`() = runTest {
        // Given
        val thread = DomainTestBuilders.buildChatThread(threadId = UUID.randomUUID())

        // When
        repository.createThread(thread)

        // Then
        coVerify { chatThreadDao.insert(any()) }
    }

    @Test
    fun `updateThread should update a thread via the dao`() = runTest {
        // Given
        val thread = DomainTestBuilders.buildChatThread(threadId = UUID.randomUUID())

        // When
        repository.updateThread(thread)

        // Then
        coVerify { chatThreadDao.update(any()) }
    }

    @Test
    fun `archiveThread should archive a thread via the dao`() = runTest {
        // Given
        val threadId = UUID.randomUUID()

        // When
        repository.archiveThread(threadId)

        // Then
        coVerify { chatThreadDao.archive(threadId.toString()) }
    }

    @Test
    fun `deleteThread should delete a thread via the dao`() = runTest {
        // Given
        val threadId = UUID.randomUUID()
        val entity = createTestThreadEntity(threadId)
        coEvery { chatThreadDao.getById(threadId.toString()) } returns entity

        // When
        repository.deleteThread(threadId)

        // Then
        coVerify { chatThreadDao.delete(entity) }
    }

    @Test
    fun `addMessage should insert a message and touch the thread`() = runTest {
        // Given
        val message = DomainTestBuilders.buildMessage(threadId = UUID.randomUUID())
        coEvery { clock.now() } returns Instant.fromEpochMilliseconds(0)

        // When
        repository.addMessage(message)

        // Then
        coVerify { messageDao.insert(any()) }
        coVerify { chatThreadDao.touch(message.threadId.toString(), message.createdAt) }
    }

    @Test
    fun `getMessages should fetch and map messages for a thread`() = runTest {
        // Given
        val threadId = UUID.randomUUID()
        val entities = listOf(createTestMessageEntity(threadId))
        coEvery { messageDao.getByThreadId(threadId.toString()) } returns entities

        // When
        val result = repository.getMessages(threadId)

        // Then
        assertThat(result).hasSize(1)
    }

    @Test
    fun `getMessagesFlow should return a flow of mapped messages`() = runTest {
        // Given
        val threadId = UUID.randomUUID()
        val entities = listOf(createTestMessageEntity(threadId))
        coEvery { messageDao.observeByThreadId(threadId.toString()) } returns flowOf(entities)

        // When
        val result = repository.getMessagesFlow(threadId).first()

        // Then
        assertThat(result).hasSize(1)
    }

    @Test
    fun `getAllThreadsFlow should return a flow of mapped threads`() = runTest {
        // Given
        val entities = listOf(createTestThreadEntity(UUID.randomUUID()))
        coEvery { chatThreadDao.observeAllActive() } returns flowOf(entities)

        // When
        val result = repository.getAllThreadsFlow().first()

        // Then
        assertThat(result).hasSize(1)
    }

    @Test
    fun `createNewThread should insert a new thread with a new UUID`() = runTest {
        // Given
        val personaId = UUID.randomUUID()
        coEvery { clock.now() } returns Instant.fromEpochMilliseconds(0)

        // When
        val newThreadId = repository.createNewThread(personaId, "New Thread")

        // Then
        coVerify { chatThreadDao.insert(any()) }
        assertThat(newThreadId).isNotNull()
    }
}
