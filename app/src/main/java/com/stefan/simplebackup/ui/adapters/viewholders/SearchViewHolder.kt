package com.stefan.simplebackup.ui.adapters.viewholders

import com.google.android.material.card.MaterialCardView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.databinding.SearchItemBinding
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.utils.extensions.bytesToMegaBytesString
import com.stefan.simplebackup.utils.extensions.isVisible
import com.stefan.simplebackup.utils.extensions.loadBitmap

class SearchViewHolder(
    private val binding: SearchItemBinding,
    clickListener: OnClickListener
) : BaseViewHolder(binding, clickListener) {

    override val cardView: MaterialCardView = binding.cardItem

    override fun bind(item: AppData) {
        binding.apply {
            applicationImage.loadBitmap(item.bitmap)
            applicationName.text = item.name
            versionName.text = item.versionName
            packageName.text = item.packageName
            apkSize.text = item.apkSize.bytesToMegaBytesString()
            splitApk.isVisible = if (item.isSplit) {
                splitApk.text = root.resources.getString(R.string.split)
                true
            } else
                false
            favoritesBadge.isVisible = item.favorite
        }
    }
}