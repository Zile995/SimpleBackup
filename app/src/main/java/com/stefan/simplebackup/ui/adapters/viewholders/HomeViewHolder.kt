package com.stefan.simplebackup.ui.adapters.viewholders

import android.view.View
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.databinding.HomeItemBinding
import com.stefan.simplebackup.ui.adapters.selection.OnClickListener
import com.stefan.simplebackup.utils.extensions.bytesToString
import com.stefan.simplebackup.utils.extensions.checkedString
import com.stefan.simplebackup.utils.extensions.loadBitmap

class HomeViewHolder(
    private val binding: HomeItemBinding,
    clickListener: () -> OnClickListener
) : BaseViewHolder(binding, clickListener) {

    override val cardView = binding.cardItem

    override fun bind(item: AppData) {
        binding.apply {
            applicationImage.loadBitmap(item.bitmap)
            applicationName.text = item.name.checkedString()
            versionName.text = item.versionName.checkedString()
            packageName.text = item.packageName.checkedString()
            apkSize.text = item.apkSize.bytesToString()
            if (item.isSplit) {
                splitApk.text = binding.root.resources.getString(R.string.split)
                splitApk.visibility = View.VISIBLE
            } else
                splitApk.visibility = View.GONE
        }
    }
}