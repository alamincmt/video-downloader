package com.alamincmt.videodownloader.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alamincmt.videodownloader.MainActivity

class DownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val downloadProgress = intent?.getIntExtra("dl_progress", 0) ?: return
        if (downloadProgress > -1) {
            try {
                MainActivity.getInstance()?.updateDownloadProgress(downloadProgress)
            } catch (e: Exception) {
                print("Error occurred "+ e.message)
            }
        }else{
            try {
                MainActivity.getInstance()?.updateDownloadProgress(0)
            } catch (e: Exception) {
                print("Error occurred "+ e.message)
            }
        }
    }
}