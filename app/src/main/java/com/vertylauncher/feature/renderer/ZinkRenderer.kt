package com.vertylauncher.feature.renderer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ZinkRenderer : RendererPlugin {
    override val name = "Zink (Vulkan)"
    override val id = "zink"
    override val supportedGpuVendors = listOf(
        RendererPlugin.GpuVendor.ADRENO,
        RendererPlugin.GpuVendor.MALI
    )

    override suspend fun initialize(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            getNativeLibsDir(context).mkdirs()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getNativeLibsDir(context: Context): File = File(context.filesDir, "renderers/zink/lib")
    override fun getEnvironmentVariables(): Map<String, String> = mapOf(
        "MESA_LOADER_DRIVER_OVERRIDE" to "zink",
        "GALLIUM_DRIVER" to "zink",
        "ZINK_DESCRIPTORS" to "lazy",
        "MESA_GL_VERSION_OVERRIDE" to "4.6",
        "MESA_GLSL_VERSION_OVERRIDE" to "460",
        "TU_DEBUG" to "noconform"
    )
    override fun getRequiredPermissions(): List<String> = emptyList()
}
