package com.alamincmt.videodownloader

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.alamincmt.videodownloader.receiver.DownloadReceiver
import com.alamincmt.videodownloader.services.DownloadService
import com.alamincmt.videodownloader.utils.Utils
import com.alamincmt.videodownloader.utils.Variables.isDownloadRunning

class MainActivity : AppCompatActivity() {

    var pbDownloadProgress: ProgressBar? = null
    var tvDownloadPercentage: TextView? = null

    private lateinit var downloadService: DownloadService
    private var isDownloadServiceBound: Boolean = false
    private val downloadServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            downloadService = (binder as DownloadService.DownloadBinder).service
            isDownloadServiceBound = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isDownloadServiceBound = false
        }
    }
    private val POST_NOTIFICATION_PERM_ID: Int = 200
    lateinit var dlReceiver : DownloadReceiver

    companion object {
        var inst: MainActivity? = null
        fun getInstance(): MainActivity? {
            return inst
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        inst = this

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), POST_NOTIFICATION_PERM_ID)
        }

        pbDownloadProgress = findViewById(R.id.pbDownload)
        tvDownloadPercentage = findViewById(R.id.tvDownloadPercentage)

        dlReceiver = DownloadReceiver()
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(dlReceiver, IntentFilter("DownloadBroadcaster"))
    }

    fun updateDownloadProgress(progress: Int) {
        this@MainActivity.runOnUiThread {
            pbDownloadProgress?.progress = progress
            tvDownloadPercentage?.text = "Downloaded : $progress%"
        }
    }

    fun startDownload(view: View) {
        if(Utils.isInternetAvailable(applicationContext)){
            startService(Intent(this, DownloadService::class.java)
                .putExtra("startDownload", "StartDownload")
                .putExtra("needToShowNotification", false))
            bindService(
                Intent(this, DownloadService::class.java),
                downloadServiceConnection,
                Context.BIND_AUTO_CREATE
            )
        }else{
            Utils.showToastLong(applicationContext, "Please connect to the internet and try again.")
        }
    }

    override fun onStop() {
        super.onStop()

        if(!isDownloadRunning){
            LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(dlReceiver)
            stopService(Intent(this, DownloadService::class.java))
            if (isDownloadServiceBound) {
                unbindService(downloadServiceConnection)
            }
        }else{
            startService(Intent(this, DownloadService::class.java)
                .putExtra("startDownload", "DownloadRunning")
                .putExtra("needToShowNotification", true))
            bindService(
                Intent(this, DownloadService::class.java),
                downloadServiceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            POST_NOTIFICATION_PERM_ID -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // push notification permission granted.
                } else {
                    Utils.showToastLong(applicationContext, "Permission is required to show notification!")
                }
            }
        }
    }
}