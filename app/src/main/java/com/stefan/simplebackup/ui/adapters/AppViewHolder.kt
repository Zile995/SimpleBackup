package com.stefan.simplebackup.ui.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textview.MaterialTextView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.utils.main.transformBytesToString

class AppViewHolder private constructor(
    private val view: View,
    private val clickListener: OnClickListener
) :
    RecyclerView.ViewHolder(view) {
    val cardView: MaterialCardView = view.findViewById(R.id.card_item)
    val appImage: ImageView = view.findViewById(R.id.application_image)
    val appName: MaterialTextView = view.findViewById(R.id.application_name)
    val versionName: MaterialTextView = view.findViewById(R.id.version_name)
    val packageName: MaterialTextView = view.findViewById(R.id.package_name)
    val apkSize: Chip = view.findViewById(R.id.apk_size)
    val splitApk: Chip = view.findViewById(R.id.split_apk)
    val context: Context = view.context

    init {
        view.setOnLongClickListener {
            clickListener.onLongItemViewClick(this, adapterPosition)
            true
        }

        view.setOnClickListener {
            clickListener.onItemViewClick(this, adapterPosition)
        }
    }

    fun bind(item: AppData) {
        loadBitmapByteArray(item.bitmap)
        appName.text = item.name
        versionName.text = checkAndSetString(item.versionName)
        packageName.text = checkAndSetString(item.packageName)
        apkSize.text = item.apkSize.transformBytesToString()
        splitApk.text = if (item.split)
            view.resources.getString(R.string.split) else
            view.resources.getString(R.string.non_split)
    }

    private fun loadBitmapByteArray(byteArray: ByteArray) {
        Glide.with(context).apply {
            asBitmap()
                .load(byteArray)
                .placeholder(R.drawable.glide_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .dontAnimate()
                .into(object : CustomTarget<Bitmap?>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap?>?
                    ) {
                        appImage.setImageBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        appImage.setImageDrawable(placeholder)
                    }
                })
        }
    }

    private fun checkAndSetString(string: String): String {
        return if (string.length > 36) string.substring(0, 36).plus("...") else string
    }

    companion object ViewHolderHelper {
        fun getViewHolder(parent: ViewGroup, clickListener: OnClickListener): AppViewHolder {
            val layoutView = LayoutInflater
                .from(parent.context)
                .inflate(R.layout.list_item, parent, false)
            return AppViewHolder(layoutView, clickListener)
        }
    }
}