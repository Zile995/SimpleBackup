package com.stefan.simplebackup.utils.extensions

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        when (throwable) {
            is CancellationException -> throw throwable
            else -> {
                Log.e(
                    "CoroutineHandler",
                    "Exception from coroutine (${coroutineContext[CoroutineName]?.name})"
                            + throwable.stackTraceToString()
                )
            }
        }
    }

// Coroutine extensions
fun CoroutineScope.launchWithLogging(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
) = launch(context + coroutineExceptionHandler) {
    block()
}

// Flow extensions
/**
 * - Filter the given flow list
 */
inline fun <T> Flow<MutableList<T>>.filterBy(crossinline predicate: (T) -> Boolean) = map { list ->
    list.filter(predicate).toMutableList()
}

// LifecycleOwner coroutine extensions
inline fun LifecycleOwner.launchPostDelayed(
    delay: Long = 0L,
    crossinline block: suspend CoroutineScope.() -> Unit
) = launchOnViewLifecycle {
    delay(delay)
    block()
}

inline fun LifecycleOwner.launchOnViewLifecycle(
    context: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend CoroutineScope.() -> Unit
) = when (this) {
    is ComponentActivity -> lifecycleScope.launch(context) { block() }
    is Fragment -> viewLifecycleOwner.lifecycleScope.launch(context) { block() }
    else -> throw IllegalArgumentException("Unsupported LifecycleOwner")
}

suspend fun LifecycleOwner.repeatOnCreated(block: suspend CoroutineScope.() -> Unit) =
    repeatOnViewLifecycle(Lifecycle.State.CREATED, block)

suspend fun LifecycleOwner.repeatOnStarted(block: suspend CoroutineScope.() -> Unit) =
    repeatOnViewLifecycle(Lifecycle.State.STARTED, block)

suspend fun LifecycleOwner.repeatOnResumed(block: suspend CoroutineScope.() -> Unit) =
    repeatOnViewLifecycle(Lifecycle.State.RESUMED, block)

private suspend fun LifecycleOwner.repeatOnViewLifecycle(
    state: Lifecycle.State,
    block: suspend CoroutineScope.() -> Unit
) = when (this) {
    is ComponentActivity -> repeatOnLifecycle(state, block)
    is Fragment -> viewLifecycleOwner.repeatOnLifecycle(state, block)
    else -> throw IllegalArgumentException("Unsupported LifecycleOwner")
}