package com.stefan.simplebackup.ui.adapters.viewholders

import android.view.View
import com.google.android.material.card.MaterialCardView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.databinding.CloudItemBinding
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.utils.extensions.bytesToMegaBytesString
import com.stefan.simplebackup.utils.extensions.loadBitmap

class CloudViewHolder(
    private val binding: CloudItemBinding,
    clickListener: OnClickListener
) : BaseViewHolder(binding, clickListener) {

    override val cardView: MaterialCardView = binding.cloudCardItem

    override fun bind(item: AppData) {
        binding.apply {
            cloudApplicationImage.loadBitmap(item.bitmap)
            cloudApplicationName.text = item.name
            cloudVersionName.text = item.versionName
            cloudPackageName.text = item.packageName
            cloudApkSize.text = item.apkSize.bytesToMegaBytesString()
            if (item.isSplit) {
                cloudSplitApk.text = binding.root.resources.getString(R.string.split)
                cloudSplitApk.visibility = View.VISIBLE
            } else
                cloudSplitApk.visibility = View.GONE
        }
    }
}