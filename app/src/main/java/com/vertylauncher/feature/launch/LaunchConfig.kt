package com.vertylauncher.feature.launch

import java.io.File

data class LaunchConfig(
    val versionId: String,
    val javaRuntimePath: String,
    val classPath: String,
    val mainClass: String,
    val gameDir: File,
    val assetsDir: File,
    val nativeLibsDir: File,
    val jvmArgs: List<String> = emptyList(),
    val gameArgs: List<String> = emptyList(),
    val rendererType: RendererType = RendererType.ANGLE,
    val memoryAllocation: MemoryAllocation = MemoryAllocation.DEFAULT,
    val screenResolution: ScreenResolution = ScreenResolution.AUTO,
    val jvmProperties: Map<String, String> = emptyMap()
) {
    enum class RendererType { GL4ES, VIRGL, ZINK, ANGLE }

    data class MemoryAllocation(val minHeapMb: Int, val maxHeapMb: Int, val metaspaceMb: Int? = null) {
        companion object {
            val DEFAULT = MemoryAllocation(256, 2048)
            fun auto(deviceRamMb: Int): MemoryAllocation {
                val maxHeap = when {
                    deviceRamMb >= 12288 -> 4096
                    deviceRamMb >= 8192 -> 3072
                    deviceRamMb >= 6144 -> 2048
                    deviceRamMb >= 4096 -> 1536
                    else -> 1024
                }
                return MemoryAllocation(256, maxHeap)
            }
        }
    }

    data class ScreenResolution(val width: Int, val height: Int, val scaleFactor: Float = 1.0f) {
        companion object { val AUTO = ScreenResolution(0, 0, 1.0f) }
    }
}
