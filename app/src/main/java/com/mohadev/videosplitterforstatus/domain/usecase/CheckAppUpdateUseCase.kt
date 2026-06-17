package com.mohadev.videosplitterforstatus.domain.usecase

import com.mohadev.videosplitterforstatus.data.repository.SettingsRepository
import javax.inject.Inject

class CheckAppUpdateUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    fun getUpdateVersion() = repository.getUpdateVersion()
    fun getUpdateDesc() = repository.getUpdateDesc()
    fun getUpdateLink() = repository.getUpdateLink()
    fun isUpdateForced() = repository.isUpdateForced()
}
