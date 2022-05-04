package com.stefan.simplebackup.ui.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.ui.adapters.AppAdapter.Companion.AppDiffCallBack

class RestoreAdapter(private val clickListener: OnClickListener) :
    ListAdapter<AppData, RestoreViewHolder>(AppDiffCallBack) {

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
}