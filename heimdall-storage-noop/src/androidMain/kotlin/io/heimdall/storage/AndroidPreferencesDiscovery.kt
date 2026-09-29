package io.heimdall.storage

import android.content.Context
import io.heimdall.core.Heimdall

/** Release build stand-in for `heimdall-storage`: discovers nothing. */
fun Heimdall.discoverStorage(context: Context) = Unit

fun Heimdall.discoverAndroidPreferences(context: Context) = Unit

fun Heimdall.discoverAndroidKeystore() = Unit
