package com.stefan.simplebackup.utils.main

import android.util.Log
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val coroutineExceptionHandler =
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

fun CoroutineScope.launchWithLogging(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return launch(context + coroutineExceptionHandler) {
        block()
    }
}