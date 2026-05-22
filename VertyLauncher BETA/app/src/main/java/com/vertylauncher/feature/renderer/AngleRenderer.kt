package com.vertylauncher.feature.renderer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AngleRenderer : RendererPlugin {
    override val name = "ANGLE (Vulkan)"
    override val id = "angle"
    override val supportedGpuVendors = listOf(
        RendererPlugin.GpuVendor.ADRENO,
        RendererPlugin.GpuVendor.MALI,
        RendererPlugin.GpuVendor.UNKNOWN
    )

    override suspend fun initialize(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            getNativeLibsDir(context).mkdirs()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getNativeLibsDir(context: Context): File = File(context.filesDir, "renderers/angle/lib")
    override fun getEnvironmentVariables(): Map<String, String> = mapOf(
        "ANGLE_DEFAULT_PLATFORM" to "vulkan",
        "ANGLE_FEATURE_OVERRIDES_ENABLED" to "preferLinearFilterForYUV"
    )
    override fun getRequiredPermissions(): List<String> = emptyList()
}
