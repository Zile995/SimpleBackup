package com.stefan.simplebackup.ui.views

import com.google.android.material.appbar.AppBarLayout
import kotlin.math.abs

abstract class AppBarLayoutStateChangedListener : AppBarLayout.OnOffsetChangedListener {

    enum class AppBarLayoutState {
        IDLE,
        EXPANDED,
        COLLAPSED
    }

    private var currentState = AppBarLayoutState.IDLE

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        when {
            verticalOffset == 0 ->
                changeCurrentState(appBarLayout, AppBarLayoutState.EXPANDED)
            abs(verticalOffset) >= appBarLayout.totalScrollRange ->
                changeCurrentState(appBarLayout, AppBarLayoutState.COLLAPSED)
            else ->
                changeCurrentState(appBarLayout, AppBarLayoutState.IDLE)
        }
    }

    private fun changeCurrentState(appBarLayout: AppBarLayout, state: AppBarLayoutState) {
        if (currentState != state) {
            onStateChanged(appBarLayout, state)
            currentState = state
        }
    }

    abstract fun onStateChanged(appBarLayout: AppBarLayout, state: AppBarLayoutState)
}