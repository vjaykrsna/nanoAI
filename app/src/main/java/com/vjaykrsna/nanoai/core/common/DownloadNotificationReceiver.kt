package com.vjaykrsna.nanoai.core.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.vjaykrsna.nanoai.feature.library.domain.ModelDownloadsAndExportUseCase
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadNotificationReceiver : BroadcastReceiver() {

  @Inject lateinit var modelDownloadsUseCase: ModelDownloadsAndExportUseCase

  override fun onReceive(context: Context, intent: Intent) {
    val taskIdString = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID) ?: return
    val taskId =
      try {
        UUID.fromString(taskIdString)
      } catch (e: IllegalArgumentException) {
        Log.w("DownloadNotificationReceiver", "Invalid task ID format: $taskIdString", e)
        return
      }

    CoroutineScope(Dispatchers.IO).launch {
      when (intent.action) {
        NotificationHelper.ACTION_RESUME_DOWNLOAD -> {
          modelDownloadsUseCase.resumeDownload(taskId)
        }
        NotificationHelper.ACTION_PAUSE_DOWNLOAD -> {
          modelDownloadsUseCase.pauseDownload(taskId)
        }
        NotificationHelper.ACTION_CANCEL_DOWNLOAD -> {
          modelDownloadsUseCase.cancelDownload(taskId)
        }
      }
    }
  }
}
