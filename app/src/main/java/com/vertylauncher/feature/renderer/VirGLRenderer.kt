package com.vertylauncher.feature.renderer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VirGLRenderer : RendererPlugin {
    override val name = "VirGLRenderer"
    override val id = "virgl"
    override val supportedGpuVendors = listOf(
        RendererPlugin.GpuVendor.MALI,
        RendererPlugin.GpuVendor.ADRENO,
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

    override fun getNativeLibsDir(context: Context): File = File(context.filesDir, "renderers/virgl/lib")
    override fun getEnvironmentVariables(): Map<String, String> = mapOf(
        "VIRGL_SERVER_PATH" to "/data/local/tmp/.virgl_test",
        "LIBGL_ES" to "2",
        "LIBGL_ALWAYS_SOFTWARE" to "0",
        "MESA_GL_VERSION_OVERRIDE" to "4.5",
        "MESA_GLSL_VERSION_OVERRIDE" to "450"
    )
    override fun getRequiredPermissions(): List<String> = emptyList()
}
