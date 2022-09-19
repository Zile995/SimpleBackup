package com.stefan.simplebackup.ui.adapters

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.stefan.simplebackup.ui.fragments.BaseFragment

class ViewPagerAdapter(
    private val fragmentList: ArrayList<BaseFragment<*>>,
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = fragmentList.size

    override fun createFragment(position: Int): Fragment = fragmentList[position]

    @SuppressLint("NotifyDataSetChanged")
    fun removeFragments() {
        fragmentList.clear()
        notifyDataSetChanged()
    }

    fun addItem(fragment: BaseFragment<*>) {
        fragmentList.add(fragment)
        notifyItemInserted(fragmentList.lastIndex)
    }
}