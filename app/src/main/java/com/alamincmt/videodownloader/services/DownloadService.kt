package com.alamincmt.videodownloader.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.alamincmt.videodownloader.R
import com.alamincmt.videodownloader.utils.Variables
import java.util.*

class DownloadService : Service() {

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

        createChannel(applicationContext, channelName = "Video Downloader Channel", channelDescription = "This is description", importanceLevel = NotificationCompat.PRIORITY_HIGH)
        createNotification(applicationContext, "Video file is download", "Download Progress: ${Variables.DOWNLOAD_PERCENTAGE}%")

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        println("Service onDestroy Called")
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

    fun createNotification(context: Context, title: String, content: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        val notification = builder.setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(100, 0, false)
            .build()

//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
//            NotificationManagerCompat.from(context).notify(id, notification)
//        }

        startForeground(id, notification)
    }
}