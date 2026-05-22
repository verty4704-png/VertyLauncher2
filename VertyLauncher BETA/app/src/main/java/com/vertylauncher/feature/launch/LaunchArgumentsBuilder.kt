package com.vertylauncher.feature.launch

import android.os.Build
import java.io.File

class LaunchArgumentsBuilder {

    fun buildJvmArguments(config: LaunchConfig): List<String> {
        val args = mutableListOf<String>()
        args.add(File(config.javaRuntimePath, "bin/java").absolutePath)
        args.add("-Xms${config.memoryAllocation.minHeapMb}M")
        args.add("-Xmx${config.memoryAllocation.maxHeapMb}M")
        config.memoryAllocation.metaspaceMb?.let { args.add("-XX:MaxMetaspaceSize=${it}M") }
        args.add("-XX:+UseG1GC")
        args.add("-XX:+ParallelRefProcEnabled")
        args.add("-XX:MaxGCPauseMillis=200")
        args.add("-XX:+UnlockExperimentalVMOptions")
        args.add("-XX:+AlwaysActAsServerClassMachine")
        args.add("-XX:+AlwaysPreTouch")
        args.add("-XX:+DisableExplicitGC")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) args.add("-XX:+UseContainerSupport")
        args.add("-Djava.library.path=${config.nativeLibsDir.absolutePath}")
        args.add("-Dorg.lwjgl.libname=liblwjgl.so")
        args.add("-Dorg.lwjgl.system.libname=liblwjgl.so")

        when (config.rendererType) {
            LaunchConfig.RendererType.VIRGL -> {
                args.add("-Dorg.lwjgl.opengl.libname=libGL.so.1")
                args.add("-Dglfwstub.windowWidth=${config.screenResolution.width}")
                args.add("-Dglfwstub.windowHeight=${config.screenResolution.height}")
            }
            LaunchConfig.RendererType.ZINK -> {
                args.add("-Dorg.lwjgl.opengl.libname=libOSMesa.so")
                args.add("-DMESA_LOADER_DRIVER_OVERRIDE=zink")
                args.add("-Dmesa_glthread=true")
            }
            LaunchConfig.RendererType.ANGLE -> {
                args.add("-Dorg.lwjgl.opengl.libname=libEGL.so")
                args.add("-Dorg.lwjgl.egl.libname=libEGL.so")
            }
            else -> args.add("-Dorg.lwjgl.opengl.libname=libgl4es_114.so")
        }

        args.add("-cp")
        args.add(config.classPath)
        config.jvmProperties.forEach { (k, v) -> args.add("-D$k=$v") }
        args.addAll(config.jvmArgs)
        args.add(config.mainClass)
        return args
    }

    fun buildFullCommand(config: LaunchConfig): List<String> {
        val command = mutableListOf<String>()
        command.addAll(buildJvmArguments(config))
        command.addAll(config.gameArgs)
        return command
    }
}
