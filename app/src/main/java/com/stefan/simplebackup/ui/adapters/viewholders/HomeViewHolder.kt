package com.stefan.simplebackup.ui.adapters.viewholders

import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.databinding.HomeItemBinding
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.utils.extensions.bytesToString
import com.stefan.simplebackup.utils.extensions.checkedString
import com.stefan.simplebackup.utils.extensions.isVisible
import com.stefan.simplebackup.utils.extensions.loadBitmap

class HomeViewHolder(
    private val binding: HomeItemBinding,
    clickListener: OnClickListener
) : BaseViewHolder(binding, clickListener) {

    override val cardView = binding.cardItem

    override fun bind(item: AppData) {
        binding.apply {
            applicationImage.loadBitmap(item.bitmap)
            applicationName.text = item.name.checkedString()
            versionName.text = item.versionName.checkedString()
            packageName.text = item.packageName.checkedString()
            apkSize.text = item.apkSize.bytesToString()
            splitApk.isVisible = if (item.isSplit) {
                splitApk.text = root.resources.getString(R.string.split)
                true
            } else
                false
            favoritesBadge.isVisible = item.favorite
        }
    }
}