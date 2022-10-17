package com.stefan.simplebackup.ui.views

import android.animation.AnimatorSet
import android.util.Log
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.doOnPreDraw
import androidx.core.view.marginBottom
import com.stefan.simplebackup.R
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import com.stefan.simplebackup.ui.fragments.viewpager.HomeViewPagerFragment
import com.stefan.simplebackup.utils.extensions.getColorFromResource
import com.stefan.simplebackup.utils.extensions.getVisibleFragment
import java.lang.ref.WeakReference

class MainActivityAnimator(
    private val activityReference: WeakReference<MainActivity>,
    private val bindingReference: WeakReference<ActivityMainBinding>
) {
    private val binding get() = bindingReference.get()
    private val activity get() = activityReference.get()

    private val animationDuration = 250L
    private var animatorSet = AnimatorSet().apply {
        duration = animationDuration
        interpolator = DecelerateInterpolator()
    }

    fun animateOnSettings(isInSettings: Boolean) {
        binding?.apply {
            root.doOnPreDraw {
                if (isInSettings) {
                    animatorSet = AnimatorSet().apply {
                        duration = 100L
                        interpolator = DecelerateInterpolator()
                        play(materialSearchBar.animateToParentSize())
                        doOnEnd {
                            changeStatusBarColor(R.color.search_bar)
                            appBarLayout.setExpanded(true)
                        }
                        start()
                    }
                } else {
                    animatorSet.run {
                        if (childAnimations.isEmpty()) return@run
                        Log.d("MainAnimator", "Shrinking SearchBar from Settings")
                        duration = animationDuration
                        removeAllListeners()
                        interpolator = AccelerateInterpolator()
                        doOnStart {
                            changeStatusBarColor(R.color.bottom_view)
                        }
                        reverse()
                    }
                }
            }
            floatingButton.hidePermanently = isInSettings
            materialToolbar.changeOnSettings(isInSettings) {
                activity?.onSupportNavigateUp()
            }
        }
    }

    fun animateOnSelection(
        isSelected: Boolean,
        selectionModeCallBack: SelectionModeCallBack
    ) {
        binding?.apply {
            expandAppBarLayout(isSelected)
            if (isSelected)
                root.post {
                    floatingButton.changeOnHomeFragment(
                        activity?.supportFragmentManager?.getVisibleFragment() is HomeViewPagerFragment
                    )
                }
            root.doOnPreDraw {
                if (isSelected) {
                    startAnimations(doOnStart = {
                        setFragmentBottomMargin(appBarLayout.height)
                    })
                } else {
                    reverseAnimations {
                        activity?.getCurrentlyVisibleBaseFragment?.fixRecyclerViewScrollPosition()
                    }
                }
            }
            floatingButton.changeOnSelection(isSelected)
            materialToolbar.changeOnSelection(isSelected, selectionModeCallBack)
        }
    }

    fun animateOnSearch(isSearching: Boolean) {
        binding?.apply {
            root.doOnPreDraw {
                if (isSearching) {
                    startAnimations(
                        doOnStart = {
                            setFragmentBottomMargin(
                                -materialToolbar.height + root.resources.getDimensionPixelSize(
                                    R.dimen.chip_group_height
                                )
                            )
                        },
                        doOnEnd = {
                            materialToolbar.requestSearchActionViewFocus()
                        }
                    )
                } else {
                    reverseAnimations {
                        materialToolbar.resetSearchActionView()
                    }
                }
            }
            floatingButton.hidePermanently = isSearching
            materialToolbar.changeOnSearch(isSearching,
                setNavigationOnClickListener = {
                    activity?.onSupportNavigateUp()
                })
        }
    }

    private inline fun ActivityMainBinding.startAnimations(
        crossinline doOnStart: () -> Unit = {},
        crossinline doOnEnd: () -> Unit = {}
    ) {
        Log.d("MainAnimator", "Expanding SearchBar")
        animatorSet = AnimatorSet().apply {
            removeAllListeners()
            duration = animationDuration
            interpolator = DecelerateInterpolator()
            playTogether(
                navigationBar.moveDown(),
                materialSearchBar.animateToParentSize()
            )
            doOnStart {
                appBarLayout.setExpanded(true)
                changeStatusBarColor(R.color.search_bar)
                doOnStart()
            }
            doOnEnd {
                doOnEnd()
            }
            start()
        }
    }

    private inline fun ActivityMainBinding.reverseAnimations(
        crossinline doOnStart: () -> Unit = {},
        crossinline doOnEnd: () -> Unit = {}
    ) {
        if (animatorSet.childAnimations.isEmpty()) return
        Log.d("MainAnimator", "Shrinking SearchBar")
        animatorSet.apply {
            removeAllListeners()
            duration = animationDuration
            interpolator = AccelerateInterpolator()
            doOnStart {
                changeStatusBarColor(R.color.bottom_view)
                doOnStart()
            }
            doOnEnd {
                setFragmentBottomMargin(navigationBar.height)
                doOnEnd()
            }
            reverse()
        }
    }

    private fun ActivityMainBinding.expandAppBarLayout(shouldExpand: Boolean) {
        root.doOnPreDraw {
            if (shouldExpand)
                appBarLayout.setExpanded(true)
            else {
                val visibleFragment = activity?.getCurrentlyVisibleBaseFragment
                val shouldMoveUp = visibleFragment?.shouldMoveFragmentUp()
                if (shouldMoveUp == true) {
                    Log.d("MainAnimator", "Collapsing the AppBarLayout")
                    appBarLayout.setExpanded(false)
                }
            }
        }
    }

    private fun ActivityMainBinding.setFragmentBottomMargin(bottomMargin: Int) {
        if (navHostContainer.marginBottom == bottomMargin) return
        val layoutParams =
            navHostContainer.layoutParams as CoordinatorLayout.LayoutParams
        layoutParams.bottomMargin = bottomMargin
        navHostContainer.post {
            navHostContainer.layoutParams = layoutParams
            navHostContainer.requestLayout()
        }
    }

    private fun changeStatusBarColor(@ColorRes resId: Int) {
        binding?.materialSearchBar?.post {
            activity?.apply {
                window.statusBarColor = getColorFromResource(resId)
            }
        }
    }

    companion object {
        var animationFinished: Boolean = true
    }
}