package com.vjaykrsna.nanoai.core.security

import com.vjaykrsna.nanoai.core.security.model.CredentialScope
import com.vjaykrsna.nanoai.core.security.model.SecretCredential
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CredentialStoreTest {

  @MockK(relaxed = true) private lateinit var encryptedSecretStore: EncryptedSecretStore

  private lateinit var credentialStore: ProviderCredentialStore

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this)
    credentialStore = ProviderCredentialStore(encryptedSecretStore)
  }

  @Test
  fun `save creates new credential id when missing`() {
    val capturedId = slot<String>()

    every {
      encryptedSecretStore.saveCredential(
        providerId = capture(capturedId),
        encryptedValue = any(),
        scope = CredentialScope.TEXT_INFERENCE,
        metadata = any(),
        rotatesAfter = null,
      )
    } answers { call ->
      SecretCredential(
        providerId = capturedId.captured,
        encryptedValue = call.invocation.args[1] as String,
        keyAlias = "nanoai.encrypted.master",
        storedAt = Instant.DISTANT_PAST,
        rotatesAfter = null,
        scope = CredentialScope.TEXT_INFERENCE,
        metadata = mapOf("providerId" to "provider-1"),
      )
    }

    val id =
      credentialStore.save(
        providerId = "provider-1",
        credentialValue = "token",
        existingCredentialId = null,
      )

    assertTrue(id.startsWith("provider-provider-1-"))
    verify {
      encryptedSecretStore.saveCredential(
        providerId = id,
        encryptedValue = "token",
        scope = CredentialScope.TEXT_INFERENCE,
        metadata = mapOf("providerId" to "provider-1"),
        rotatesAfter = null,
      )
    }
  }

  @Test
  fun `save reuses existing credential id`() {
    every { encryptedSecretStore.saveCredential(any(), any(), any(), any(), any()) } returns
      SecretCredential(
        providerId = "existing-id",
        encryptedValue = "token",
        keyAlias = "nanoai.encrypted.master",
        storedAt = Instant.DISTANT_PAST,
        rotatesAfter = null,
        scope = CredentialScope.TEXT_INFERENCE,
        metadata = emptyMap(),
      )

    val id =
      credentialStore.save(
        providerId = "provider-2",
        credentialValue = "token",
        existingCredentialId = "existing-id",
      )

    assertEquals("existing-id", id)
    verify {
      encryptedSecretStore.saveCredential(
        providerId = "existing-id",
        encryptedValue = "token",
        scope = CredentialScope.TEXT_INFERENCE,
        metadata = mapOf("providerId" to "provider-2"),
        rotatesAfter = null,
      )
    }
  }

  @Test
  fun `resolve returns credential value when present`() {
    every { encryptedSecretStore.getCredential("cred-1") } returns
      SecretCredential(
        providerId = "cred-1",
        encryptedValue = "ciphertext",
        keyAlias = "nanoai.encrypted.master",
        storedAt = Instant.fromEpochMilliseconds(1_000L),
        rotatesAfter = null,
        scope = CredentialScope.TEXT_INFERENCE,
        metadata = emptyMap(),
      )

    val value = credentialStore.resolve("cred-1")

    assertEquals("ciphertext", value)
  }

  @Test
  fun `resolve returns null when id missing`() {
    val value = credentialStore.resolve(null)

    assertNull(value)
    verify(exactly = 0) { encryptedSecretStore.getCredential(any()) }
  }

  @Test
  fun `delete removes credential when id provided`() {
    credentialStore.delete("cred-1")

    verify { encryptedSecretStore.deleteCredential("cred-1") }
  }

  @Test
  fun `delete no-ops when id missing`() {
    credentialStore.delete(null)

    verify(exactly = 0) { encryptedSecretStore.deleteCredential(any()) }
  }
}
