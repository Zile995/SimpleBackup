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
    private val selectionListener: SelectionListener
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
            Glide.with(getContext).apply {
                asBitmap()
                    .load(item.getBitmapByteArray())
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
            appName.text = item.getName()
            versionName.text = item.getVersionName()
            packageName.text = item.getPackageName()
            apkSize.text = FileUtil.transformBytes(item.getApkSize())
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
        val cardView = holder.getCardView
        holder.bind(item)


        if (selectionListener.getSelectedItems().contains(item)) {
            holder.getCardView.setCardBackgroundColor(holder.getContext.getColor(R.color.darkCardViewSelected))
        }

        cardView.setOnLongClickListener {
            selectionListener.setSelectionMode(true)
            doSelection(holder, item)
            true
        }

        cardView.setOnClickListener {
            if (selectionListener.hasSelectedItems()) {
                doSelection(holder, item)
            } else {
                val context = holder.getContext
                val intent = Intent(context, BackupActivity::class.java)
                intent.putExtra("application", item)
                context.startActivity(intent)
            }
        }
    }

    private fun doSelection(holder: AppViewHolder, item: AppData) {
        val selectionList = selectionListener.getSelectedItems()
        val context = holder.getContext
        if (selectionList.contains(item)) {
            selectionListener.removeSelectedItem(item)
            holder.getCardView.toggle()
            holder.getCardView.setCardBackgroundColor(context.getColor(R.color.darkCardView))
        } else {
            selectionListener.addSelectedItem(item)
            holder.getCardView.toggle()
            holder.getCardView.setCardBackgroundColor(context.getColor(R.color.darkCardViewSelected))
        }
        if (selectionList.isEmpty()) {
            selectionListener.setSelectionMode(false)
        }
        println("Listener list: ${selectionListener.getSelectedItems().size}")
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