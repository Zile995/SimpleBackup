package com.stefan.simplebackup.ui.adapters.viewholders

import android.view.View
import com.google.android.material.card.MaterialCardView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.databinding.FavoritesItemBinding
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.utils.extensions.bytesToMegaBytesString
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
            if (item.isSplit) {
                favoritesSplitApk.text = binding.root.resources.getString(R.string.split)
                favoritesSplitApk.visibility = View.VISIBLE
            } else
                favoritesSplitApk.visibility = View.GONE
        }
    }
}