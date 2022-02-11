package com.stefan.simplebackup.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
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
import com.stefan.simplebackup.data.AppData
import com.stefan.simplebackup.ui.activities.BackupActivity
import com.stefan.simplebackup.utils.FileUtil

class AppAdapter(
    private val selectionListener: SelectionListener,
    private val clickListener: OnClickListener
) :
    ListAdapter<AppData, AppAdapter.AppViewHolder>(AppDiffCallBack) {

    object AppDiffCallBack : DiffUtil.ItemCallback<AppData>() {
        override fun areItemsTheSame(oldItem: AppData, newItem: AppData): Boolean {
            return oldItem.getName() == newItem.getName() &&
                    oldItem.getPackageName() == newItem.getPackageName() &&
                    oldItem.getVersionName() == newItem.getVersionName()
        }

        override fun areContentsTheSame(oldItem: AppData, newItem: AppData): Boolean {
            return oldItem == newItem
        }
    }

    private lateinit var appList: MutableList<AppData>

    class AppViewHolder private constructor(private val view: View) :
        RecyclerView.ViewHolder(view) {
        private val cardView: MaterialCardView = view.findViewById(R.id.card_item)
        private val appImage: ImageView = view.findViewById(R.id.application_image)
        private val appName: MaterialTextView = view.findViewById(R.id.application_name)
        private val versionName: MaterialTextView = view.findViewById(R.id.version_name)
        private val packageName: MaterialTextView = view.findViewById(R.id.package_name)
        private val apkSize: Chip = view.findViewById(R.id.apk_size)
        val getCardView get() = cardView
        val getContext: Context get() = view.context

        fun bind(item: AppData) {
            loadBitmapByteArray(item.getBitmap())
            appName.text = item.getName()
            versionName.text = checkAndSetString(item.getVersionName())
            packageName.text = checkAndSetString(item.getPackageName())
            apkSize.text = FileUtil.transformBytesToString(item.getApkSize())
        }

        private fun loadBitmapByteArray(byteArray: ByteArray) {
            Glide.with(getContext).apply {
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

        companion object {
            fun from(parent: ViewGroup): AppViewHolder {
                val layout = LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.list_item, parent, false)
                return AppViewHolder(layout)
            }
        }
    }

    /**
     * - Služi da kreiramo View preko kojeg možemo da pristupimo elementima iz list item-a
     * - Parent parametar, je view grupa za koju će da se zakači list item view kao child view. Parent je RecyclerView.
     * - Layout sadži referencu na child view (list_item) koji je zakačen na parent view (RecyclerView)
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        return AppViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)

        if (selectionListener.getSelectedItems().contains(item)) {
            holder.getCardView.setCardBackgroundColor(holder.getContext.getColor(R.color.cardViewSelected))
        }

        holder.itemView.setOnLongClickListener {
            clickListener.onLongItemViewClick(holder, item)
            true
        }

        holder.itemView.setOnClickListener {
            clickListener.onItemViewClick(holder, item)
        }
    }

    fun setData(list: MutableList<AppData>) {
        appList = list
        submitList(appList)
    }

    fun filter(newText: String) {
        if (newText.length < 2) {
            setData(appList)
            return
        }
        val pattern = newText.lowercase().trim()
        val filteredList = appList.filter { pattern in it.getName().lowercase() }
        submitList(filteredList)
    }
}