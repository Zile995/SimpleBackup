package com.stefan.simplebackup.utils.main

import android.util.Log
import kotlinx.coroutines.*

val coroutineExceptionHandler get() =
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

inline fun CoroutineScope.launchWithLogging(
    coroutineName: CoroutineName,
    crossinline block: suspend CoroutineScope.() -> Unit
) {
    launch(coroutineName + coroutineExceptionHandler) {
        block()
    }
}