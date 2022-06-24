package com.stefan.simplebackup.ui.adapters

import android.view.ViewGroup
import com.stefan.simplebackup.databinding.LocalItemBinding

class LocalAdapter(
    selectedItems: MutableList<Int>,
    onSelectionModeCallback: SelectionModeCallBack,
    clickListener: () -> OnClickListener
) : BaseAdapter(selectedItems, onSelectionModeCallback, clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val layoutInflater = getLayoutInflater(parent)
        return LocalViewHolder(
            LocalItemBinding.inflate(layoutInflater, parent, false),
            clickListener
        )
    }
}