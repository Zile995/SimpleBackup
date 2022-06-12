package com.stefan.simplebackup.ui.adapters

import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.databinding.LocalItemBinding
import com.stefan.simplebackup.utils.extensions.bytesToString
import com.stefan.simplebackup.utils.extensions.checkedString
import com.stefan.simplebackup.utils.extensions.loadBitmap

class LocalViewHolder(
    private val binding: LocalItemBinding,
    clickListener: OnClickListener?
) : BaseViewHolder(binding, clickListener) {
    override val cardView = binding.cardRestoreItem

    override fun bind(item: AppData) {
        binding.apply {
            restoreApplicationImage.loadBitmap(item.bitmap)
            restoreApplicationName.text = item.name.checkedString()
            restoreVersionName.text = item.versionName.checkedString()
            restorePackageName.text = item.versionName.checkedString()
            restoreDataSize.text = item.dataSize.bytesToString()
            backupDate.text = item.date
        }
    }
}