package com.alamincmt.videodownloader.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.alamincmt.videodownloader.MainActivity
import com.alamincmt.videodownloader.R
import com.alamincmt.videodownloader.data.api.FileDownloadApi
import com.alamincmt.videodownloader.data.network.createRetrofitApi
import com.alamincmt.videodownloader.model.FileDownloadState
import com.alamincmt.videodownloader.utils.Variables
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import java.io.File
import java.util.*


class DownloadService : Service() {

    private val api: FileDownloadApi = createRetrofitApi()
    var state: MutableStateFlow<FileDownloadState> = MutableStateFlow(FileDownloadState.Idle)

    private var id: Int = UUID.randomUUID().hashCode()
    private var CHANNEL_ID: String = "com.alamincmt.videodownloader.ANDROID_CHANNEL"
    private val binder: Binder = DownloadBinder()

    override fun onBind(p0: Intent?): IBinder = binder

    inner class DownloadBinder : Binder() {
        val service: DownloadService
            get() = this@DownloadService
    }

    override fun onCreate() {
        super.onCreate()
        println("Service onCreate Called")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("Service onStartCommand Called")

        if(intent?.extras?.getString("startDownload").equals("StartDownload")){
            downloadFile()
            createChannel(applicationContext, channelName = "Video Downloader Channel", channelDescription = "This is description", importanceLevel = NotificationCompat.PRIORITY_HIGH)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        println("Service onDestroy Called")
    }

    fun downloadFile() {
        GlobalScope.launch(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            api.downloadVideoFile()
                .saveFile(timestamp.toString())
                .collect { downloadState ->
                    when (downloadState) {
                        is DownloadState.Downloading -> {
                            state.value = FileDownloadState.Downloading(progress = downloadState.progress)
                        }
                        is DownloadState.Failed -> {
                            state.value = FileDownloadState.Failed(error = downloadState.error)
                        }
                        DownloadState.Finished -> {
                            state.value = FileDownloadState.Downloaded
                        }
                    }
                }
        }
    }

    sealed class DownloadState {
        data class Downloading(val progress: Int) : DownloadState()
        object Finished : DownloadState()
        data class Failed(val error: Throwable? = null) : DownloadState()
    }

    private fun ResponseBody.saveFile(filePostfix: String): Flow<DownloadState> {
        return flow {
            emit(DownloadState.Downloading(0))

            val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destinationFile = File(downloadFolder.absolutePath, "video_${filePostfix}.mp4")

            try {
                byteStream().use { inputStream ->
                    destinationFile.outputStream().use { outputStream ->
                        val totalBytes = contentLength()
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var progressBytes = 0L

                        var bytes = inputStream.read(buffer)
                        while (bytes >= 0) {
                            outputStream.write(buffer, 0, bytes)
                            progressBytes += bytes
                            bytes = inputStream.read(buffer)
                            emit(DownloadState.Downloading(((progressBytes * 100) / totalBytes).toInt()))
                            Variables.DOWNLOAD_PERCENTAGE = DownloadState.Downloading(((progressBytes * 100) / totalBytes).toInt()).progress
                            Variables.isDownloadRunning = true
                            createNotification(applicationContext, "Video file downloading...${Variables.DOWNLOAD_PERCENTAGE}%", "Dow", Variables.DOWNLOAD_PERCENTAGE)
                            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(Intent("DownloadBroadcaster").putExtra("dl_progress", Variables.DOWNLOAD_PERCENTAGE))
                            println("dl----progress: ${DownloadState.Downloading(((progressBytes * 100) / totalBytes).toInt())}")
                        }
                    }
                }

                emit(DownloadState.Finished)
                createNotification(applicationContext, "Video file download completed!", "Download Progress: ${Variables.DOWNLOAD_PERCENTAGE}%", Variables.DOWNLOAD_PERCENTAGE)
                Variables.isDownloadRunning = false
            } catch (e: Exception) {
                emit(DownloadState.Failed(e))
                Variables.isDownloadRunning = false
            }
        }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()
    }

    fun createChannel(
        context: Context,
        channelId: String = CHANNEL_ID,
        channelName: String,
        channelDescription: String,
        importanceLevel: Int = NotificationManager.IMPORTANCE_HIGH
    ) {
        val channel = NotificationChannel(
            channelId,
            channelName,
            importanceLevel
        ).apply {
            description = channelDescription
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun createNotification(context: Context, title: String, content: String, progress: Int) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)
        builder.setContentIntent(pendingIntent)

        val notification = builder.setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(100, progress, false)
            .build()
        startForeground(id, notification)
    }
}