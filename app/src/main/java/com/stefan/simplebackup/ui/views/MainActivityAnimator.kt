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
import com.stefan.simplebackup.utils.extensions.getColorFromResource
import com.stefan.simplebackup.utils.extensions.getVisibleFragment
import java.lang.ref.WeakReference

class MainActivityAnimator(
    private val activityReference: WeakReference<MainActivity>,
    private val bindingReference: WeakReference<ActivityMainBinding>
) {
    private val binding get() = bindingReference.get()
    private val activity get() = activityReference.get()
    private val visibleFragment get() = activity?.getVisibleFragment()

    private val animationDuration = 250L
    private var animatorSet = AnimatorSet().apply {
        duration = animationDuration
        interpolator = DecelerateInterpolator()
    }

    fun animateOnSettings(isInSettings: Boolean) {
        binding?.apply {
            root.doOnPreDraw {
                if (isInSettings) {
                    root.doOnPreDraw {
                        animatorSet = AnimatorSet().apply {
                            duration = 50L
                            interpolator = DecelerateInterpolator()
                            play(materialSearchBar.animateToParentSize())
                            doOnEnd {
                                changeStatusBarColor(R.color.searchBar)
                                appBarLayout.setExpanded(true)
                            }
                            start()
                        }
                    }
                } else
                    root.doOnPreDraw {
                        animatorSet.apply {
                            duration = animationDuration
                            interpolator = AccelerateInterpolator()
                            doOnStart {
                                changeStatusBarColor(R.color.bottomView)
                            }
                            doOnEnd {
                                changeStatusBarColor(R.color.bottomView)
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
            root.doOnPreDraw {
                if (isSelected) {
                    startAnimations(fragmentBottomMargin = appBarLayout.height)
                } else {
                    reverseAnimations {
                        visibleFragment?.fixRecyclerViewScrollPosition()
                    }
                }
                expandAppBarLayout(isSelected)
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
                        fragmentBottomMargin = -materialToolbar.height - root.resources.getDimensionPixelSize(
                            R.dimen.chip_group_height
                        )
                    )
                } else {
                    reverseAnimations(
                        doOnStart = {
                            setFragmentBottomMargin(navigationBar.height)
                        }
                    )
                }
            }
            floatingButton.hidePermanently = isSearching
            materialToolbar.changeOnSearch(isSearching,
                setNavigationOnClickListener = {
                    activity?.onSupportNavigateUp()
                })
        }
    }

    private fun ActivityMainBinding.startAnimations(
        fragmentBottomMargin: Int
    ) {
        root.doOnPreDraw {
            animatorSet = AnimatorSet().apply {
                duration = animationDuration
                interpolator = DecelerateInterpolator()
                playTogether(
                    navigationBar.moveDown(),
                    materialSearchBar.animateToParentSize()
                )
                doOnStart {
                    Log.d("MainAnimator", "Expanding SearchBar on click")
                    changeStatusBarColor(R.color.searchBar)
                    setFragmentBottomMargin(fragmentBottomMargin)
                    appBarLayout.setExpanded(true)
                }
                start()
            }
        }
    }

    private inline fun ActivityMainBinding.reverseAnimations(
        crossinline doOnStart: () -> Unit = {},
        crossinline doOnEnd: () -> Unit = {}
    ) {
        root.doOnPreDraw {
            animatorSet.apply {
                interpolator = AccelerateInterpolator()
                doOnStart {
                    changeStatusBarColor(R.color.bottomView)
                    doOnStart()
                }
                doOnEnd {
                    setFragmentBottomMargin(navigationBar.height)
                    doOnEnd()
                }
                reverse()
            }
        }
    }

    private fun ActivityMainBinding.expandAppBarLayout(shouldExpand: Boolean) {
        root.doOnPreDraw {
            if (shouldExpand)
                appBarLayout.setExpanded(true)
            else {
                if (visibleFragment?.shouldMoveFragmentUp() == true) {
                    Log.d("AppBarLayout", "Collapsing the AppBarLayout")
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
    }

    private fun changeStatusBarColor(@ColorRes resId: Int) {
        activity?.apply {
            window.statusBarColor = getColorFromResource(resId)
        }
    }

    companion object {
        @Volatile
        var animationFinished: Boolean = true
    }
}