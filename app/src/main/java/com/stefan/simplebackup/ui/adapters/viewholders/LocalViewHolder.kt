package com.stefan.simplebackup.ui.adapters.viewholders

import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.databinding.LocalItemBinding
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.utils.extensions.bytesToMegaBytesString
import com.stefan.simplebackup.utils.extensions.isVisible
import com.stefan.simplebackup.utils.extensions.loadBitmap

class LocalViewHolder(
    private val binding: LocalItemBinding,
    clickListener: OnClickListener
) : BaseViewHolder(binding, clickListener) {

    override val cardView = binding.backupCardItem

    override fun bind(item: AppData) {
        binding.apply {
            backupApplicationImage.loadBitmap(item.bitmap)
            backupApplicationName.text = item.name
            backupVersionName.text = item.versionName
            backupPackageName.text = item.packageName
            backupDataSize.text = item.dataSize.bytesToMegaBytesString()
            backupDate.text = item.getDateText()
            splitApk.isVisible = if (item.isSplit) {
                splitApk.text = root.resources.getString(R.string.split)
                true
            } else
                false
            favoritesBadge.isVisible = item.isFavorite
        }
    }
}