package com.stefan.simplebackup.ui.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.ListAdapter
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
import com.stefan.simplebackup.ui.adapters.AppAdapter.Companion.AppDiffCallBack
import com.stefan.simplebackup.utils.main.transformBytesToString

class RestoreAdapter(private val clickListener: OnClickListener) :
    ListAdapter<AppData, RestoreAdapter.RestoreViewHolder>(AppDiffCallBack) {

    /**
     * - Služi da kreiramo View preko kojeg možemo da pristupimo elementima iz liste
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestoreViewHolder {
        return RestoreViewHolder.getViewHolder(parent, clickListener)
    }

    /**
     * - Služi da postavimo parametre
     */
    override fun onBindViewHolder(holder: RestoreViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class RestoreViewHolder private constructor(
        private val view: View,
        private val clickListener: OnClickListener
    ) : RecyclerView.ViewHolder(view) {
        private val cardView: MaterialCardView = view.findViewById(R.id.card_item)
        private val appImage: ImageView = view.findViewById(R.id.restore_application_image)
        private val appVersionName: MaterialTextView = view.findViewById(R.id.restore_version_name)
        private val appName: MaterialTextView = view.findViewById(R.id.restore_application_name)
        private val appPackageName: MaterialTextView = view.findViewById(R.id.restore_package_name)
        private val appDataSize: Chip = view.findViewById(R.id.restore_data_size)
        private val appBackupDate: Chip = view.findViewById(R.id.backup_date)
        val getCardView get() = cardView
        val getContext: Context get() = view.context

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
            appVersionName.text = item.versionName
            appName.text = item.name
            appPackageName.text = item.packageName
            appDataSize.text = item.dataSize.transformBytesToString()
            appBackupDate.text = item.date
        }

        private fun loadBitmapByteArray(byteArray: ByteArray) {
            Glide.with(view.context).apply {
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

        companion object {
            fun getViewHolder(parent: ViewGroup, clickListener: OnClickListener): RestoreViewHolder {
                val layoutView = LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.restore_item, parent, false)
                return RestoreViewHolder(layoutView, clickListener)
            }
        }
    }
}