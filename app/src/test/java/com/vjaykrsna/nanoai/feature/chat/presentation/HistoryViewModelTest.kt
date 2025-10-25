@file:Suppress("LargeClass")

package com.vjaykrsna.nanoai.feature.chat.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.vjaykrsna.nanoai.testing.DomainTestBuilders
import com.vjaykrsna.nanoai.testing.FakeConversationRepository
import com.vjaykrsna.nanoai.testing.MainDispatcherExtension
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * Unit tests for [HistoryViewModel].
 *
 * Covers loading states, thread operations, and error handling.
 */
class HistoryViewModelTest {

  @JvmField @RegisterExtension val mainDispatcherExtension = MainDispatcherExtension()

  private lateinit var conversationRepository: FakeConversationRepository
  private lateinit var viewModel: HistoryViewModel

  @BeforeEach
  fun setup() {
    conversationRepository = FakeConversationRepository()
    viewModel = HistoryViewModel(conversationRepository, mainDispatcherExtension.dispatcher)
  }

  @Test
  fun `init loads threads automatically`() = runTest {
    val thread =
      DomainTestBuilders.buildChatThread(threadId = UUID.randomUUID(), title = "Test Thread")
    conversationRepository.addThread(thread)

    // Wait for init to complete
    viewModel.threads.test {
      val threads = awaitItem()
      assertThat(threads).hasSize(1)
      assertThat(threads.first().title).isEqualTo("Test Thread")
    }
  }

  @Test
  fun `loadThreads sets loading state during operation`() = runTest {
    viewModel.isLoading.test {
      assertThat(awaitItem()).isFalse() // Initial state

      viewModel.loadThreads()
      assertThat(awaitItem()).isTrue() // Loading starts
      assertThat(awaitItem()).isFalse() // Loading ends
    }
  }

  @Test
  fun `loadThreads updates threads on success`() = runTest {
    val thread =
      DomainTestBuilders.buildChatThread(threadId = UUID.randomUUID(), title = "Loaded Thread")
    conversationRepository.addThread(thread)

    viewModel.loadThreads()

    viewModel.threads.test {
      val threads = awaitItem()
      assertThat(threads).hasSize(1)
      assertThat(threads.first().title).isEqualTo("Loaded Thread")
    }
  }

  @Test
  fun `loadThreads emits LoadFailed error on failure`() = runTest {
    conversationRepository.shouldFailOnGetAllThreads = true

    viewModel.errors.test {
      viewModel.loadThreads()

      val error = awaitItem()
      assertThat(error).isInstanceOf(HistoryError.LoadFailed::class.java)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `archiveThread reloads threads on success`() = runTest {
    val threadId = UUID.randomUUID()
    val thread =
      DomainTestBuilders.buildChatThread(
        threadId = threadId,
        title = "To Archive",
        isArchived = false,
      )
    conversationRepository.addThread(thread)

    viewModel.archiveThread(threadId)

    // Should reload and show archived threads or not
    // Assuming getAllThreads returns all, including archived
    viewModel.threads.test {
      val threads = awaitItem()
      assertThat(threads).hasSize(1)
      assertThat(threads.first().isArchived).isTrue()
    }
  }

  @Test
  fun `archiveThread emits ArchiveFailed error on failure`() = runTest {
    val threadId = UUID.randomUUID()
    conversationRepository.shouldFailOnArchiveThread = true

    viewModel.errors.test {
      viewModel.archiveThread(threadId)

      val error = awaitItem()
      assertThat(error).isInstanceOf(HistoryError.ArchiveFailed::class.java)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `deleteThread reloads threads on success`() = runTest {
    val threadId = UUID.randomUUID()
    val thread =
      DomainTestBuilders.buildChatThread(
        threadId = threadId,
        title = "To Delete",
        isArchived = false,
      )
    conversationRepository.addThread(thread)

    viewModel.deleteThread(threadId)

    viewModel.threads.test {
      val threads = awaitItem()
      assertThat(threads).isEmpty()
    }
  }

  @Test
  fun `deleteThread emits DeleteFailed error on failure`() = runTest {
    val threadId = UUID.randomUUID()
    conversationRepository.shouldFailOnDeleteThread = true

    viewModel.errors.test {
      viewModel.deleteThread(threadId)

      val error = awaitItem()
      assertThat(error).isInstanceOf(HistoryError.DeleteFailed::class.java)
      cancelAndIgnoreRemainingEvents()
    }
  }
}
