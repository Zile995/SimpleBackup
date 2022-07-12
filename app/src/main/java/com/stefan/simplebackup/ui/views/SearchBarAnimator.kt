package com.stefan.simplebackup.ui.views

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.view.View
import androidx.annotation.ColorRes
import com.stefan.simplebackup.R
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.utils.extensions.*
import java.lang.ref.WeakReference

class SearchBarAnimator(
    val activityReference: WeakReference<MainActivity>,
    val bindingReference: WeakReference<ActivityMainBinding>
) {

    val expandDuration = 300L
    val shrinkDuration = 250L

    inline fun animateSearchBarOnClick(
        crossinline navigate: () -> Unit
    ) {
        activityReference.get()?.apply {
            bindingReference.get()?.apply {
                searchBar.animateTo(
                    (searchBar.parent as View).height,
                    (searchBar.parent as View).width,
                    animationDuration = expandDuration,
                    doOnAnimationStart = {
                        navigate.invoke()
                        searchMagIcon.fadeOut(expandDuration)
                        searchText.fadeOut(expandDuration)
                        searchText.moveHorizontally(expandDuration, -15f)
                        animateStatusBarColor(
                            color = R.color.searchBar,
                            animationDuration = expandDuration
                        )
                    },
                    doOnAnimationEnd = {
                        searchView.requestFocus()
                        floatingButton.hide()
                        floatingButton.setOnClickListener(null)
                        searchView.fadeIn(animationDuration = expandDuration)
                        materialToolbar.fadeIn(animationDuration = expandDuration,
                            onAnimationEnd = {
                                searchBar.hide()
                            })
                    })
            }
        }
    }

    fun revertSearchBarToInitialSize() {
        activityReference.get()?.apply {
            bindingReference.get()?.apply {
                searchBar.show()
                searchText.fadeIn(0L)
                resetSearchView()
                appBarLayout.changeBackgroundColor(applicationContext, R.color.bottomView)
                materialToolbar.fadeOut(animationDuration = 0L) {
                    searchBar.animateToInitialSize(animationDuration = shrinkDuration,
                        doOnAnimationStart = {
                            animateStatusBarColor(
                                color = R.color.bottomView,
                                animationDuration = shrinkDuration
                            )
                            searchMagIcon.fadeIn(shrinkDuration)
                            searchText.moveHorizontally(shrinkDuration, 0f)
                        },
                        doOnAnimationEnd = {
                            searchBar.isEnabled = true
                        })
                }
            }
        }
    }

    private fun resetSearchView() {
        bindingReference.get()?.apply {
            searchView.setQuery("", false)
            searchView.clearFocus()
            searchView.fadeOut(0L)
        }
    }

    fun prepareWhenSearching(isSearching: Boolean) {
        activityReference.get()?.apply {
            bindingReference.get()?.apply {
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

    fun animateStatusBarColor(@ColorRes color: Int, animationDuration: Long) {
        activityReference.get()?.apply {
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