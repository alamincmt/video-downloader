package com.alamincmt.videodownloader.viewmodel

import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import com.alamincmt.videodownloader.R
import com.alamincmt.videodownloader.data.api.FileDownloadApi
import com.alamincmt.videodownloader.data.network.createRetrofitApi
import com.alamincmt.videodownloader.model.FileDownloadState
import com.alamincmt.videodownloader.utils.Variables.DOWNLOAD_PERCENTAGE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import java.io.File

class DownloadViewModel : ViewModel() {

    private val api: FileDownloadApi = createRetrofitApi()
    var state:MutableStateFlow<FileDownloadState> = MutableStateFlow(FileDownloadState.Idle)

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

    fun onIdleRequested() {
        state.value = FileDownloadState.Idle
    }

    private sealed class DownloadState {
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
                            DOWNLOAD_PERCENTAGE = DownloadState.Downloading(((progressBytes * 100) / totalBytes).toInt()).progress
                            println("dl----progress: ${DownloadState.Downloading(((progressBytes * 100) / totalBytes).toInt())}")
                        }
                    }
                }
                emit(DownloadState.Finished)
            } catch (e: Exception) {
                emit(DownloadState.Failed(e))
            }
        }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()
    }
}
