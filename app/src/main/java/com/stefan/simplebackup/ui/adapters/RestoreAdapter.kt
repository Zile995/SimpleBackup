package com.stefan.simplebackup.ui.adapters

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
import com.google.android.material.chip.Chip
import com.google.android.material.textview.MaterialTextView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.domain.model.AppData
import com.stefan.simplebackup.ui.adapters.AppAdapter.Companion.AppDiffCallBack
import com.stefan.simplebackup.utils.main.transformBytesToString

class RestoreAdapter :
    ListAdapter<AppData, RestoreAdapter.RestoreViewHolder>(AppDiffCallBack) {

    private lateinit var appList: MutableList<AppData>

    fun setData(list: MutableList<AppData>) {
        appList = list
        submitList(appList)
    }

    class RestoreViewHolder private constructor(private val view: View) :
        RecyclerView.ViewHolder(view) {
        private val textItem: MaterialTextView = view.findViewById(R.id.stored_application_name)
        private val appSize: MaterialTextView = view.findViewById(R.id.app_size_text)
        private val dateText: MaterialTextView = view.findViewById(R.id.date_text)
        private val appImage: ImageView = view.findViewById(R.id.application_image)
        private val chipVersion: Chip = view.findViewById(R.id.chip_version)

        fun bind(item: AppData) {
            loadBitmapByteArray(item.bitmap)
            val charSequenceVersion: CharSequence = "v" + item.versionName

            textItem.text = item.name
            chipVersion.text = charSequenceVersion.toString()
            appSize.text = item.dataSize.transformBytesToString()
            dateText.text = item.date
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
            fun getViewHolder(parent: ViewGroup): RestoreViewHolder {
                val layoutView = LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.restore_item, parent, false)
                return RestoreViewHolder(layoutView)
            }
        }
    }

    /**
     * - Služi da kreiramo View preko kojeg možemo da pristupimo elementima iz liste
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestoreViewHolder {
        return RestoreViewHolder.getViewHolder(parent)
    }

    /**
     * - Služi da postavimo parametre
     */
    override fun onBindViewHolder(holder: RestoreViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
}