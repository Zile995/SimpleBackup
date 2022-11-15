package com.stefan.simplebackup.ui.adapters.viewholders

import com.google.android.material.card.MaterialCardView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.databinding.FavoritesItemBinding
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.utils.extensions.bytesToMegaBytesString
import com.stefan.simplebackup.utils.extensions.isVisible
import com.stefan.simplebackup.utils.extensions.loadBitmap

class FavoritesViewHolder(
    private val binding: FavoritesItemBinding,
    clickListener: OnClickListener
) : BaseViewHolder(binding, clickListener) {

    override val cardView: MaterialCardView = binding.favoritesCardItem

    override fun bind(item: AppData) {
        binding.apply {
            favoritesApplicationImage.loadBitmap(item.bitmap)
            favoritesApplicationName.text = item.name
            favoritesVersionName.text = item.versionName
            favoritesPackageName.text = item.packageName
            favoritesApkSize.text = item.apkSize.bytesToMegaBytesString()
            favoritesInstallDate.text = item.getDateString()
            favoritesSplitApk.isVisible = if (item.isSplit) {
                favoritesSplitApk.text = root.resources.getString(R.string.split)
                true
            } else
                false
        }
    }
}