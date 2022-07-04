package com.stefan.simplebackup.ui.viewmodels

import com.stefan.simplebackup.data.receivers.PackageListener

class FavoritesViewModel(
    packageListener: PackageListener,
    shouldControlSpinner: Boolean = false
) : HomeViewModel(packageListener, shouldControlSpinner)