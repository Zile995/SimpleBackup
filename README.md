<a href="https://www.gnu.org/licenses/gpl-3.0" alt="License: GPLv3"><img src="https://img.shields.io/badge/License-GPL%20v3-blue.svg"></a>
# [Root] [WIP] SimpleBackup
Simple Backup, backup your android apps.

This application is still under development, but in its current state it can be used to easily backup or restore backup copies of other applications on rooted phones. Please test the application carefully.

The application cannot be used to create SMS or Wi-Fi backups. It does not support other cloud services. For now, there is Google Drive upload functionality. In the future, Google Drive services may be completely removed.

## Current features
  * Delete user apps
  * Google Drive backup upload
  * Control zip compression level
  * Restore apps on rooted phones only
  * Backup user apps, apk (split apks) + data only
  * Track installed, updated and deleted apps or backups.
  
## Libraries
  * Coil
  * Room
  * Libsu
  * Zip4j
  * Lifecycle 
  * WorkManager
  * NavigationUI
  * Facebook Shimmer
  * Android Kotlin Coroutine extensions
  * Google Play Services Auth + Google Drive API

## Screenshots
<p float="left">
  <img src="https://user-images.githubusercontent.com/32335484/221413419-18820c56-8096-4359-8b1e-9d74ae7a2ce9.jpg" width="170" />
  <img src="https://user-images.githubusercontent.com/32335484/221413428-7156c4c5-2669-45ba-943a-89d5e3ea30f2.jpg" width="170" />
  <img src="https://user-images.githubusercontent.com/32335484/221413436-6060ccb9-bcdf-4617-88e3-7e9420cdd9d2.jpg" width="170" />
  <img src="https://user-images.githubusercontent.com/32335484/221413439-2859d21b-35aa-49e8-97be-7f848e2de236.jpg" width="170" />
</p>
<p float="left">
  <img src="https://user-images.githubusercontent.com/32335484/221413445-18a76495-d5ea-4754-9f87-17047d5ad018.jpg" width="170" />
  <img src="https://user-images.githubusercontent.com/32335484/221413450-4299f7f6-776f-40d9-822a-058f80073b5f.jpg" width="170" />
  <img src="https://user-images.githubusercontent.com/32335484/221413453-45ac1e8b-74cb-4dd4-9eb6-6056df181207.jpg" width="170" />
  <img src="https://user-images.githubusercontent.com/32335484/221413455-7510e968-647c-4a83-9b84-563f672dbed0.jpg" width="170" />
</p>
<p float="left">
  <img src="https://user-images.githubusercontent.com/32335484/221413457-f134b01e-c879-415f-afcd-9d3c3124db83.jpg" width="170" />
  <img src="https://user-images.githubusercontent.com/32335484/221413462-ddc14e92-b14b-4392-a214-557304d3e34b.jpg" width="170" />
  <img src="https://user-images.githubusercontent.com/32335484/221413466-6b3d7612-f901-4a97-a9ce-421376dcaf29.jpg" width="170" />
  <img src="https://user-images.githubusercontent.com/32335484/221413971-368d65de-b3b5-4c6b-a0e0-9461f46a6e89.jpg" width="170" />
</p>

## TODO:
  * New app icon
  * Cloud restore
  * Scheduled backups
  * Encrypted backups
  * Better animations
  * Material 3 Bottom sheet
  * Clear cache or app data
  * More reliable Google Drive upload
  * Restore apps without deleting them first
  * Apk install for phones without root access
  * Material 3 theming (also add a light theme)
  * Redesign app details and progress activities
  * Google Sign in: Track multiple Google accounts
  * More per app backup options (obb, user_de, Android/data)
  * Navigation drawer with phone storage info and app settings
  * Settings PreferenceScreen, improve settings fragment performance
  
## Contribution
As an android beginner, help is always welcome! Yes, please contribute if you can. :blush:
