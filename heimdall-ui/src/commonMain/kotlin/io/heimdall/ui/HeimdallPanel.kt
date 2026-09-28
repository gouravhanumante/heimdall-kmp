package io.heimdall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.heimdall.core.FlagValue
import io.heimdall.core.Heimdall

private data class PanelDestination(val title: String, val glyph: String)

private val panelDestinations = listOf(
    PanelDestination("Overview", "⌂"),
    PanelDestination("Timeline", "⋮"),
    PanelDestination("Network", "↗"),
    PanelDestination("Database", "▦"),
    PanelDestination("Storage", "▤"),
    PanelDestination("Logs", "≡"),
    PanelDestination("Flags", "⚑"),
)
private val PanelBackground = Color(0xFF0F0F1A)
private val RailBackground = Color(0xFF172536)
private val RailAccent = Color(0xFF35B9F2)

@Composable
fun HeimdallPanel(onClose: () -> Unit, modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf(0) }
    val sessions = Heimdall.sessions()
    val networkRecords by Heimdall.network.current.collectAsState()
    val storageSnapshots by Heimdall.storage.current.collectAsState()
    val logs by Heimdall.logs.current.collectAsState()
    val crashes by Heimdall.crashes.current.collectAsState()
    val events by Heimdall.events.current.collectAsState()
    val performance by Heimdall.performance.current.collectAsState()
    val databaseSnapshots by Heimdall.database.current.collectAsState()
    val flagDefinitions = Heimdall.flags.definitions()
    val currentSession = sessions.firstOrNull()
    val hasCrash = currentSession?.crashed == true || crashes.any { it.isFatal }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 3) Heimdall.database.refreshAll()
        if (selectedTab == 4) Heimdall.storage.refresh()
    }

    Box(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp)
                .fillMaxHeight(0.72f)
                .heightIn(max = 620.dp)
                .pointerInput(Unit) { detectTapGestures() },
            shape = RoundedCornerShape(18.dp),
            color = PanelBackground,
            shadowElevation = 16.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = panelDestinations[selectedTab].title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        )
                        Text(
                            text = "${Heimdall.currentScreen ?: "Global"}${if (hasCrash) " • crash" else ""}",
                            color = if (hasCrash) Color(0xFFFFB4B4) else Color(0xFFB9F7C6),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    TextButton(onClick = onClose) { Text("Close") }
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .width(56.dp)
                            .fillMaxHeight()
                            .background(RailBackground)
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        panelDestinations.forEachIndexed { index, destination ->
                            RailButton(
                                destination = destination,
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        when (selectedTab) {
                            0 -> OverviewTab(networkRecords, logs, crashes, events, performance)
                            1 -> TimelineTab(events)
                            2 -> NetworkTab(networkRecords)
                            3 -> DatabaseTab(databaseSnapshots)
                            4 -> StorageTab(storageSnapshots)
                            5 -> LogsTab(logs, crashes)
                            6 -> FlagsTab(flagDefinitions)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RailButton(
    destination: PanelDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(bottom = 6.dp)
            .size(40.dp)
            .background(if (selected) RailAccent else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = destination.title },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = destination.glyph,
            color = if (selected) Color(0xFF08202C) else Color(0xFFB9D8E8),
            fontSize = 20.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun OverviewTab(
    networkRecords: List<io.heimdall.core.NetworkRecord>,
    logs: List<io.heimdall.core.LogEntry>,
    crashes: List<io.heimdall.core.CrashRecord>,
    events: List<io.heimdall.core.HeimdallEvent>,
    performance: List<io.heimdall.core.PerformanceRecord>,
) {
    Column {
        Text("Current session", color = Color.White, fontWeight = FontWeight.Bold)
        Text("${networkRecords.size} network calls", color = Color(0xFFD7E3FF), modifier = Modifier.padding(top = 8.dp))
        Text("${logs.size} logs", color = Color(0xFFD7E3FF), modifier = Modifier.padding(top = 4.dp))
        Text("${crashes.count { it.isFatal }} fatal crashes", color = Color(0xFFFFB4B4), modifier = Modifier.padding(top = 4.dp))
        Text("${events.size} timeline events", color = Color(0xFFD7E3FF), modifier = Modifier.padding(top = 4.dp))
        Text(
            "${performance.count { it.durationMillis >= 200 }} slow operations",
            color = if (performance.any { it.durationMillis >= 200 }) Color(0xFFFFD38A) else Color(0xFFD7E3FF),
            modifier = Modifier.padding(top = 4.dp),
        )
        performance.maxByOrNull { it.durationMillis }?.let { slowest ->
            Text(
                "Slowest: ${slowest.name} (${slowest.durationMillis} ms)",
                color = Color(0xFFFFD38A),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun TimelineTab(events: List<io.heimdall.core.HeimdallEvent>) {
    if (events.isEmpty()) {
        EmptyState("No timeline events recorded")
        return
    }

    LazyColumn {
        items(events) { event ->
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(event.name, color = Color.White, fontWeight = FontWeight.Medium)
                event.screen?.let {
                    Text("Screen: $it", color = Color(0xFFB9C7FF), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
                if (event.attributes.isNotEmpty()) {
                    Text(
                        text = event.attributes.entries.joinToString { (key, value) -> "$key=$value" },
                        color = Color(0xFFD7E3FF),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkTab(records: List<io.heimdall.core.NetworkRecord>) {
    if (records.isEmpty()) {
        EmptyState("No network calls captured yet")
        return
    }

    LazyColumn {
        items(records) { record ->
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = "${record.method} ${record.url}",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = record.error ?: (record.statusCode?.let { "HTTP $it" } ?: "Pending"),
                    color = if (record.isError) Color(0xFFFFB4B4) else Color(0xFFB9F7C6),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun DatabaseTab(snapshots: List<io.heimdall.core.DatabaseSnapshot>) {
    if (snapshots.isEmpty()) {
        EmptyState("No databases discovered")
        return
    }

    LazyColumn {
        items(snapshots) { snapshot ->
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(snapshot.databaseName, color = Color.White, fontWeight = FontWeight.Medium)
                snapshot.tables.forEach { table ->
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = "${table.name} (${table.rows.size} rows)",
                            color = Color(0xFFD7E3FF),
                            fontSize = 12.sp,
                        )
                        if (table.columns.isNotEmpty()) {
                            Text(
                                text = "Columns: ${table.columns.joinToString()}",
                                color = Color(0xFFB9C7FF),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageTab(snapshots: Map<String, io.heimdall.core.StorageSnapshot>) {
    val entries = snapshots.values.toList()
    if (entries.isEmpty()) {
        EmptyState("No storage sources discovered")
        return
    }

    LazyColumn {
        items(entries) { snapshot ->
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(snapshot.sourceName, color = Color.White, fontWeight = FontWeight.Medium)
                snapshot.entries.forEach { (key, value) ->
                    Text(
                        text = "$key = $value",
                        color = Color(0xFFD7E3FF),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LogsTab(
    entries: List<io.heimdall.core.LogEntry>,
    crashes: List<io.heimdall.core.CrashRecord>,
) {
    if (entries.isEmpty() && crashes.isEmpty()) {
        EmptyState("No logs recorded")
        return
    }

    LazyColumn {
        items(crashes) { crash ->
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = "${if (crash.isFatal) "FATAL" else "CRASH"}: ${crash.exceptionType}",
                    color = Color(0xFFFFB4B4),
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = crash.message ?: "No exception message",
                    color = Color(0xFFFFD7D7),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        items(entries) { entry ->
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = "[${entry.level.name}] ${entry.tag}",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = entry.message,
                    color = Color(0xFFD7E3FF),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun FlagsTab(definitions: List<io.heimdall.core.FlagDefinition>) {
    if (definitions.isEmpty()) {
        EmptyState("No feature flags registered")
        return
    }

    LazyColumn {
        item {
            Button(
                onClick = { Heimdall.flags.resetAll() },
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Text("Reset overrides")
            }
        }
        items(definitions) { flag ->
            val overrideValue = Heimdall.flags.overrideFor(flag.key)
            val currentValue = overrideValue ?: flag.default
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(flag.key, color = Color.White, fontWeight = FontWeight.Medium)
                Text(
                    text = "Default: ${flag.default} | ${if (overrideValue != null) "Override: $overrideValue" else "No override"}",
                    color = Color(0xFFD7E3FF),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )

                when (val value = currentValue) {
                    is FlagValue.BoolValue -> {
                        TextButton(
                            onClick = {
                                Heimdall.flags.setOverride(flag.key, FlagValue.BoolValue(!value.value))
                            },
                        ) {
                            Text(if (value.value) "Turn off" else "Turn on")
                        }
                    }
                    is FlagValue.TextValue -> {
                        Text(
                            text = "Text value: ${value.value}",
                            color = Color(0xFFB9C7FF),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    is FlagValue.NumberValue -> {
                        Text(
                            text = "Number value: ${value.value}",
                            color = Color(0xFFB9C7FF),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = Color.White)
    }
}
