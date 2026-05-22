package com.vertylauncher.feature.download

data class DownloadProgress(
    val downloaded: Long,
    val total: Long,
    val percentage: Int
) {
    companion object {
        val ZERO = DownloadProgress(0, 0, 0)
    }
}
