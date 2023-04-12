package com.alamincmt.videodownloader

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.alamincmt.videodownloader.model.FileDownloadState
import com.alamincmt.videodownloader.services.DownloadService
import com.alamincmt.videodownloader.utils.Utils
import com.alamincmt.videodownloader.utils.Variables.isDownloadRunning
import com.alamincmt.videodownloader.viewmodel.DownloadViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), POST_NOTIFICATION_PERM_ID)
        }

        startService(Intent(this, DownloadService::class.java))
        bindService(
            Intent(this, DownloadService::class.java),
            downloadServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun startDownload(view: View) {
        if(Utils.isInternetAvailable(applicationContext)){

            val viewModel = ViewModelProvider(this)[DownloadViewModel::class.java]
            viewModel.downloadFile()
            lifecycleScope.launchWhenStarted { viewModel.state.collect{
                when(it) {
                    is FileDownloadState.Idle -> {
                        print("Idle Model")
                    }
                    is FileDownloadState.Downloading -> {
                        print("Downloading file: " + it.progress)
                        findViewById<ProgressBar>(R.id.pbDownload).progress = it.progress
                        findViewById<TextView>(R.id.tvDownloadPercentage).text = "Downloaded : ${it.progress}%"
                    }
                    is FileDownloadState.Downloaded -> {
                        print("File Downloaded")
                        findViewById<TextView>(R.id.tvDownloadPercentage).text = "Download Completed!"
                    }
                    else -> {

                    }
                }
            } }
        }else{
            Utils.showToastLong(applicationContext, "Please connect to the internet and try again.")
        }
    }

    override fun onStop() {
        super.onStop()

        if(!isDownloadRunning){
            stopService(Intent(this, DownloadService::class.java))
            if (isDownloadServiceBound) {
                unbindService(downloadServiceConnection)
            }
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