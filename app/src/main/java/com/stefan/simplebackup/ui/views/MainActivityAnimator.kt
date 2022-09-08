package com.stefan.simplebackup.ui.views

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.util.Log
import android.view.animation.DecelerateInterpolator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.marginBottom
import com.stefan.simplebackup.R
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import com.stefan.simplebackup.utils.extensions.getColorFromResource
import com.stefan.simplebackup.utils.extensions.getVisibleFragment
import com.stefan.simplebackup.utils.extensions.launchPostDelayed
import java.lang.ref.WeakReference

class MainActivityAnimator(
    private val activityReference: WeakReference<MainActivity>,
    private val bindingReference: WeakReference<ActivityMainBinding>
) {

    private val animationDuration = 250L
    private val binding get() = bindingReference.get()
    private val activity get() = activityReference.get()
    private val visibleFragment get() = activity?.getVisibleFragment()

    fun animateOnSettings(isInSettings: Boolean) {
        binding?.apply {
            root.doOnPreDraw {
                if (isInSettings) {
                    val animatorSet = AnimatorSet().apply {
                        duration = 50L
                        interpolator = DecelerateInterpolator()
                    }
                    animatorSet.playTogether(*materialSearchBar.animateToParentSize(doOnEnd = {
                        activity?.apply {
                            window.statusBarColor = getColorFromResource(R.color.searchBar)
                        }
                        appBarLayout.setExpanded(true)
                    }))
                    animatorSet.start()
                } else {
                    animateOnNavigateFromSettings()
                }
            }
            floatingButton.hidePermanently = isInSettings
            materialToolbar.changeOnSettings(isInSettings) {
                activity?.onSupportNavigateUp()
            }
        }
    }

    private fun animateOnNavigateFromSettings() {
        binding?.apply {
            if (materialSearchBar.height == materialSearchBar.initialHeight) return@apply
            val animatorSet = AnimatorSet().apply {
                duration = 250L
                interpolator = DecelerateInterpolator()
            }
            animatorSet.playTogether(*shrinkSearchBarToInitialSize())
            animatorSet.start()
        }
    }

    fun animateOnSelection(
        isSelected: Boolean,
        selectionModeCallBack: SelectionModeCallBack
    ) {
        binding?.apply {
            val animatorSet = AnimatorSet().apply {
                duration = animationDuration
                interpolator = DecelerateInterpolator()
            }
            if (isSelected) {
                root.doOnPreDraw {
                    animatorSet.playTogether(
                        navigationBar.moveDown(),
                        *animateSearchBarOnSelection()
                    )
                    animatorSet.start()
                }
                activity?.launchPostDelayed(50L) {
                    setFragmentContainerMargin(appBarLayout.height)
                }
            } else {
                root.doOnPreDraw {
                    animatorSet.playTogether(
                        navigationBar.moveUp {
                            setFragmentContainerMargin(navigationBar.height)
                            visibleFragment?.fixRecyclerViewScrollPosition()
                        }, *shrinkSearchBarToInitialSize()
                    )
                    animatorSet.start()
                }
            }
            expandAppBarLayout(isSelected)
            floatingButton.changeOnSelection(isSelected)
            materialToolbar.changeOnSelection(isSelected, selectionModeCallBack)
        }
    }

    fun animateOnSearch(isSearching: Boolean) {
        binding?.apply {
            val animatorSet = AnimatorSet().apply {
                duration = animationDuration
                interpolator = DecelerateInterpolator()
            }
            if (isSearching) {
                setFragmentContainerMargin(appBarLayout.height)
                root.doOnPreDraw {
                    animatorSet.playTogether(
                        navigationBar.moveDown(),
                        *animateSearchBarOnClick()
                    )
                    animatorSet.start()
                }
            } else {
                root.doOnPreDraw {
                    animatorSet.playTogether(
                        navigationBar.moveUp {
                            setFragmentContainerMargin(navigationBar.height)
                        }, *shrinkSearchBarToInitialSize()
                    )
                    animatorSet.start()
                }
            }
            floatingButton.hidePermanently = isSearching
            materialToolbar.changeOnSearch(isSearching,
                setNavigationOnClickListener = {
                    activity?.onSupportNavigateUp()
                })
        }
    }

    private fun expandAppBarLayout(shouldExpand: Boolean) =
        binding?.apply {
            root.doOnPreDraw {
                if (shouldExpand) {
                    animationFinished = false
                    appBarLayout.setExpanded(shouldExpand)
                } else {
                    if (visibleFragment?.shouldMoveFragmentUp() == true) {
                        Log.d("AppBarLayout", "Collapsing the AppBarLayout")
                        appBarLayout.setExpanded(false)
                    }
                }
            }
        }

    private fun setFragmentContainerMargin(margin: Int) {
        binding?.apply {
            if (navHostContainer.marginBottom == margin) return@apply
            val layoutParams =
                navHostContainer.layoutParams as CoordinatorLayout.LayoutParams
            layoutParams.bottomMargin = margin
            navHostContainer.layoutParams = layoutParams
            navHostContainer.requestLayout()
        }
    }

    private fun animateSearchBarOnClick(): Array<ValueAnimator> =
        binding?.run {
            materialSearchBar.animateToParentSize(
                doOnStart = {
                    Log.d("MainAnimator", "Expanding SearchBar on click")
                    activity?.apply {
                        window.statusBarColor = getColorFromResource(R.color.searchBar)
                    }
                },
                doOnEnd = {
                    appBarLayout.setExpanded(true)
                })
        } ?: arrayOf()

    private fun animateSearchBarOnSelection(): Array<ValueAnimator> =
        binding?.run {
            materialSearchBar.animateToParentSize(
                doOnStart = {
                    Log.d("MainAnimator", "Expanding SearchBar on selection")
                    activity?.apply {
                        window.statusBarColor = getColorFromResource(R.color.searchBar)
                    }
                })
        } ?: arrayOf()

    private fun shrinkSearchBarToInitialSize(): Array<ValueAnimator> =
        binding?.run {
            materialSearchBar.animateToInitialSize(
                doOnStart = {
                    Log.d("MainAnimator", "Shrinking SearchBar to initial size")
                    activity?.apply {
                        window.statusBarColor = getColorFromResource(R.color.bottomView)
                    }
                })
        } ?: arrayOf()

    companion object {
        @Volatile
        var animationFinished: Boolean = true
    }
}