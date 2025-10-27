package com.vjaykrsna.nanoai

import android.app.Application
import android.content.IntentFilter
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.vjaykrsna.nanoai.core.common.DownloadNotificationReceiver
import com.vjaykrsna.nanoai.core.common.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for nanoAI.
 *
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection throughout the app, including
 * WorkManager workers.
 */
@HiltAndroidApp
class NanoAIApplication : Application(), Configuration.Provider {
  @Inject lateinit var workerFactory: HiltWorkerFactory

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

  override fun onCreate() {
    super.onCreate()
    registerDownloadNotificationReceiver()
  }

  private fun registerDownloadNotificationReceiver() {
    val receiver = DownloadNotificationReceiver()
    val filter =
      IntentFilter().apply {
        addAction(NotificationHelper.ACTION_RESUME_DOWNLOAD)
        addAction(NotificationHelper.ACTION_PAUSE_DOWNLOAD)
        addAction(NotificationHelper.ACTION_CANCEL_DOWNLOAD)
      }
    registerReceiver(receiver, filter, RECEIVER_EXPORTED)
  }
}
