package com.stefan.simplebackup.ui.adapters

import android.view.ViewGroup
import com.stefan.simplebackup.databinding.HomeItemBinding
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.adapters.viewholders.BaseViewHolder
import com.stefan.simplebackup.ui.adapters.viewholders.HomeViewHolder
import com.stefan.simplebackup.utils.extensions.viewBinding

class HomeAdapter(
    selectedItems: MutableList<Int>,
    onSelectionModeCallback: SelectionModeCallBack,
    clickListener: () -> OnClickListener
) : BaseAdapter(selectedItems, onSelectionModeCallback, clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder =
        HomeViewHolder(parent.viewBinding(HomeItemBinding::inflate), clickListener)
}