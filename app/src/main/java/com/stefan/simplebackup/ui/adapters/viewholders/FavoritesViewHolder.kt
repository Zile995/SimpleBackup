package com.stefan.simplebackup.ui.adapters.viewholders

import android.view.View
import com.google.android.material.card.MaterialCardView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.databinding.FavoritesItemBinding
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.utils.extensions.bytesToString
import com.stefan.simplebackup.utils.extensions.checkedString
import com.stefan.simplebackup.utils.extensions.loadBitmap

class FavoritesViewHolder(
    private val binding: FavoritesItemBinding,
    clickListener: OnClickListener
) : BaseViewHolder(binding, clickListener) {

    override val cardView: MaterialCardView = binding.favoritesCardItem

    override fun bind(item: AppData) {
        binding.apply {
            favoritesApplicationImage.loadBitmap(item.bitmap)
            favoritesApplicationName.text = item.name.checkedString()
            favoritesVersionName.text = item.versionName.checkedString()
            favoritesPackageName.text = item.packageName.checkedString()
            favoritesApkSize.text = item.apkSize.bytesToString()
            if (item.isSplit) {
                favoritesSplitApk.text = binding.root.resources.getString(R.string.split)
                favoritesSplitApk.visibility = View.VISIBLE
            } else
                favoritesSplitApk.visibility = View.GONE
        }
    }
}