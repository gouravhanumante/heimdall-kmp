package io.heimdall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val panelTabs = listOf("Network", "Database", "Storage", "Logs", "Flags")

/** Full-screen panel opened by tapping the bubble. Tab bodies are placeholders until each
 * collector (network/db/storage/logs/flags) lands — see docs/TODO.md for the milestone order. */
@Composable
fun HeimdallPanel(onClose: () -> Unit, modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF0F0F1A))) {
        TabRow(selectedTabIndex = selectedTab) {
            panelTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(text = "${panelTabs[selectedTab]} — coming soon", color = Color.White)
        }
    }
}
