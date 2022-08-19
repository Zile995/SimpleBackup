package com.stefan.simplebackup.ui.adapters

import android.view.ViewGroup
import com.stefan.simplebackup.databinding.CloudItemBinding
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.adapters.viewholders.BaseViewHolder
import com.stefan.simplebackup.ui.adapters.viewholders.CloudViewHolder
import com.stefan.simplebackup.utils.extensions.viewBinding

class CloudAdapter(selectedItems: MutableList<String>,
                   onSelectionModeCallback: SelectionModeCallBack,
                   clickListener: () -> OnClickListener
) : BaseAdapter(selectedItems, onSelectionModeCallback, clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder =
        CloudViewHolder(parent.viewBinding(CloudItemBinding::inflate), clickListener())
}