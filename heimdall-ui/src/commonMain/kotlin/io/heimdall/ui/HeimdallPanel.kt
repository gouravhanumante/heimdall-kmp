package io.heimdall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.heimdall.core.FlagValue
import io.heimdall.core.HealthMetric
import io.heimdall.core.HealthRules
import io.heimdall.core.HealthStatus
import io.heimdall.core.Heimdall
import io.heimdall.ui.generated.resources.Res
import io.heimdall.ui.generated.resources.heimdall_horn_logo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource

private data class PanelDestination(val title: String, val glyph: String)

private val panelDestinations = listOf(
    PanelDestination("Overview", "⌂"),
    PanelDestination("Network", "↗"),
    PanelDestination("Database", "▦"),
    PanelDestination("Storage", "▤"),
    PanelDestination("Logs", "≡"),
    PanelDestination("Flags", "⚑"),
    PanelDestination("Sessions", "◷"),
)
private val PanelBackground = HeimdallDesign.background
private val RailBackground = HeimdallDesign.surfaceVariant
private val RailAccent = HeimdallDesign.primary
private val CodeBackground = HeimdallDesign.code
private val TableHeader = HeimdallDesign.surfaceVariant
private val TableRow = HeimdallDesign.surface

@Composable
fun HeimdallPanel(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val palette = HeimdallDesign.palette()
    var selectedTab by remember { mutableStateOf(0) }
    var selectedSessionId by remember { mutableStateOf<Long?>(null) }
    var selectedNetworkRecord by remember { mutableStateOf<io.heimdall.core.NetworkRecord?>(null) }
    val sessions = Heimdall.sessions()
    val networkRecords by Heimdall.network.current.collectAsState()
    val storageSnapshots by Heimdall.storage.current.collectAsState()
    val logs by Heimdall.logs.current.collectAsState()
    val crashes by Heimdall.crashes.current.collectAsState()
    val events by Heimdall.events.current.collectAsState()
    val performance by Heimdall.performance.current.collectAsState()
    val frameRecords by Heimdall.performance.frames.collectAsState()
    val databaseSnapshots by Heimdall.database.current.collectAsState()
    val flagDefinitions = Heimdall.flags.definitions()
    val currentSession = sessions.firstOrNull()
    val hasCrash = currentSession?.crashed == true || crashes.any { it.isFatal }
    val historicalNetwork by produceState(emptyList<io.heimdall.core.NetworkRecord>(), selectedSessionId) {
        value = selectedSessionId?.let { withContext(Dispatchers.Default) { Heimdall.network.forSession(it) } } ?: emptyList()
    }
    val historicalLogs by produceState(emptyList<io.heimdall.core.LogEntry>(), selectedSessionId) {
        value = selectedSessionId?.let { withContext(Dispatchers.Default) { Heimdall.logs.forSession(it) } } ?: emptyList()
    }
    val historicalCrashes by produceState(emptyList<io.heimdall.core.CrashRecord>(), selectedSessionId) {
        value = selectedSessionId?.let { withContext(Dispatchers.Default) { Heimdall.crashes.forSession(it) } } ?: emptyList()
    }
    val historicalEvents by produceState(emptyList<io.heimdall.core.HeimdallEvent>(), selectedSessionId) {
        value = selectedSessionId?.let { withContext(Dispatchers.Default) { Heimdall.events.forSession(it) } } ?: emptyList()
    }
    val displayedNetwork = if (selectedSessionId == null) networkRecords else historicalNetwork
    val displayedLogs = if (selectedSessionId == null) logs else historicalLogs
    val displayedCrashes = if (selectedSessionId == null) crashes else historicalCrashes
    val displayedEvents = if (selectedSessionId == null) events else historicalEvents
    val displayedPerformance = if (selectedSessionId == null) performance else emptyList()
    val displayedFrames = if (selectedSessionId == null) frameRecords else emptyList()

    LaunchedEffect(selectedTab) {
        if (selectedTab == 2) Heimdall.database.refreshAll()
        if (selectedTab == 3) Heimdall.storage.refresh()
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        val popupWidth = minOf(maxWidth * 0.94f, 560.dp)
        val popupHeight = minOf(maxHeight * 0.9f, 760.dp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClose),
        ) {
        Surface(
            modifier = modifier
                .align(Alignment.Center)
                .width(popupWidth)
                .height(popupHeight)
                .clickable(enabled = false) { }
                .pointerInput(Unit) { detectTapGestures() },
            shape = RoundedCornerShape(HeimdallDesign.popupCorner),
            color = palette.background,
            shadowElevation = 16.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.heimdall_horn_logo),
                        contentDescription = "Heimdall",
                        modifier = Modifier.size(32.dp).padding(end = 8.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = panelDestinations[selectedTab].title,
                            color = palette.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        )
                        Text(
                            text = "${Heimdall.currentScreen ?: "Global"}${if (hasCrash) " • crash" else ""}",
                            color = if (hasCrash) palette.error else palette.success,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.semantics { contentDescription = "Close inspector" },
                    ) {
                        Text("×", color = palette.onSurfaceVariant, fontSize = 24.sp)
                    }
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .width(56.dp)
                            .fillMaxHeight()
                            .background(palette.surfaceVariant)
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
                            0 -> OverviewTab(displayedNetwork, displayedLogs, displayedCrashes, displayedEvents, displayedPerformance, displayedFrames) { selectedTab = it }
                            1 -> if (selectedNetworkRecord == null) {
                                NetworkTab(displayedNetwork) { selectedNetworkRecord = it }
                            } else {
                                NetworkDetail(selectedNetworkRecord!!) { selectedNetworkRecord = null }
                            }
                            2 -> if (selectedSessionId == null) DatabaseTab(databaseSnapshots) else HistoryOnlyNotice()
                            3 -> if (selectedSessionId == null) StorageTab(storageSnapshots) else HistoryOnlyNotice()
                            4 -> LogsTab(displayedLogs, displayedCrashes)
                            5 -> if (selectedSessionId == null) FlagsTab(flagDefinitions) else HistoryOnlyNotice()
                            6 -> SessionsTab(sessions, selectedSessionId) { selectedSessionId = it }
                        }
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
            color = if (selected) HeimdallDesign.onPrimary else HeimdallDesign.onSurfaceVariant,
            fontSize = 20.sp,
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
    frames: List<io.heimdall.core.FrameRecord>,
    onNavigate: (Int) -> Unit,
) {
    val health = HealthRules.current(networkRecords, crashes, performance, frames)
    Column {
        Text("Current session", color = Color.White, fontWeight = FontWeight.Bold)
        Text("${networkRecords.size} network calls", color = HeimdallDesign.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        Text("${logs.size} logs", color = HeimdallDesign.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        Text("${crashes.count { it.isFatal }} fatal crashes", color = HeimdallDesign.error, modifier = Modifier.padding(top = 4.dp))
        Text("${events.size} timeline events", color = HeimdallDesign.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        Text(
            "${performance.count { it.durationMillis >= 200 }} slow operations",
            color = if (performance.any { it.durationMillis >= 200 }) HeimdallDesign.warning else HeimdallDesign.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            "${frames.count { it.durationMillis > 16 }} slow frames",
            color = if (frames.any { it.durationMillis > 16 }) HeimdallDesign.warning else HeimdallDesign.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        frames.maxByOrNull { it.durationMillis }?.let { worstFrame ->
            Text(
                "Worst frame: ${worstFrame.durationMillis} ms",
                color = HeimdallDesign.warning,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        performance.maxByOrNull { it.durationMillis }?.let { slowest ->
            Text(
                "Slowest: ${slowest.name} (${slowest.durationMillis} ms)",
                color = HeimdallDesign.warning,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Text("Health", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
        health.forEach { HealthMetricRow(it) }
        Text("Quick access", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
        Row(modifier = Modifier.padding(top = 8.dp)) {
            listOf(
                PanelDestination("Network", "↗") to 1,
                PanelDestination("Database", "▦") to 2,
                PanelDestination("Storage", "▤") to 3,
                PanelDestination("Logs", "≡") to 4,
            ).forEach { (destination, tab) ->
                Surface(
                    color = HeimdallDesign.surface,
                    shape = RoundedCornerShape(HeimdallDesign.corner),
                    modifier = Modifier.padding(end = 6.dp).clickable { onNavigate(tab) },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                        Text(destination.glyph, color = HeimdallDesign.primary, fontSize = 18.sp)
                        Text(destination.title, color = HeimdallDesign.onSurfaceVariant, fontSize = 9.sp)
                    }
                }
            }
        }
        val issueCount = crashes.count { it.isFatal } + networkRecords.count { it.isError } +
            performance.count { it.durationMillis >= 200 } + frames.count { it.durationMillis > 16 }
        if (issueCount > 0) {
            Text("Needs attention", color = HeimdallDesign.warning, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
            if (crashes.any { it.isFatal }) IssueSurface("Fatal crash recorded", HeimdallDesign.error) { onNavigate(4) }
            if (networkRecords.any { it.isError }) IssueSurface("Failed network request", HeimdallDesign.error) { onNavigate(1) }
            if (performance.any { it.durationMillis >= 200 }) IssueSurface("Slow operation detected", HeimdallDesign.warning) { onNavigate(0) }
            if (frames.any { it.durationMillis > 16 }) IssueSurface("Slow frame detected", HeimdallDesign.warning) { onNavigate(0) }
        }
    }
}

@Composable
private fun HealthMetricRow(metric: HealthMetric) {
    val accent = when (metric.status) {
        HealthStatus.OK -> HeimdallDesign.success
        HealthStatus.WARNING -> HeimdallDesign.warning
        HealthStatus.CRITICAL -> HeimdallDesign.error
        HealthStatus.UNAVAILABLE -> HeimdallDesign.onSurfaceVariant
    }
    Surface(
        color = HeimdallDesign.surface,
        shape = RoundedCornerShape(HeimdallDesign.corner),
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(metric.label, color = HeimdallDesign.onSurface, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(metric.displayValue, color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(
                text = if (metric.confidence == io.heimdall.core.MetricConfidence.MEASURED) "Measured" else "Unavailable",
                color = HeimdallDesign.onSurfaceVariant,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun IssueSurface(message: String, accent: Color, onClick: () -> Unit) {
    Surface(
        color = HeimdallDesign.surface,
        shape = RoundedCornerShape(HeimdallDesign.corner),
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clickable(onClick = onClick),
    ) {
        Text(message, color = accent, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
    }
}

@Composable
private fun SessionsTab(
    sessions: List<io.heimdall.core.Session>,
    selectedSessionId: Long?,
    onSelect: (Long?) -> Unit,
) {
    if (sessions.isEmpty()) {
        EmptyState("No sessions recorded")
        return
    }

    LazyColumn {
        items(sessions) { session ->
            val isCurrent = session.id == Heimdall.currentSessionId && selectedSessionId == null
            Surface(
                color = if (isCurrent) HeimdallDesign.primaryContainer else HeimdallDesign.surface,
                shape = RoundedCornerShape(HeimdallDesign.corner),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable {
                    onSelect(if (session.id == Heimdall.currentSessionId) null else session.id)
                },
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("◷", color = HeimdallDesign.primary, fontSize = 20.sp)
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text(
                            if (isCurrent) "Current session" else "Session ${session.id}",
                            color = HeimdallDesign.onSurface,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Started ${formatSessionTimestamp(session.startedAtMillis)}${if (session.crashed) " • crashed" else ""}",
                            color = if (session.crashed) HeimdallDesign.error else HeimdallDesign.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryOnlyNotice() {
    EmptyState("This section shows live state only. Select Current session to view it.")
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
                    Text("Screen: $it", color = HeimdallDesign.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
                if (event.attributes.isNotEmpty()) {
                    Text(
                        text = event.attributes.entries.joinToString { (key, value) -> "$key=$value" },
                        color = HeimdallDesign.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkTab(
    records: List<io.heimdall.core.NetworkRecord>,
    onSelect: (io.heimdall.core.NetworkRecord) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = records.filter { record ->
        val value = query.trim().lowercase()
        value.isEmpty() || record.url.lowercase().contains(value) ||
            record.method.lowercase().contains(value) ||
            record.statusCode?.toString()?.contains(value) == true ||
            record.error?.lowercase()?.contains(value) == true
    }
    if (records.isEmpty()) {
        EmptyState("No network calls captured yet")
        return
    }

    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search URL, method, status, or error") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
        items(filtered) { record ->
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Column(modifier = Modifier.clickable { onSelect(record) }) {
                    Text(
                        text = record.method,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(record.url, color = HeimdallDesign.onSurfaceVariant, fontSize = 12.sp)
                    Text(
                        text = record.error ?: (record.statusCode?.let { "HTTP $it" } ?: "Pending") +
                            (record.durationMillis?.let { " • ${it} ms" } ?: ""),
                        color = if (record.isError) HeimdallDesign.error else HeimdallDesign.success,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun NetworkDetail(record: io.heimdall.core.NetworkRecord, onBack: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    LazyColumn {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Back to network calls" }) {
                    Text("←", color = HeimdallDesign.onSurfaceVariant, fontSize = 22.sp)
                }
                Text("${record.method} ${record.url}", color = HeimdallDesign.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(modifier = Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = if (record.isError) HeimdallDesign.error else HeimdallDesign.success, shape = RoundedCornerShape(6.dp)) {
                    Text("${record.statusCode ?: "Pending"}", color = HeimdallDesign.onSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                }
                record.durationMillis?.let { Text("$it ms", color = HeimdallDesign.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp)) }
            }
            OutlinedButton(
                onClick = { clipboard.setText(AnnotatedString(record.toCurl())) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            ) {
                Text("Copy cURL", modifier = Modifier.padding(start = 8.dp))
            }
            NetworkBlock(clipboard, "Request headers", record.requestHeaders.entries.joinToString("\n") { "${it.key}: ${it.value}" })
            NetworkBlock(clipboard, "Request body", record.requestBody ?: "No request body")
            NetworkBlock(clipboard, "Response headers", record.responseHeaders.entries.joinToString("\n") { "${it.key}: ${it.value}" })
            NetworkBlock(clipboard, "Response body", record.responseBody ?: record.error ?: "No response body")
        }
    }
}

@Composable
private fun NetworkBlock(clipboard: androidx.compose.ui.platform.ClipboardManager, title: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            IconButton(
                onClick = { clipboard.setText(AnnotatedString(value)) },
                modifier = Modifier.semantics { contentDescription = "Copy $title" },
            ) { Text("⧉", color = HeimdallDesign.onSurfaceVariant, fontSize = 18.sp) }
        }
        Surface(
            color = CodeBackground,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            SelectionContainer {
                Text(
                    value,
                    color = HeimdallDesign.onCode,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
    }
}

private fun io.heimdall.core.NetworkRecord.toCurl(): String = buildString {
    append("curl -X ").append(method).append(" '").append(url).append("'")
    requestHeaders.forEach { (key, value) -> append(" -H '").append(key).append(": ").append(value).append("'") }
    requestBody?.let { append(" --data-raw '").append(it.replace("'", "'\\''")).append("'") }
}

@Composable
private fun DatabaseTab(snapshots: List<io.heimdall.core.DatabaseSnapshot>) {
    if (snapshots.isEmpty()) {
        EmptyState("No databases discovered")
        return
    }

    var selectedDatabase by remember { mutableStateOf<String?>(null) }
    var selectedTable by remember { mutableStateOf<String?>(null) }
    val databaseName = selectedDatabase
    val table = snapshots.firstOrNull { it.databaseName == databaseName }
        ?.tables?.firstOrNull { it.name == selectedTable }

    if (table != null && databaseName != null) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedTable = null }, modifier = Modifier.semantics { contentDescription = "Back to tables" }) {
                    Text("←", color = HeimdallDesign.onSurfaceVariant, fontSize = 22.sp)
                }
                Text("$databaseName / ${table.name}", color = HeimdallDesign.onSurface, fontWeight = FontWeight.Bold)
            }
            DatabaseTableGrid(databaseName = databaseName, table = table)
        }
        return
    }

    LazyColumn {
        items(snapshots.flatMap { snapshot -> snapshot.tables.map { snapshot.databaseName to it } }) { (databaseName, tableItem) ->
            Surface(
                color = HeimdallDesign.surface,
                shape = RoundedCornerShape(HeimdallDesign.corner),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable {
                    selectedDatabase = databaseName
                    selectedTable = tableItem.name
                },
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("▦", color = HeimdallDesign.primary, fontSize = 20.sp)
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text(tableItem.name, color = HeimdallDesign.onSurface, fontWeight = FontWeight.Bold)
                        Text("$databaseName • ${tableItem.rows.size} rows", color = HeimdallDesign.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DatabaseTableGrid(databaseName: String, table: io.heimdall.core.DatabaseTable) {
    var query by remember { mutableStateOf("") }
    val filteredRows by produceState(initialValue = table.rows, query, table, databaseName) {
        value = withContext(Dispatchers.Default) {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) table.rows else searchTable(databaseName, table, trimmed)
        }
    }
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.padding(top = 8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search ${table.name}") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Surface(
            color = TableRow,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(top = 8.dp).horizontalScroll(scrollState),
        ) {
            Column {
                TableRowView(table.columns, isHeader = true)
                filteredRows.forEach { row -> TableRowView(row.map { it ?: "NULL" }) }
            }
        }
    }
}

/**
 * Reaches the real table through the database's own [io.heimdall.core.DatabaseQueryRunner]
 * instead of filtering the (possibly truncated) rows already loaded into [table] — a table with
 * more rows than fit in one snapshot can still be searched this way. Falls back to filtering
 * [table]'s already-loaded rows if no runner was published, or if the query fails.
 */
private fun searchTable(databaseName: String, table: io.heimdall.core.DatabaseTable, searchText: String): List<List<String?>> {
    val runner = Heimdall.database.queryRunnerFor(databaseName)
    if (runner == null || table.columns.isEmpty()) return filterRowsLocally(table.rows, searchText)
    val (sql, args) = buildSearchQuery(table.name, table.columns, searchText)
    return runCatching { runner.query(sql, args).rows }.getOrElse { filterRowsLocally(table.rows, searchText) }
}

private fun filterRowsLocally(rows: List<List<String?>>, searchText: String): List<List<String?>> {
    val normalized = searchText.lowercase()
    return rows.filter { row -> row.any { it.orEmpty().lowercase().contains(normalized) } }
}

/** Builds `SELECT * FROM <table> WHERE <col> LIKE ? OR <col> LIKE ? ... LIMIT <limit>`, one bound
 * `?` per column so [searchText] is never concatenated into the SQL itself. `%`/`_`/`\` in
 * [searchText] are escaped so they match literally rather than as LIKE wildcards. */
internal fun buildSearchQuery(tableName: String, columns: List<String>, searchText: String, limit: Int = 200): Pair<String, List<String>> {
    fun quoteIdentifier(name: String) = "\"${name.replace("\"", "\"\"")}\""
    val pattern = "%${searchText.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")}%"
    val whereClause = columns.joinToString(" OR ") { column -> "${quoteIdentifier(column)} LIKE ? ESCAPE '\\'" }
    val sql = "SELECT * FROM ${quoteIdentifier(tableName)} WHERE $whereClause LIMIT $limit"
    return sql to List(columns.size) { pattern }
}

@Composable
private fun TableRowView(
    values: List<String>,
    isHeader: Boolean = false,
    cellWidth: androidx.compose.ui.unit.Dp = 116.dp,
    maxLines: Int = 1,
) {
    Row {
        values.forEach { value ->
            Surface(color = if (isHeader) TableHeader else TableRow) {
                Text(
                    value,
                    color = if (isHeader) HeimdallDesign.onSurface else HeimdallDesign.onCode,
                    fontSize = 11.sp,
                    fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(cellWidth).padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun StorageTab(snapshots: Map<String, io.heimdall.core.StorageSnapshot>) {
    var query by remember { mutableStateOf("") }
    val normalizedQuery = query.trim().lowercase()
    val entries = snapshots.values.mapNotNull { snapshot ->
        val matchingEntries = snapshot.entries.filter { (key, value) ->
            normalizedQuery.isEmpty() ||
                snapshot.sourceName.lowercase().contains(normalizedQuery) ||
                key.lowercase().contains(normalizedQuery) ||
                value.lowercase().contains(normalizedQuery)
        }
        if (matchingEntries.isEmpty()) null else snapshot.copy(entries = matchingEntries)
    }
    if (entries.isEmpty()) {
        Column {
            OutlinedTextField(query, { query = it }, label = { Text("Search storage keys or values") }, singleLine = true)
            EmptyState(if (snapshots.isEmpty()) "No storage sources discovered" else "No matching storage values")
        }
        return
    }

    Column {
        OutlinedTextField(query, { query = it }, label = { Text("Search storage keys or values") }, singleLine = true)
        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
        items(entries) { snapshot ->
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(snapshot.sourceName, color = Color.White, fontWeight = FontWeight.Medium)
                Surface(
                    color = TableRow,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Column {
                        TableRowView(listOf("Key", "Value"), isHeader = true, cellWidth = 180.dp, maxLines = 1)
                        snapshot.entries.forEach { (key, value) ->
                            TableRowView(listOf(key, value), cellWidth = 180.dp, maxLines = Int.MAX_VALUE)
                        }
                    }
                }
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
    var query by remember { mutableStateOf("") }
    val normalizedQuery = query.trim().lowercase()
    val filteredEntries = entries.filter {
        normalizedQuery.isEmpty() ||
            it.tag.lowercase().contains(normalizedQuery) ||
            it.message.lowercase().contains(normalizedQuery) ||
            it.level.name.lowercase().contains(normalizedQuery)
    }
    val filteredCrashes = crashes.filter {
        normalizedQuery.isEmpty() ||
            it.exceptionType.lowercase().contains(normalizedQuery) ||
            it.message.orEmpty().lowercase().contains(normalizedQuery)
    }
    Column {
        OutlinedTextField(query, { query = it }, label = { Text("Search logs") }, singleLine = true)
        if (filteredEntries.isEmpty() && filteredCrashes.isEmpty()) {
            EmptyState(if (entries.isEmpty() && crashes.isEmpty()) "No logs recorded" else "No matching logs")
        } else {
            LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
                items(filteredCrashes) { crash ->
                    LogSurface(
                        title = "${if (crash.isFatal) "FATAL" else "CRASH"}: ${crash.exceptionType}",
                        detail = crash.message ?: "No exception message",
                        accent = HeimdallDesign.error,
                    )
                }
                items(filteredEntries) { entry ->
                    LogSurface(
                        title = "${entry.level.name}  ${entry.tag}",
                        detail = entry.message,
                        accent = if (entry.level == io.heimdall.core.LogLevel.ERROR) HeimdallDesign.error else HeimdallDesign.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LogSurface(title: String, detail: String, accent: Color) {
    Surface(
        color = TableRow,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(detail, color = HeimdallDesign.onSurface, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun FlagsTab(definitions: List<io.heimdall.core.FlagDefinition>) {
    var query by remember { mutableStateOf("") }
    val filteredDefinitions = definitions.filter {
        val value = query.trim().lowercase()
        value.isEmpty() || it.key.lowercase().contains(value) || it.label.lowercase().contains(value)
    }
    if (definitions.isEmpty()) {
        EmptyState("No feature flags registered")
        return
    }

    Column {
        OutlinedTextField(query, { query = it }, label = { Text("Search flags") }, singleLine = true)
        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            item {
                Button(
                    onClick = { Heimdall.flags.resetAll() },
                    modifier = Modifier.padding(bottom = 12.dp),
                ) { Text("Reset overrides") }
                if (Heimdall.flags.restartHandler != null) {
                    Button(
                        onClick = { Heimdall.flags.requestRestart() },
                        modifier = Modifier.padding(bottom = 12.dp),
                    ) { Text("Restart app") }
                }
            }
            items(filteredDefinitions) { flag ->
            val overrideValue = Heimdall.flags.overrideFor(flag.key)
            val currentValue = overrideValue ?: flag.default
            Surface(
                color = HeimdallDesign.surface,
                shape = RoundedCornerShape(HeimdallDesign.corner),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(flag.label, color = HeimdallDesign.onSurface, fontWeight = FontWeight.Bold)
                    Text(flag.key, color = HeimdallDesign.onSurfaceVariant, fontSize = 11.sp)
                    Text(
                        text = "Default: ${flag.default} | ${if (overrideValue != null) "Override: $overrideValue" else "No override"}",
                        color = HeimdallDesign.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )

                    when (val value = currentValue) {
                        is FlagValue.BoolValue -> {
                            TextButton(
                                onClick = { Heimdall.flags.setOverride(flag.key, FlagValue.BoolValue(!value.value)) },
                            ) { Text(if (value.value) "Turn off" else "Turn on") }
                        }
                        is FlagValue.TextValue -> TextFlagEditor(flag.key, value.value)
                        is FlagValue.NumberValue -> NumberFlagEditor(flag.key, value.value)
                    }
                    if (overrideValue != null) {
                        TextButton(onClick = { Heimdall.flags.setOverride(flag.key, null) }) {
                            Text("Reset this flag")
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun TextFlagEditor(key: String, current: String) {
    var draft by remember(key, current) { mutableStateOf(current) }
    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        label = { Text("Override value") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    Button(onClick = { Heimdall.flags.setOverride(key, FlagValue.TextValue(draft)) }) {
        Text("Save text override")
    }
}

@Composable
private fun NumberFlagEditor(key: String, current: Double) {
    var draft by remember(key, current) { mutableStateOf(current.toString()) }
    val parsed = draft.toDoubleOrNull()
    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        label = { Text("Override number") },
        singleLine = true,
        isError = draft.isNotEmpty() && parsed == null,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    Button(
        onClick = { parsed?.let { Heimdall.flags.setOverride(key, FlagValue.NumberValue(it)) } },
        enabled = parsed != null,
    ) { Text("Save number override") }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = Color.White)
    }
}
