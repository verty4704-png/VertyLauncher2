package com.vertylauncher.feature.download

import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DownloadManager(private val client: HttpClient) {

    suspend fun downloadFile(url: String, destination: File, onProgress: ((downloaded: Long, total: Long) -> Unit)? = null): Result<File> = withContext(Dispatchers.IO) {
        try {
            destination.parentFile?.mkdirs()

            client.prepareGet(url).execute { response ->
                val channel = response.bodyAsChannel()
                val total = response.contentLength() ?: -1L
                var downloaded = 0L

                destination.outputStream().use { out ->
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE)
                        if (packet.isEmpty) break
                        val bytes = packet.readBytes()
                        out.write(bytes)
                        downloaded += bytes.size
                        if (total > 0) {
                            onProgress?.invoke(downloaded, total)
                        }
                    }
                }
            }
            Result.success(destination)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
