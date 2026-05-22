package com.vertylauncher.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.vertylauncher.feature.auth.AuthManager
import com.vertylauncher.feature.auth.elyby.ElyByAuth
import com.vertylauncher.feature.auth.microsoft.MicrosoftAuth
import com.vertylauncher.feature.controller.SmartControllerConfig
import com.vertylauncher.feature.download.DownloadManager
import com.vertylauncher.feature.game.NativeBridge
import com.vertylauncher.feature.launch.LaunchArgumentsBuilder
import com.vertylauncher.feature.modloader.FabricInstaller
import com.vertylauncher.feature.modloader.ForgeInstaller
import com.vertylauncher.feature.renderer.AngleRenderer
import com.vertylauncher.feature.renderer.RendererPlugin
import com.vertylauncher.feature.renderer.VirGLRenderer
import com.vertylauncher.feature.renderer.ZinkRenderer
import com.vertylauncher.feature.runtime.JavaRuntimeManager
import com.vertylauncher.feature.settings.SettingsRepository
import com.vertylauncher.feature.setup.NativeSetupManager
import com.vertylauncher.feature.version.VersionManager
import com.vertylauncher.ui.screen.HomeViewModel
import com.vertylauncher.ui.screen.SettingsViewModel
import com.vertylauncher.ui.screen.VersionListViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val appModule = module {
    single {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }
    single { DownloadManager(get()) }
    single { VersionManager(androidContext(), get()) }
    single { JavaRuntimeManager(androidContext(), get()) }
    single { NativeSetupManager(androidContext(), get()) }
    single { MicrosoftAuth(androidContext()) }
    single { ElyByAuth(androidContext()) }
    single { AuthManager(androidContext(), get(), get()) }
    single { SettingsRepository(androidContext()) }
    single { SmartControllerConfig(androidContext()) }
    single { LaunchArgumentsBuilder() }
    single { FabricInstaller(androidContext(), get()) }
    single { ForgeInstaller(androidContext(), get()) }

    single<RendererPlugin>(qualifier = org.koin.core.qualifier.named("virgl")) { VirGLRenderer() }
    single<RendererPlugin>(qualifier = org.koin.core.qualifier.named("zink")) { ZinkRenderer() }
    single<RendererPlugin>(qualifier = org.koin.core.qualifier.named("angle")) { AngleRenderer() }

    viewModel { HomeViewModel(get()) }
    viewModel { VersionListViewModel(get(), get()) }
    viewModel { SettingsViewModel(get()) }
}
