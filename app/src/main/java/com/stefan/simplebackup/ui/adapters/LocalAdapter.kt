package com.stefan.simplebackup.ui.adapters

import android.view.ViewGroup
import com.stefan.simplebackup.databinding.LocalItemBinding
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.adapters.viewholders.BaseViewHolder
import com.stefan.simplebackup.ui.adapters.viewholders.LocalViewHolder
import com.stefan.simplebackup.utils.extensions.viewBinding

class LocalAdapter(
    selectedItems: MutableList<String>,
    onSelectionModeCallback: SelectionModeCallBack,
    clickListener: OnClickListener
) : BaseAdapter(selectedItems, onSelectionModeCallback, clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder =
        LocalViewHolder(parent.viewBinding(LocalItemBinding::inflate), clickListener)
}