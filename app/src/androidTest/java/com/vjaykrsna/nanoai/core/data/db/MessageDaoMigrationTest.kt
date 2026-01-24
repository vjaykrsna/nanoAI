package com.vjaykrsna.nanoai.core.data.db

import android.os.Build
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Migration scaffold for encrypting chat message text.
 *
 * This intentionally fails until a migration adds ciphertext/IV columns and backfills existing
 * plaintext rows to encrypted values.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class MessageDaoMigrationTest {

  @get:Rule
  val helper =
    MigrationTestHelper(
      InstrumentationRegistry.getInstrumentation(),
      NanoAIDatabase::class.java,
      listOf(),
      FrameworkSQLiteOpenHelperFactory(),
    )

  @After
  fun tearDown() {
    InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DB)
  }

  @Test
  fun migrate8To9_encryptsExistingMessages() {
    helper.createDatabase(TEST_DB, 8).apply {
      execSQL(
        """
        INSERT INTO messages (
          message_id,
          thread_id,
          role,
          text,
          audio_uri,
          image_uri,
          source,
          latency_ms,
          created_at,
          error_code
        ) VALUES (
          'm1',
          't1',
          'USER',
          'plaintext body',
          NULL,
          NULL,
          'LOCAL_MODEL',
          NULL,
          1700000000000,
          NULL
        )
        """
          .trimIndent()
      )
      close()
    }

    helper.runMigrationsAndValidate(TEST_DB, 9, true, *NanoAIDatabaseMigrations.ALL).use { db ->
      db.query("PRAGMA table_info(messages)").use { cursor ->
        val columnNames = mutableSetOf<String>()
        while (cursor.moveToNext()) {
          columnNames.add(cursor.getString(cursor.getColumnIndex("name")))
        }
        assertThat(columnNames).containsAtLeast("ciphertext", "iv", "encryption_version")
        assertThat(columnNames).doesNotContain("text")
      }

      db.query("SELECT ciphertext, iv, encryption_version FROM messages WHERE message_id = 'm1'")
        .use { cursor ->
          assertThat(cursor.count).isEqualTo(1)
          cursor.moveToFirst()
          assertThat(cursor.getString(0)).isNotEmpty()
          assertThat(cursor.getString(0)).isNotEqualTo("plaintext body")
          assertThat(cursor.getString(1)).isNotEmpty()
          assertThat(cursor.getInt(2)).isEqualTo(1)
        }
    }
  }

  private companion object {
    const val TEST_DB = "message-migration-test.db"
  }
}
