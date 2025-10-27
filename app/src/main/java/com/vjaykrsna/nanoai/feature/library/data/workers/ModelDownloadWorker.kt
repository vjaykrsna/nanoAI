package com.vjaykrsna.nanoai.feature.library.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker.Result as WorkResult
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.vjaykrsna.nanoai.core.common.NanoAIResult
import com.vjaykrsna.nanoai.core.common.NotificationHelper
import com.vjaykrsna.nanoai.core.common.NotificationHelper.Companion.NOTIFICATION_ID_DOWNLOAD_PROGRESS
import com.vjaykrsna.nanoai.feature.library.data.catalog.DownloadManifest
import com.vjaykrsna.nanoai.feature.library.data.catalog.VerificationOutcome
import com.vjaykrsna.nanoai.feature.library.data.daos.DownloadTaskDao
import com.vjaykrsna.nanoai.feature.library.data.daos.ModelPackageReadDao
import com.vjaykrsna.nanoai.feature.library.data.daos.ModelPackageWriteDao
import com.vjaykrsna.nanoai.feature.library.domain.DownloadStatus
import com.vjaykrsna.nanoai.feature.library.domain.InstallState
import com.vjaykrsna.nanoai.feature.library.domain.ModelManifestUseCase
import com.vjaykrsna.nanoai.telemetry.TelemetryReporter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody

private const val DOWNLOAD_BUFFER_SIZE = 8_192
private const val MAX_RETRY_ATTEMPTS = 3
private const val PROGRESS_PERCENTAGE_MULTIPLIER = 100
private const val KEY_TASK_ID = "TASK_ID"
private const val KEY_MODEL_ID = "MODEL_ID"
private const val KEY_FILE_PATH = "FILE_PATH"
private const val KEY_MODEL_VERSION = "MODEL_VERSION"
private const val KEY_CHECKSUM = "CHECKSUM_SHA256"
private const val KEY_ERROR_TYPE = "ERROR_TYPE"
private const val KEY_ERROR_MESSAGE = "ERROR_MESSAGE"
private const val KEY_ERROR_TELEMETRY = "ERROR_TELEMETRY"
private const val KEY_ERROR_RETRY_AFTER = "ERROR_RETRY_AFTER"
private const val KEY_ERROR_CONTEXT = "ERROR_CONTEXT"
private const val ERROR_TYPE_RECOVERABLE = "recoverable"
private const val ERROR_TYPE_FATAL = "fatal"

/**
 * WorkManager worker that downloads model artifacts after fetching signed manifests and enforcing
 * integrity checks.
 */
class ModelDownloadDependencies
@Inject
constructor(
  val downloadTaskDao: DownloadTaskDao,
  val modelPackageReadDao: ModelPackageReadDao,
  val modelPackageWriteDao: ModelPackageWriteDao,
  val okHttpClient: OkHttpClient,
  val modelManifestUseCase: ModelManifestUseCase,
  val telemetryReporter: TelemetryReporter,
  val notificationHelper: NotificationHelper,
)

@HiltWorker
class ModelDownloadWorker
@AssistedInject
constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  private val dependencies: ModelDownloadDependencies,
) : CoroutineWorker(appContext, workerParams) {

  private val downloadTaskDao: DownloadTaskDao = dependencies.downloadTaskDao
  private val modelPackageReadDao: ModelPackageReadDao = dependencies.modelPackageReadDao
  private val modelPackageWriteDao: ModelPackageWriteDao = dependencies.modelPackageWriteDao
  private val okHttpClient: OkHttpClient = dependencies.okHttpClient
  private val modelManifestUseCase: ModelManifestUseCase = dependencies.modelManifestUseCase
  private val telemetryReporter: TelemetryReporter = dependencies.telemetryReporter
  private val notificationHelper: NotificationHelper = dependencies.notificationHelper
  private var modelName: String = ""

  override suspend fun doWork(): WorkResult = withContext(Dispatchers.IO) { executeWork() }

  private suspend fun executeWork(): WorkResult {
    val taskId = inputData.getString(KEY_TASK_ID)
    val modelId = inputData.getString(KEY_MODEL_ID)
    val modelPackage = modelId?.let { modelPackageReadDao.getById(it) }

    return when {
      taskId == null -> fatalFailureResult("Missing task id")
      modelId == null -> fatalFailureResult("Missing model id")
      modelPackage == null -> fatalFailureResult("Model not found: $modelId")
      else -> {
        val modelName = modelId // TODO: Use proper model name from modelPackage
        markTaskStarting(downloadTaskDao, taskId)
        modelPackageWriteDao.updateInstallState(
          modelId,
          InstallState.DOWNLOADING,
          Clock.System.now(),
        )
        val initialNotification =
          notificationHelper.buildProgressNotification(modelName, 0, taskId, modelId)
        setForeground(ForegroundInfo(NOTIFICATION_ID_DOWNLOAD_PROGRESS, initialNotification))

        when (
          val manifestResult = modelManifestUseCase.refreshManifest(modelId, modelPackage.version)
        ) {
          is NanoAIResult.Success -> processManifest(taskId, modelId, manifestResult.value)
          is NanoAIResult.RecoverableError ->
            handleRecoverable(taskId, modelId, manifestResult, modelName)
          is NanoAIResult.FatalError -> handleFatal(taskId, modelId, manifestResult, modelName)
        }
      }
    }
  }

  private suspend fun processManifest(
    taskId: String,
    modelId: String,
    manifest: DownloadManifest,
  ): WorkResult {
    val downloadDir = File(applicationContext.filesDir, "models").apply { mkdirs() }
    val outputFile = File(downloadDir, "${manifest.modelId}.bin")

    return when (val downloadOutcome = downloadModel(taskId, manifest, outputFile)) {
      is NanoAIResult.Success -> {
        val downloadedFile = downloadOutcome.value
        when (val integrityResult = validateIntegrity(downloadedFile, manifest)) {
          is NanoAIResult.Success -> {
            val checksum = integrityResult.value
            reportVerification(modelId, manifest, VerificationOutcome.SUCCESS, null)
            markSuccess(taskId, modelId, manifest, downloadedFile, checksum)
          }
          is NanoAIResult.RecoverableError -> {
            reportVerification(
              modelId,
              manifest,
              VerificationOutcome.CORRUPTED,
              integrityResult.message,
            )
            handleRecoverable(taskId, modelId, integrityResult, modelName)
          }
          is NanoAIResult.FatalError -> {
            reportVerification(
              modelId,
              manifest,
              VerificationOutcome.CORRUPTED,
              integrityResult.message,
            )
            handleFatal(taskId, modelId, integrityResult, modelName)
          }
        }
      }
      is NanoAIResult.RecoverableError ->
        handleRecoverable(taskId, modelId, downloadOutcome, modelName)
      is NanoAIResult.FatalError -> handleFatal(taskId, modelId, downloadOutcome, modelName)
    }
  }

  private suspend fun downloadModel(
    taskId: String,
    manifest: DownloadManifest,
    outputFile: File,
  ): NanoAIResult<File> {
    val request = Request.Builder().url(manifest.downloadUrl).build()
    val response =
      runCatching { okHttpClient.newCall(request).execute() }
        .getOrElse { error ->
          outputFile.delete()
          return NanoAIResult.recoverable(
            message = "Download request failed",
            retryAfterSeconds = 30L,
            cause = error,
            context = mapOf("modelId" to manifest.modelId),
          )
        }

    response.use { resp ->
      val failure =
        when {
          !resp.isSuccessful ->
            recoverableDownloadError(
              message = "Download failed with HTTP ${resp.code}",
              cause = IOException("HTTP ${resp.code}"),
              manifest = manifest,
            )
          resp.body == null ->
            recoverableDownloadError(
              message = "Empty response body",
              cause = IOException("Empty body"),
              manifest = manifest,
            )
          else ->
            resp.body!!.saveToFile(
              outputFile = outputFile,
              manifest = manifest,
              taskId = taskId,
              onProgress = { progress, downloaded, total ->
                downloadTaskDao.updateProgress(taskId, progress, downloaded)
                setProgress(
                  workDataOf(
                    "PROGRESS" to progress,
                    "BYTES_DOWNLOADED" to downloaded,
                    "TOTAL_BYTES" to total,
                  )
                )
              },
            )
        }

      return if (failure != null) {
        outputFile.delete()
        failure
      } else {
        NanoAIResult.success(outputFile)
      }
    }
  }

  private fun recoverableDownloadError(
    message: String,
    cause: Throwable?,
    manifest: DownloadManifest,
  ): NanoAIResult.RecoverableError =
    NanoAIResult.recoverable(
      message = message,
      retryAfterSeconds = 30L,
      cause = cause,
      context = mapOf("modelId" to manifest.modelId),
    )

  private suspend fun ResponseBody.saveToFile(
    outputFile: File,
    manifest: DownloadManifest,
    taskId: String,
    onProgress: suspend (Float, Long, Long) -> Unit,
  ): NanoAIResult.RecoverableError? {
    var status: NanoAIResult.RecoverableError? = null
    byteStream().use { input ->
      FileOutputStream(outputFile).use { output ->
        status = processDownloadStream(input, output, manifest, taskId, onProgress)
      }
    }
    return status
  }

  private suspend fun processDownloadStream(
    input: InputStream,
    output: FileOutputStream,
    manifest: DownloadManifest,
    taskId: String,
    onProgress: suspend (Float, Long, Long) -> Unit,
  ): NanoAIResult.RecoverableError? {
    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
    var downloaded = 0L
    val totalBytes = manifest.sizeBytes
    var status: NanoAIResult.RecoverableError? = null
    var read = input.read(buffer)
    while (status == null && read != -1) {
      if (isStopped) {
        status =
          NanoAIResult.recoverable(
            message = "Download cancelled",
            retryAfterSeconds = null,
            context = mapOf("modelId" to manifest.modelId),
          )
      } else {
        output.write(buffer, 0, read)
        downloaded += read
        val progress = progressFor(downloaded, totalBytes)
        val progressNotification =
          notificationHelper.buildProgressNotification(
            modelName,
            (progress * PROGRESS_PERCENTAGE_MULTIPLIER).toInt(),
            taskId,
            manifest.modelId,
          )
        notificationHelper.notifyProgress(progressNotification)
        onProgress(progress, downloaded, totalBytes)
        read = input.read(buffer)
      }
    }
    return status
  }

  private fun progressFor(downloaded: Long, totalBytes: Long): Float {
    if (totalBytes <= 0) return 0f
    return downloaded.toFloat() / totalBytes.toFloat()
  }

  private suspend fun validateIntegrity(
    file: File,
    manifest: DownloadManifest,
  ): NanoAIResult<String> {
    val fileSize = file.length()
    val sizeValidation =
      if (fileSize == manifest.sizeBytes) null
      else {
        file.delete()
        NanoAIResult.recoverable(
          message = "Size mismatch detected",
          retryAfterSeconds = 30L,
          context =
            mapOf(
              "expectedSize" to manifest.sizeBytes.toString(),
              "actualSize" to fileSize.toString(),
            ),
        )
      }

    val result =
      sizeValidation
        ?: run {
          val checksum = calculateSha256(file)
          when {
            !checksum.equals(manifest.checksumSha256, ignoreCase = true) -> {
              file.delete()
              NanoAIResult.recoverable(
                message = "Checksum mismatch detected",
                retryAfterSeconds = 30L,
                context =
                  mapOf("expectedChecksum" to manifest.checksumSha256, "actualChecksum" to checksum),
              )
            }
            manifest.signature != null &&
              manifest.publicKeyUrl != null &&
              !verifySignature(file, manifest.signature, manifest.publicKeyUrl) -> {
              file.delete()
              NanoAIResult.recoverable(
                message = "Signature verification failed",
                retryAfterSeconds = 30L,
                context = mapOf("modelId" to manifest.modelId),
              )
            }
            else -> NanoAIResult.success(checksum)
          }
        }

    return result
  }

  private suspend fun verifySignature(
    file: File,
    signature: String,
    publicKeyUrl: String,
  ): Boolean =
    withContext(Dispatchers.IO) {
      try {
        val keyBytes = URL(publicKeyUrl).readBytes()
        val spec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey = keyFactory.generatePublic(spec)

        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(publicKey)
        file.inputStream().use { input ->
          val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
          while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            verifier.update(buffer, 0, read)
          }
        }

        verifier.verify(android.util.Base64.decode(signature, android.util.Base64.DEFAULT))
      } catch (ioException: IOException) {
        telemetryReporter.report(
          source = "ModelDownloadWorker.verifySignature",
          result =
            NanoAIResult.recoverable(
              message = "Failed to download verification key",
              cause = ioException,
              context = mapOf("publicKeyUrl" to publicKeyUrl),
            ),
        )
        false
      } catch (securityException: GeneralSecurityException) {
        telemetryReporter.report(
          source = "ModelDownloadWorker.verifySignature",
          result =
            NanoAIResult.recoverable(
              message = "Signature verification failed",
              cause = securityException,
              context = mapOf("modelId" to file.nameWithoutExtension),
            ),
        )
        false
      }
    }

  private suspend fun handleRecoverable(
    taskId: String,
    modelId: String,
    error: NanoAIResult.RecoverableError,
    modelName: String,
  ): WorkResult {
    notificationHelper.cancelProgressNotification()
    val failureNotification = notificationHelper.buildFailureNotification(modelName, error.message)
    notificationHelper.notifyFailure(failureNotification)
    downloadTaskDao.updateStatusWithError(taskId, DownloadStatus.FAILED, error.message)
    modelPackageWriteDao.updateInstallState(modelId, InstallState.ERROR, Clock.System.now())
    markTaskFinished(downloadTaskDao, taskId)
    val willRetry = runAttemptCount < MAX_RETRY_ATTEMPTS - 1
    telemetryReporter.report(
      source = "ModelDownloadWorker",
      result = error,
      extraContext =
        mapOf(
          "taskId" to taskId,
          "modelId" to modelId,
          "attempt" to runAttemptCount.toString(),
          "willRetry" to willRetry.toString(),
        ),
    )
    return if (willRetry) {
      WorkResult.retry()
    } else {
      WorkResult.failure(recoverableResultData(error))
    }
  }

  private suspend fun handleFatal(
    taskId: String,
    modelId: String,
    error: NanoAIResult.FatalError,
    modelName: String,
  ): WorkResult {
    notificationHelper.cancelProgressNotification()
    val failureNotification = notificationHelper.buildFailureNotification(modelName, error.message)
    notificationHelper.notifyFailure(failureNotification)
    downloadTaskDao.updateStatusWithError(taskId, DownloadStatus.FAILED, error.message)
    modelPackageWriteDao.updateInstallState(modelId, InstallState.ERROR, Clock.System.now())
    markTaskFinished(downloadTaskDao, taskId)
    telemetryReporter.report(
      source = "ModelDownloadWorker",
      result = error,
      extraContext =
        mapOf("taskId" to taskId, "modelId" to modelId, "attempt" to runAttemptCount.toString()),
    )
    return WorkResult.failure(fatalResultData(error))
  }

  private suspend fun markSuccess(
    taskId: String,
    modelId: String,
    manifest: DownloadManifest,
    outputFile: File,
    checksum: String,
  ): WorkResult {
    notificationHelper.cancelProgressNotification()
    val completionNotification = notificationHelper.buildCompletionNotification(modelName)
    notificationHelper.notifyCompletion(completionNotification)
    downloadTaskDao.updateStatus(taskId, DownloadStatus.COMPLETED)
    downloadTaskDao.updateProgress(taskId, 1f, manifest.sizeBytes)
    markTaskFinished(downloadTaskDao, taskId)
    modelPackageWriteDao.updateInstallState(modelId, InstallState.INSTALLED, Clock.System.now())

    return WorkResult.success(
      workDataOf(
        KEY_FILE_PATH to outputFile.absolutePath,
        KEY_MODEL_ID to modelId,
        KEY_MODEL_VERSION to manifest.version,
        KEY_CHECKSUM to checksum,
      )
    )
  }

  private suspend fun reportVerification(
    modelId: String,
    manifest: DownloadManifest,
    outcome: VerificationOutcome,
    failureReason: String?,
  ) {
    when (
      val result =
        modelManifestUseCase.reportVerification(
          modelId = modelId,
          version = manifest.version,
          checksumSha256 = manifest.checksumSha256,
          status = outcome,
          failureReason = failureReason,
        )
    ) {
      is NanoAIResult.Success -> Unit
      is NanoAIResult.RecoverableError ->
        telemetryReporter.report(
          source = "ModelDownloadWorker.reportVerification",
          result = result,
          extraContext =
            mapOf("modelId" to modelId, "version" to manifest.version, "outcome" to outcome.name),
        )
      is NanoAIResult.FatalError ->
        telemetryReporter.report(
          source = "ModelDownloadWorker.reportVerification",
          result = result,
          extraContext =
            mapOf("modelId" to modelId, "version" to manifest.version, "outcome" to outcome.name),
        )
    }
  }
}

private fun fatalFailureResult(message: String): WorkResult =
  WorkResult.failure(workDataOf(KEY_ERROR_TYPE to ERROR_TYPE_FATAL, KEY_ERROR_MESSAGE to message))

private suspend fun markTaskStarting(downloadTaskDao: DownloadTaskDao, taskId: String) {
  val task = downloadTaskDao.getById(taskId) ?: return
  downloadTaskDao.update(
    task.copy(startedAt = Clock.System.now(), finishedAt = null, errorMessage = null)
  )
  downloadTaskDao.updateStatus(taskId, DownloadStatus.DOWNLOADING)
}

private suspend fun markTaskFinished(downloadTaskDao: DownloadTaskDao, taskId: String) {
  val task = downloadTaskDao.getById(taskId) ?: return
  downloadTaskDao.update(task.copy(finishedAt = Clock.System.now()))
}

private fun recoverableResultData(error: NanoAIResult.RecoverableError) =
  workDataOf(
    KEY_ERROR_TYPE to ERROR_TYPE_RECOVERABLE,
    KEY_ERROR_MESSAGE to error.message,
    KEY_ERROR_TELEMETRY to error.telemetryId,
    KEY_ERROR_RETRY_AFTER to error.retryAfterSeconds,
    KEY_ERROR_CONTEXT to error.context.entries.joinToString("&") { "${it.key}=${it.value}" },
  )

private fun fatalResultData(error: NanoAIResult.FatalError) =
  workDataOf(
    KEY_ERROR_TYPE to ERROR_TYPE_FATAL,
    KEY_ERROR_MESSAGE to error.message,
    KEY_ERROR_TELEMETRY to error.telemetryId,
  )

private fun calculateSha256(file: File): String {
  val digest = MessageDigest.getInstance("SHA-256")
  file.inputStream().use { input ->
    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
    while (true) {
      val read = input.read(buffer)
      if (read == -1) break
      digest.update(buffer, 0, read)
    }
  }
  return digest.digest().joinToString(separator = "") { byte ->
    String.format(Locale.US, "%02x", byte)
  }
}
