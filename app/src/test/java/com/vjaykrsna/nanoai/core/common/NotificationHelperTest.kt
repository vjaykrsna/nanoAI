package com.vjaykrsna.nanoai.core.common

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationHelperTest {

  private lateinit var context: Context
  private lateinit var notificationHelper: NotificationHelper

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    notificationHelper = NotificationHelper(context)
  }

  @Test
  fun buildProgressNotification_createsNotificationWithCorrectDetails() {
    val modelName = "Test Model"
    val progress = 50
    val taskId = "task-123"
    val modelId = "model-456"

    val notification =
      notificationHelper.buildProgressNotification(modelName, progress, taskId, modelId)

    assertNotNull(notification)
    assertEquals("Downloading $modelName", notification.extras.getString(Notification.EXTRA_TITLE))
    assertEquals("$progress% complete", notification.extras.getString(Notification.EXTRA_TEXT))
    assertEquals(NotificationCompat.PRIORITY_DEFAULT, notification.priority)
    assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
  }

  @Test
  fun buildCompletionNotification_createsNotificationWithCorrectDetails() {
    val modelName = "Test Model"

    val notification = notificationHelper.buildCompletionNotification(modelName)

    assertNotNull(notification)
    assertEquals("Download Complete", notification.extras.getString(Notification.EXTRA_TITLE))
    assertEquals(
      "$modelName downloaded successfully",
      notification.extras.getString(Notification.EXTRA_TEXT),
    )
    assertEquals(NotificationCompat.PRIORITY_DEFAULT, notification.priority)
    assertTrue(notification.flags and Notification.FLAG_AUTO_CANCEL != 0)
  }

  @Test
  fun buildFailureNotification_createsNotificationWithCorrectDetails() {
    val modelName = "Test Model"
    val errorMessage = "Network error"

    val notification = notificationHelper.buildFailureNotification(modelName, errorMessage)

    assertNotNull(notification)
    assertEquals("Download Failed", notification.extras.getString(Notification.EXTRA_TITLE))
    assertEquals(
      "$modelName: $errorMessage",
      notification.extras.getString(Notification.EXTRA_TEXT),
    )
    assertEquals(NotificationCompat.PRIORITY_DEFAULT, notification.priority)
    assertTrue(notification.flags and Notification.FLAG_AUTO_CANCEL != 0)
  }
}
