package com.alamincmt.videodownloader.model

sealed class FileDownloadState {
    object Idle : FileDownloadState()
    data class Downloading(val progress: Int) : FileDownloadState()
    data class Failed(val error: Throwable? = null) : FileDownloadState()
    object Downloaded : FileDownloadState()
}
