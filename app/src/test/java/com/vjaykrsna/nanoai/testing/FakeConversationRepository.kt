package com.vjaykrsna.nanoai.testing

import com.vjaykrsna.nanoai.core.data.repository.ConversationRepository
import com.vjaykrsna.nanoai.core.domain.model.ChatThread
import com.vjaykrsna.nanoai.core.domain.model.Message
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of [ConversationRepository] for testing. Maintains in-memory state for
 * threads and messages.
 */
@Singleton
class FakeConversationRepository @Inject constructor() : ConversationRepository {
  private val _threads = MutableStateFlow<List<ChatThread>>(emptyList())
  private val _messages = MutableStateFlow<Map<java.util.UUID, List<Message>>>(emptyMap())

  var shouldFailOnCreateThread = false
  var shouldFailOnSaveMessage = false
  var shouldFailOnDeleteThread = false
  var shouldFailOnArchiveThread = false
  var shouldFailOnGetAllThreads = false

  fun addThread(thread: ChatThread) {
    _threads.value += thread
  }

  fun addMessage(threadId: UUID, message: Message) {
    _messages.value =
      _messages.value.toMutableMap().apply {
        this[threadId] = (this[threadId] ?: emptyList()) + message
      }
  }

  fun clearAll() {
    _threads.value = emptyList()
    _messages.value = emptyMap()
    shouldFailOnCreateThread = false
    shouldFailOnSaveMessage = false
    shouldFailOnDeleteThread = false
    shouldFailOnArchiveThread = false
    shouldFailOnGetAllThreads = false
  }

  override suspend fun getThread(threadId: UUID): ChatThread? =
    _threads.value.firstOrNull { it.threadId == threadId }

  override suspend fun getAllThreads(): List<ChatThread> {
    if (shouldFailOnGetAllThreads) {
      error("Failed to get all threads")
    }
    return _threads.value.filter { !it.isArchived }
  }

  override suspend fun getArchivedThreads(): List<ChatThread> =
    _threads.value.filter { it.isArchived }

  override suspend fun createThread(thread: ChatThread) {
    if (shouldFailOnCreateThread) {
      error("Failed to create thread")
    }
    _threads.value += thread
  }

  override suspend fun updateThread(thread: ChatThread) {
    _threads.value = _threads.value.map { if (it.threadId == thread.threadId) thread else it }
  }

  override suspend fun archiveThread(threadId: UUID) {
    if (shouldFailOnArchiveThread) {
      error("Failed to archive thread")
    }
    _threads.value =
      _threads.value.map { if (it.threadId == threadId) it.copy(isArchived = true) else it }
  }

  override suspend fun deleteThread(threadId: UUID) {
    if (shouldFailOnDeleteThread) {
      error("Failed to delete thread")
    }
    _threads.value = _threads.value.filterNot { it.threadId == threadId }
    _messages.value = _messages.value - threadId
  }

  override suspend fun addMessage(message: Message) {
    if (shouldFailOnSaveMessage) {
      error("Failed to save message")
    }
    addMessage(message.threadId, message)
  }

  override suspend fun getMessages(threadId: UUID): List<Message> =
    _messages.value[threadId] ?: emptyList()

  override fun getMessagesFlow(threadId: UUID): Flow<List<Message>> =
    _messages.map { it[threadId] ?: emptyList() }

  override fun getAllThreadsFlow(): Flow<List<ChatThread>> = _threads

  override suspend fun getCurrentPersonaForThread(threadId: UUID): UUID? =
    getThread(threadId)?.personaId

  override suspend fun createNewThread(personaId: UUID, title: String?): UUID {
    if (shouldFailOnCreateThread) {
      error("Failed to create thread")
    }
    val newThread =
      ChatThread(
        threadId = UUID.randomUUID(),
        personaId = personaId,
        title = title ?: "New Thread",
        activeModelId = "default-model-id",
        isArchived = false,
        createdAt = kotlinx.datetime.Clock.System.now(),
        updatedAt = kotlinx.datetime.Clock.System.now(),
      )
    createThread(newThread)
    return newThread.threadId
  }

  override suspend fun updateThreadPersona(threadId: UUID, personaId: UUID?) {
    val thread = getThread(threadId) ?: return
    updateThread(thread.copy(personaId = personaId))
  }

  override suspend fun saveMessage(message: Message) {
    addMessage(message)
  }
}
