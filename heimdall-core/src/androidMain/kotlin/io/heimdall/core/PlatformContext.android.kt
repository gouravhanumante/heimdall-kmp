package io.heimdall.core

import android.content.Context

actual class PlatformContext(val context: Context)

/** `no_backup_files_dir`, not `getDatabasePath()` — the latter is included in Android's
 * auto-backup by default, which would let a debug capture (headers included, see
 * docs/plugins/network.md) end up in a user's Google Drive backup. */
actual fun resolveDatabasePath(context: PlatformContext, fileName: String): String =
    context.context.applicationContext.noBackupFilesDir.resolve(fileName).absolutePath
