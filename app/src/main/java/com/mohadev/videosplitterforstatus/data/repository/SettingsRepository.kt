package com.mohadev.videosplitterforstatus.data.repository

import com.mohadev.videosplitterforstatus.RemoteConfigManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val remoteConfig: RemoteConfigManager
) {
    fun getUpdateVersion(): Long = remoteConfig.getUpdateVersion()
    fun getUpdateDesc(): String = remoteConfig.getUpdateDesc()
    fun getUpdateLink(): String = remoteConfig.getUpdateLink()
    fun isUpdateForced(): Boolean = remoteConfig.isUpdateForced()
}
