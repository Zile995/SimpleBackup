package com.stefan.simplebackup.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.data.local.repository.AppRepository
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.data.receivers.PackageListener

@Suppress("UNCHECKED_CAST")

// FIXME: Ugly code, should find better way of creating the ViewModels
class ViewModelFactory(
    private val application: MainApplication,
    private val additionalProperty: Any? = null
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            MainViewModel::class.java -> MainViewModel(application)
            HomeViewModel::class.java -> HomeViewModel(
                additionalProperty as PackageListener
            )
            FavoritesViewModel::class.java -> FavoritesViewModel(
                additionalProperty as PackageListener
            )
            LocalViewModel::class.java -> LocalViewModel(
                application,
                additionalProperty as AppRepository
            )
            ProgressViewModel::class.java -> ProgressViewModel(
                additionalProperty as? IntArray?,
                application
            )
            DetailsViewModel::class.java -> DetailsViewModel(
                additionalProperty as? AppData?,
                application
            )
            ProgressViewModel::class.java -> ProgressViewModel(
                additionalProperty as? IntArray?,
                application
            )
            else -> modelClass.newInstance()
        } as T
    }
}