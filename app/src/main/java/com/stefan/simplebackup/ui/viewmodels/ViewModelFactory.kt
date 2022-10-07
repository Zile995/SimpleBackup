package com.stefan.simplebackup.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.receivers.PackageListener
import com.stefan.simplebackup.data.model.AppDataType

@Suppress("UNCHECKED_CAST")

// FIXME: Ugly code, should find better way of creating the ViewModels
class ViewModelFactory(
    private val application: MainApplication,
    private val additionalProperty: Any? = null,
    private val secondProperty: Any? = null
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            MainViewModel::class.java -> MainViewModel(application)
            HomeViewModel::class.java -> HomeViewModel(
                additionalProperty as PackageListener
            )
            FavoritesViewModel::class.java -> FavoritesViewModel(
                additionalProperty as AppRepository,
                secondProperty as AppDataType?
            )
            LocalViewModel::class.java -> LocalViewModel(
                application
            )
            ProgressViewModel::class.java -> ProgressViewModel(
                additionalProperty as? Array<String>?,
                secondProperty as AppDataType?,
                application
            )
            DetailsViewModel::class.java -> DetailsViewModel(
                additionalProperty as? AppData?,
                application
            )
            SearchViewModel::class.java -> SearchViewModel()
            SettingsViewModel::class.java -> SettingsViewModel(application)
            else -> modelClass.newInstance()
        } as T
    }
}