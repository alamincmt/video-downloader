package com.alamincmt.videodownloader.data.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming

interface FileDownloadApi {
    @Streaming
    @GET("storage/fe9278ad7f642dbd39ac5c9/2017/04/file_example_MP4_1920_18MG.mp4")
    suspend fun downloadVideoFile(): ResponseBody
}
