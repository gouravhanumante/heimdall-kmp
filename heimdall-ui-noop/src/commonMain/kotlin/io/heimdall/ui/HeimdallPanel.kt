package io.heimdall.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Release build stand-in: renders nothing. Not reachable through [HeimdallOverlay] here, kept
 * only so code referencing it directly still compiles against this artifact. */
@Composable
fun HeimdallPanel(onClose: () -> Unit, modifier: Modifier = Modifier) = Unit
