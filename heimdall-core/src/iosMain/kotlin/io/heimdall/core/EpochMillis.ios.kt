package io.heimdall.core

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

@OptIn(ExperimentalForeignApi::class)
internal actual fun epochMillisNow(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
