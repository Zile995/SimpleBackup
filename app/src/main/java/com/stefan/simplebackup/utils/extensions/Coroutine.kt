package com.stefan.simplebackup.utils.extensions

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

val coroutineExceptionHandler =
    CoroutineExceptionHandler { coroutineContext, throwable ->
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

suspend fun <T, R> Iterable<T>.pmap(f: suspend (T) -> R): List<R> = coroutineScope {
    map {
        async {
            f(it)
        }
    }.awaitAll()
}

fun CoroutineScope.launchWithLogging(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return launch(context + coroutineExceptionHandler) {
        block()
    }
}

inline fun LifecycleOwner.launchOnViewLifecycle(
    context: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend CoroutineScope.() -> Unit
) {
    when (this) {
        is ComponentActivity -> {
            lifecycleScope.launch(context) { block() }
        }
        is Fragment -> {
            viewLifecycleOwner.lifecycleScope.launch(context) { block() }
        }
        else -> throw IllegalArgumentException("Unsupported LifecycleOwner")
    }
}

suspend fun LifecycleOwner.repeatOnViewLifecycle(
    state: Lifecycle.State,
    block: suspend CoroutineScope.() -> Unit
) {
    when (this) {
        is ComponentActivity -> {
            this.repeatOnLifecycle(state, block)
        }
        is Fragment -> {
            viewLifecycleOwner.repeatOnLifecycle(state, block)
        }
        else -> throw IllegalArgumentException("Unsupported LifecycleOwner")
    }
}