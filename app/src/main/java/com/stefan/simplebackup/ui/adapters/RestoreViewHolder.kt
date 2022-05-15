package com.stefan.simplebackup.ui.adapters

import android.view.View
import android.widget.ImageView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textview.MaterialTextView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.main.loadBitmap
import com.stefan.simplebackup.utils.main.transformBytesToString

class RestoreViewHolder(
    view: View,
    clickListener: OnClickListener
) : BaseViewHolder(view, clickListener) {
    override val cardView: MaterialCardView = view.findViewById(R.id.card_restore_item)
    private val appImage: ImageView = view.findViewById(R.id.restore_application_image)
    private val appVersionName: MaterialTextView = view.findViewById(R.id.restore_version_name)
    private val appName: MaterialTextView = view.findViewById(R.id.restore_application_name)
    private val appPackageName: MaterialTextView = view.findViewById(R.id.restore_package_name)
    private val appDataSize: Chip = view.findViewById(R.id.restore_data_size)
    private val appBackupDate: Chip = view.findViewById(R.id.backup_date)

    fun bind(item: AppData) {
        appImage.loadBitmap(item.bitmap)
        appVersionName.text = item.versionName
        appName.text = item.name
        appPackageName.text = item.packageName
        appDataSize.text = item.dataSize.transformBytesToString()
        appBackupDate.text = item.date
    }
}