package io.heimdall.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.heimdall.core.Heimdall
import kotlinx.coroutines.CoroutineScope

/** Release build stand-in for `heimdall-storage`: never reads [dataStore]. */
fun Heimdall.attachDataStore(dataStore: DataStore<Preferences>, name: String, scope: CoroutineScope) = Unit
