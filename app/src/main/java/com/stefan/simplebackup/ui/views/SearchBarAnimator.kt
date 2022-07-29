package com.stefan.simplebackup.ui.views

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import androidx.annotation.ColorRes
import com.stefan.simplebackup.R
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.utils.extensions.*
import java.lang.ref.WeakReference

class SearchBarAnimator(
    private val activityReference: WeakReference<MainActivity>,
    private val bindingReference: WeakReference<ActivityMainBinding>
) {

    val expandDuration = 300L
    val shrinkDuration = 250L

    private val activity get() = activityReference.get()
    private val binding get() = bindingReference.get()

    fun animateOnClick() {
        activity?.apply {
            binding?.apply {
                searchBar.isEnabled = false
                expandToParentView(
                    doOnStart = {
                        searchMagIcon.fadeOut(expandDuration)
                        searchText.fadeOut(expandDuration)
                        searchText.moveHorizontally(expandDuration, -15f)
                        animateStatusBarColor(
                            color = R.color.searchBar,
                            animationDuration = expandDuration
                        )
                    },
                    doOnEnd = {
                        appBarLayout.setExpanded(true)
                        searchView.requestFocus()
                        searchView.fadeIn(animationDuration = expandDuration)
                        materialToolbar.fadeIn(animationDuration = expandDuration,
                            onAnimationCancel = {
                                revertToInitialSize(true)
                            },
                            onAnimationEnd = {
                                searchBar.hide()
                            })
                    })
            }
        }
    }

    fun animateOnSelection() {
        activity?.apply {
            binding?.apply {
                searchBar.isEnabled = false
                expandToParentView(
                    doOnStart = {
                        searchMagIcon.fadeOut(expandDuration)
                        searchText.fadeOut(expandDuration)
                        animateStatusBarColor(
                            color = R.color.searchBar,
                            animationDuration = expandDuration
                        )
                    },
                    doOnEnd = {
                        materialToolbar.fadeIn(animationDuration = expandDuration,
                            onAnimationCancel = {
                                revertToInitialSize(false)
                            },
                            onAnimationEnd = {
                                searchBar.hide()
                            })
                    })
            }
        }
    }

    fun revertToInitialSize(isSearching: Boolean) {
        activity?.apply {
            binding?.apply {
                searchBar.show()
                if (isSearching) {
                    searchText.fadeIn(0L)
                    resetSearchView()
                }
                appBarLayout.changeBackgroundColor(applicationContext, R.color.bottomView)
                materialToolbar.fadeOut(animationDuration = 0L) {
                    searchBar.animateToInitialSize(duration = shrinkDuration,
                        doOnStart = {
                            animateStatusBarColor(
                                color = R.color.bottomView,
                                animationDuration = shrinkDuration
                            )
                            searchMagIcon.fadeIn(shrinkDuration)
                            if (isSearching)
                                searchText.moveHorizontally(shrinkDuration, 0f)
                            else
                                searchText.fadeIn(shrinkDuration)
                        },
                        doOnEnd = {
                            searchBar.isEnabled = true
                        })
                }
            }
        }
    }

    private inline fun expandToParentView(
        crossinline doOnStart: () -> Unit,
        crossinline doOnEnd: () -> Unit
    ) {
        activity?.apply {
            binding?.apply {
                searchBar.animateToParentSize(duration = expandDuration,
                    doOnStart = {
                        animationFinished = false
                        doOnStart.invoke()
                    },
                    doOnEnd = {
                        doOnEnd.invoke()
                        animationFinished = true
                    })
            }
        }
    }

    private fun resetSearchView() {
        activity?.apply {
            binding?.apply {
                searchView.clearFocus()
                searchView.fadeOut(0L)
                searchView.setQuery("", false)
            }
        }
    }

    fun prepareWhenSearching(isSearching: Boolean) {
        activity?.apply {
            binding?.apply {
                if (isSearching) {
                    window.statusBarColor = getColorFromResource(R.color.searchBar)
                    appBarLayout.changeBackgroundColor(applicationContext, R.color.searchBar)
                    searchText.moveHorizontally(0L, -15f)
                    materialToolbar.show()
                    searchView.show()
                    navigationBar.hide()
                }
            }
        }
    }

    private fun animateStatusBarColor(@ColorRes color: Int, animationDuration: Long) {
        activity?.apply {
            binding?.apply {
                window.apply {
                    ObjectAnimator.ofObject(
                        this,
                        "statusBarColor",
                        ArgbEvaluator(),
                        statusBarColor,
                        context.getColorFromResource(color)
                    ).apply {
                        duration = animationDuration
                        start()
                    }
                }
            }
        }
    }

    companion object {
        var animationFinished: Boolean = true
    }
}