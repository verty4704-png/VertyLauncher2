package com.vertylauncher.feature.renderer

import android.content.Context
import java.io.File

interface RendererPlugin {
    val name: String
    val id: String
    val supportedGpuVendors: List<GpuVendor>

    suspend fun initialize(context: Context): Result<Unit>
    fun getNativeLibsDir(context: Context): File
    fun getEnvironmentVariables(): Map<String, String>
    fun getRequiredPermissions(): List<String>

    enum class GpuVendor {
        ADRENO, MALI, POWERVR, EXYNOS, UNKNOWN
    }

    companion object {
        fun detectGpuVendor(): GpuVendor {
            val renderer = android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_RENDERER) ?: ""
            return when {
                renderer.contains("Adreno", ignoreCase = true) -> GpuVendor.ADRENO
                renderer.contains("Mali", ignoreCase = true) -> GpuVendor.MALI
                renderer.contains("PowerVR", ignoreCase = true) -> GpuVendor.POWERVR
                renderer.contains("Exynos", ignoreCase = true) -> GpuVendor.EXYNOS
                else -> GpuVendor.UNKNOWN
            }
        }
    }
}
