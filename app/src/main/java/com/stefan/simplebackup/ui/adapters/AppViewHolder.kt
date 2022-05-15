package com.stefan.simplebackup.ui.adapters

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textview.MaterialTextView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.main.loadBitmap
import com.stefan.simplebackup.utils.main.transformBytesToString

class AppViewHolder(
    private val view: View,
    clickListener: OnClickListener
) : BaseViewHolder(view, clickListener) {
    val context: Context = view.context
    val packageName: MaterialTextView = view.findViewById(R.id.package_name)
    override val cardView: MaterialCardView = view.findViewById(R.id.card_item)
    private val appImage: ImageView = view.findViewById(R.id.application_image)
    private val appName: MaterialTextView = view.findViewById(R.id.application_name)
    private val versionName: MaterialTextView = view.findViewById(R.id.version_name)
    private val apkSize: Chip = view.findViewById(R.id.apk_size)
    private val splitApk: Chip = view.findViewById(R.id.split_apk)

    fun bind(item: AppData) {
        appImage.loadBitmap(item.bitmap)
        appName.text = checkAndSetString(item.name)
        versionName.text = checkAndSetString(item.versionName)
        packageName.text = checkAndSetString(item.packageName)
        apkSize.text = item.apkSize.transformBytesToString()
        if (item.isSplit) {
            splitApk.text = view.resources.getString(R.string.split)
            splitApk.visibility = View.VISIBLE
        } else
            splitApk.visibility = View.GONE
    }

    private fun checkAndSetString(string: String): String {
        return if (string.length > 36) string.substring(0, 36).plus("...") else string
    }
}