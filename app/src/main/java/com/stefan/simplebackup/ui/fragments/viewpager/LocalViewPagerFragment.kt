package com.stefan.simplebackup.ui.fragments.viewpager

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.stefan.simplebackup.MainApplication
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppDataType
import com.stefan.simplebackup.databinding.FragmentLocalViewPagerBinding
import com.stefan.simplebackup.ui.fragments.BaseFragment
import com.stefan.simplebackup.ui.fragments.FavoritesFragment
import com.stefan.simplebackup.ui.fragments.LocalFragment
import com.stefan.simplebackup.ui.viewmodels.LocalViewModel
import com.stefan.simplebackup.ui.viewmodels.ViewModelFactory

class LocalViewPagerFragment : BaseViewPagerFragment<FragmentLocalViewPagerBinding>() {
    private val localViewModel: LocalViewModel by viewModels {
        ViewModelFactory(
            requireActivity().application as MainApplication,
            mainViewModel.repository
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        localViewModel
    }

    override fun createFragments(): ArrayList<BaseFragment<*>> =
        arrayListOf(LocalFragment(), FavoritesFragment.newInstance(AppDataType.LOCAL))

    override fun configureTabText(): ArrayList<String> =
        arrayListOf(
            requireContext().applicationContext.getString(R.string.backups),
            requireContext().applicationContext.getString(R.string.favorites)
        )
}