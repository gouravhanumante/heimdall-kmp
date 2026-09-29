package io.heimdall.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import io.heimdall.core.LogLevel
import io.heimdall.ui.generated.resources.Res
import io.heimdall.ui.generated.resources.heimdall_horn_logo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource

private data class PanelDestination(val title: String, val icon: ImageVector)

private val panelDestinations = listOf(
    PanelDestination("Overview", Icons.Filled.Home),
    PanelDestination("Network", Icons.Filled.Wifi),
    PanelDestination("Database", Icons.Filled.Storage),
    PanelDestination("Storage", Icons.Filled.Save),
    PanelDestination("Logs", Icons.AutoMirrored.Filled.List),
    PanelDestination("Flags", Icons.Filled.Flag),
    PanelDestination("Sessions", Icons.Filled.History),
)

@Composable
fun HeimdallPanel(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val palette = HeimdallDesign.palette()
    var selectedTab by remember { mutableStateOf(0) }
    var selectedSessionId by remember { mutableStateOf<Long?>(null) }
    var selectedNetworkRecord by remember { mutableStateOf<io.heimdall.core.NetworkRecord?>(null) }
    var selectedDatabaseTable by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedCrash by remember { mutableStateOf<io.heimdall.core.CrashRecord?>(null) }
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
                            fontSize = HeimdallDesign.screenTitleSize,
                        )
                        Text(
                            text = "${Heimdall.currentScreen ?: "Global"}${if (hasCrash) " • crash" else ""}",
                            color = if (hasCrash) palette.error else palette.onSurfaceVariant,
                            fontSize = HeimdallDesign.labelSize,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.semantics { contentDescription = "Close inspector" },
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = palette.onSurfaceVariant, modifier = Modifier.size(HeimdallDesign.iconSize))
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
                                palette = palette,
                                selected = selectedTab == index,
                                onClick = {
                                    selectedTab = index
                                    // Tapping a tab's own icon always lands on that tab's top-level
                                    // view, even if you were already there, drilled into a detail.
                                    when (index) {
                                        1 -> selectedNetworkRecord = null
                                        2 -> selectedDatabaseTable = null
                                        4 -> selectedCrash = null
                                    }
                                },
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (selectedSessionId != null) {
                            val currentIndex = sessions.indexOfFirst { it.id == Heimdall.currentSessionId }
                            val viewedIndex = sessions.indexOfFirst { it.id == selectedSessionId }
                            HistoricalSessionBanner(palette, sessionLabel(viewedIndex, currentIndex)) { selectedSessionId = null }
                        }
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            when (selectedTab) {
                                0 -> OverviewTab(palette, displayedNetwork, displayedCrashes, displayedPerformance) { selectedTab = it }
                                1 -> if (selectedNetworkRecord == null) {
                                    NetworkTab(palette, displayedNetwork) { selectedNetworkRecord = it }
                                } else {
                                    NetworkDetail(palette, selectedNetworkRecord!!) { selectedNetworkRecord = null }
                                }
                                2 -> if (selectedSessionId == null) DatabaseTab(palette, databaseSnapshots, selectedDatabaseTable) { selectedDatabaseTable = it } else HistoryOnlyNotice(palette)
                                3 -> if (selectedSessionId == null) StorageTab(palette, storageSnapshots) else HistoryOnlyNotice(palette)
                                4 -> LogsTab(palette, displayedLogs, displayedCrashes, selectedCrash) { selectedCrash = it }
                                5 -> if (selectedSessionId == null) FlagsTab(palette, flagDefinitions) else HistoryOnlyNotice(palette)
                                6 -> SessionsTab(palette, sessions, selectedSessionId) { selectedSessionId = it }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun HistoricalSessionBanner(palette: HeimdallPalette, sessionLabel: String, onBackToLive: () -> Unit) {
    Surface(
        color = palette.primaryContainer,
        shape = RoundedCornerShape(HeimdallDesign.corner),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable(onClick = onBackToLive),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Viewing ${sessionLabel.replaceFirstChar { it.lowercase() }} — not live",
                color = palette.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = HeimdallDesign.labelSize,
                modifier = Modifier.weight(1f),
            )
            Text("Back to live", color = palette.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = HeimdallDesign.labelSize)
        }
    }
}

@Composable
private fun RailButton(
    destination: PanelDestination,
    palette: HeimdallPalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(bottom = 6.dp)
            .size(40.dp)
            .background(if (selected) palette.primary else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = destination.title },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            destination.icon,
            contentDescription = null,
            tint = if (selected) palette.onPrimary else palette.onSurfaceVariant,
            modifier = Modifier.size(HeimdallDesign.iconSize),
        )
    }
}

@Composable
private fun OverviewTab(
    palette: HeimdallPalette,
    networkRecords: List<io.heimdall.core.NetworkRecord>,
    crashes: List<io.heimdall.core.CrashRecord>,
    performance: List<io.heimdall.core.PerformanceRecord>,
    onNavigate: (Int) -> Unit,
) {
    val health = HealthRules.current(networkRecords, crashes, performance)
    Column {
        SectionTitle("Health", palette)
        health.forEach { HealthMetricRow(palette, it) }
        SectionTitle("Quick access", palette, modifier = Modifier.padding(top = 18.dp))
        Row(modifier = Modifier.padding(top = 8.dp)) {
            listOf(
                PanelDestination("Network", Icons.Filled.Wifi) to 1,
                PanelDestination("Database", Icons.Filled.Storage) to 2,
                PanelDestination("Storage", Icons.Filled.Save) to 3,
                PanelDestination("Logs", Icons.AutoMirrored.Filled.List) to 4,
            ).forEach { (destination, tab) ->
                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(HeimdallDesign.corner),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.padding(end = 6.dp).clickable { onNavigate(tab) },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                        Icon(destination.icon, contentDescription = null, tint = palette.primary, modifier = Modifier.size(HeimdallDesign.smallIconSize))
                        Text(destination.title, color = palette.onSurfaceVariant, fontSize = HeimdallDesign.captionSize, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
        val issueCount = crashes.count { it.isFatal } + networkRecords.count { it.isError } +
            performance.count { it.durationMillis >= 200 }
        if (issueCount > 0) {
            SectionTitle("Needs attention", palette, color = palette.warning, modifier = Modifier.padding(top = 18.dp))
            if (crashes.any { it.isFatal }) IssueSurface(palette, "Fatal crash recorded", palette.error) { onNavigate(4) }
            if (networkRecords.any { it.isError }) IssueSurface(palette, "Failed network request", palette.error) { onNavigate(1) }
            if (performance.any { it.durationMillis >= 200 }) IssueSurface(palette, "Slow operation detected", palette.warning) { onNavigate(0) }
        }
    }
}

@Composable
private fun SectionTitle(text: String, palette: HeimdallPalette, color: Color = palette.onBackground, modifier: Modifier = Modifier) {
    Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = HeimdallDesign.sectionTitleSize, modifier = modifier)
}

@Composable
private fun HealthMetricRow(palette: HeimdallPalette, metric: HealthMetric) {
    val accent = when (metric.status) {
        HealthStatus.OK -> palette.success
        HealthStatus.WARNING -> palette.warning
        HealthStatus.CRITICAL -> palette.error
        HealthStatus.UNAVAILABLE -> palette.onSurfaceVariant
    }
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(HeimdallDesign.corner),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(metric.label, color = palette.onSurface, fontSize = HeimdallDesign.bodySize, modifier = Modifier.weight(1f))
            Text(metric.displayValue, color = accent, fontWeight = FontWeight.Bold, fontSize = HeimdallDesign.bodySize)
            Text(
                text = if (metric.confidence == io.heimdall.core.MetricConfidence.MEASURED) "Measured" else "Unavailable",
                color = palette.onSurfaceVariant,
                fontSize = HeimdallDesign.captionSize,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun IssueSurface(palette: HeimdallPalette, message: String, accent: Color, onClick: () -> Unit) {
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(HeimdallDesign.corner),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clickable(onClick = onClick),
    ) {
        Text(message, color = accent, fontSize = HeimdallDesign.bodySize, modifier = Modifier.padding(10.dp))
    }
}

@Composable
private fun SessionsTab(
    palette: HeimdallPalette,
    sessions: List<io.heimdall.core.Session>,
    selectedSessionId: Long?,
    onSelect: (Long?) -> Unit,
) {
    if (sessions.isEmpty()) {
        EmptyState(palette, "No sessions recorded")
        return
    }

    val currentIndex = sessions.indexOfFirst { it.id == Heimdall.currentSessionId }
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        itemsIndexed(sessions) { index, session ->
            val isViewed = session.id == (selectedSessionId ?: Heimdall.currentSessionId)
            Surface(
                color = if (isViewed) palette.primaryContainer else palette.surface,
                shape = RoundedCornerShape(HeimdallDesign.corner),
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable {
                    onSelect(if (session.id == Heimdall.currentSessionId) null else session.id)
                },
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = if (isViewed) palette.onPrimaryContainer else palette.primary, modifier = Modifier.size(HeimdallDesign.iconSize))
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text(
                            sessionLabel(index, currentIndex),
                            color = if (isViewed) palette.onPrimaryContainer else palette.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = HeimdallDesign.bodySize,
                        )
                        Row {
                            Text(
                                "Started ${formatSessionTimestamp(session.startedAtMillis)}${if (session.crashed) " • crashed" else ""}",
                                color = if (session.crashed) palette.error else if (isViewed) palette.onPrimaryContainer else palette.onSurfaceVariant,
                                fontSize = HeimdallDesign.labelSize,
                            )
                            if (isViewed) {
                                Text(" • viewing", color = palette.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = HeimdallDesign.labelSize)
                            }
                        }
                    }
                }
            }
        }
    }
}


/** "Current session" / "Previous session" / "N sessions ago" instead of a raw, meaningless
 * database id — [index] and [currentIndex] are positions in the newest-first session list. */
private fun sessionLabel(index: Int, currentIndex: Int): String {
    val stepsAgo = if (currentIndex >= 0) index - currentIndex else index + 1
    return when {
        stepsAgo <= 0 -> "Current session"
        stepsAgo == 1 -> "Previous session"
        else -> "$stepsAgo sessions ago"
    }
}

@Composable
private fun HistoryOnlyNotice(palette: HeimdallPalette) {
    EmptyState(palette, "This section shows live state only. Select Current session to view it.")
}

@Composable
private fun TimelineTab(palette: HeimdallPalette, events: List<io.heimdall.core.HeimdallEvent>) {
    if (events.isEmpty()) {
        EmptyState(palette, "No timeline events recorded")
        return
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(events) { event ->
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(event.name, color = palette.onBackground, fontWeight = FontWeight.Medium, fontSize = HeimdallDesign.bodySize)
                event.screen?.let {
                    Text("Screen: $it", color = palette.onSurfaceVariant, fontSize = HeimdallDesign.labelSize, modifier = Modifier.padding(top = 4.dp))
                }
                if (event.attributes.isNotEmpty()) {
                    Text(
                        text = event.attributes.entries.joinToString { (key, value) -> "$key=$value" },
                        color = palette.onSurfaceVariant,
                        fontSize = HeimdallDesign.labelSize,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkTab(
    palette: HeimdallPalette,
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
        EmptyState(palette, "No network calls captured yet")
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
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            itemsIndexed(filtered) { index, record ->
                if (index > 0) HorizontalDivider(color = palette.border, thickness = 1.dp)
                Column(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(record) }.padding(vertical = 10.dp),
                ) {
                    Text(
                        text = record.method,
                        color = palette.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = HeimdallDesign.bodySize,
                    )
                    Text(record.url, color = palette.onSurfaceVariant, fontSize = HeimdallDesign.labelSize, modifier = Modifier.padding(top = 2.dp))
                    Text(
                        text = record.error ?: (record.statusCode?.let { "HTTP $it" } ?: "Pending") +
                            (record.durationMillis?.let { " • ${it} ms" } ?: ""),
                        color = if (record.isError) palette.error else palette.success,
                        fontSize = HeimdallDesign.labelSize,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkDetail(palette: HeimdallPalette, record: io.heimdall.core.NetworkRecord, onBack: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Back to network calls" }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = palette.onSurfaceVariant, modifier = Modifier.size(HeimdallDesign.iconSize))
                }
                Text(
                    "${record.method} ${record.url}",
                    color = palette.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = HeimdallDesign.bodySize,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = if (record.isError) palette.error else palette.success, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        "${record.statusCode ?: "Pending"}",
                        color = palette.onSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = HeimdallDesign.labelSize,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                }
                record.durationMillis?.let {
                    Text("$it ms", color = palette.onSurfaceVariant, fontSize = HeimdallDesign.labelSize, modifier = Modifier.padding(start = 8.dp))
                }
            }
            OutlinedButton(
                onClick = { clipboard.setText(AnnotatedString(record.toCurl())) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.onBackground),
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(HeimdallDesign.smallIconSize))
                Text("Copy cURL", modifier = Modifier.padding(start = 8.dp))
            }
            CopyableCodeBlock(palette, clipboard, "Request headers", record.requestHeaders.entries.joinToString("\n") { "${it.key}: ${it.value}" })
            CopyableCodeBlock(palette, clipboard, "Request body", record.requestBody ?: "No request body")
            CopyableCodeBlock(palette, clipboard, "Response headers", record.responseHeaders.entries.joinToString("\n") { "${it.key}: ${it.value}" })
            CopyableCodeBlock(palette, clipboard, "Response body", record.responseBody ?: record.error ?: "No response body")
        }
    }
}

@Composable
private fun CopyableCodeBlock(palette: HeimdallPalette, clipboard: androidx.compose.ui.platform.ClipboardManager, title: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = palette.onBackground, fontWeight = FontWeight.Medium, fontSize = HeimdallDesign.bodySize, modifier = Modifier.weight(1f))
            IconButton(
                onClick = { clipboard.setText(AnnotatedString(value)) },
                modifier = Modifier.semantics { contentDescription = "Copy $title" },
            ) { Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = palette.onSurfaceVariant, modifier = Modifier.size(HeimdallDesign.smallIconSize)) }
        }
        Surface(
            color = palette.code,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) {
            SelectionContainer {
                Text(
                    value,
                    color = palette.onCode,
                    fontSize = HeimdallDesign.bodySize,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
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
private fun DatabaseTab(
    palette: HeimdallPalette,
    snapshots: List<io.heimdall.core.DatabaseSnapshot>,
    selected: Pair<String, String>?,
    onSelect: (Pair<String, String>?) -> Unit,
) {
    if (snapshots.isEmpty()) {
        EmptyState(palette, "No databases discovered")
        return
    }

    val databaseName = selected?.first
    val table = snapshots.firstOrNull { it.databaseName == databaseName }
        ?.tables?.firstOrNull { it.name == selected?.second }

    if (table != null && databaseName != null) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onSelect(null) }, modifier = Modifier.semantics { contentDescription = "Back to tables" }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = palette.onSurfaceVariant, modifier = Modifier.size(HeimdallDesign.iconSize))
                }
                Text("$databaseName / ${table.name}", color = palette.onSurface, fontWeight = FontWeight.Bold, fontSize = HeimdallDesign.bodySize)
            }
            DatabaseTableGrid(palette, databaseName = databaseName, table = table)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(snapshots.flatMap { snapshot -> snapshot.tables.map { snapshot.databaseName to it } }) { (databaseName, tableItem) ->
            Surface(
                color = palette.surface,
                shape = RoundedCornerShape(HeimdallDesign.corner),
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable {
                    onSelect(databaseName to tableItem.name)
                },
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Storage, contentDescription = null, tint = palette.primary, modifier = Modifier.size(HeimdallDesign.iconSize))
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text(tableItem.name, color = palette.onSurface, fontWeight = FontWeight.Bold, fontSize = HeimdallDesign.bodySize)
                        Text("$databaseName • ${tableItem.rows.size} rows", color = palette.onSurfaceVariant, fontSize = HeimdallDesign.labelSize)
                    }
                }
            }
        }
    }
}

@Composable
private fun DatabaseTableGrid(palette: HeimdallPalette, databaseName: String, table: io.heimdall.core.DatabaseTable) {
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
            color = palette.surface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier.padding(top = 8.dp).horizontalScroll(scrollState),
        ) {
            Column {
                TableRowView(palette, table.columns, isHeader = true)
                filteredRows.forEach { row -> TableRowView(palette, row.map { it ?: "NULL" }) }
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
    palette: HeimdallPalette,
    values: List<String>,
    isHeader: Boolean = false,
    cellWidth: androidx.compose.ui.unit.Dp = 116.dp,
    maxLines: Int = 1,
) {
    Row {
        values.forEach { value ->
            Surface(color = if (isHeader) palette.surfaceVariant else palette.surface) {
                Text(
                    value,
                    color = if (isHeader) palette.onSurface else palette.onCode,
                    fontSize = HeimdallDesign.labelSize,
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
private fun StorageTab(palette: HeimdallPalette, snapshots: Map<String, io.heimdall.core.StorageSnapshot>) {
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
            OutlinedTextField(query, { query = it }, label = { Text("Search storage keys or values") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            EmptyState(palette, if (snapshots.isEmpty()) "No storage sources discovered" else "No matching storage values")
        }
        return
    }

    Column {
        OutlinedTextField(query, { query = it }, label = { Text("Search storage keys or values") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        items(entries) { snapshot ->
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(snapshot.sourceName, color = palette.onBackground, fontWeight = FontWeight.Medium, fontSize = HeimdallDesign.bodySize)
                Surface(
                    color = palette.surface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, palette.border),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Column {
                        TableRowView(palette, listOf("Key", "Value"), isHeader = true, cellWidth = 180.dp, maxLines = 1)
                        snapshot.entries.forEach { (key, value) ->
                            TableRowView(palette, listOf(key, value), cellWidth = 180.dp, maxLines = Int.MAX_VALUE)
                        }
                    }
                }
            }
        }
        }
    }
}

/** Severity color tiers so the eye can jump straight to what matters: fatal/error are the
 * strongest accent, warnings get their own (distinct from error) accent, and info/debug/verbose
 * step down in emphasis rather than all sharing one flat gray. */
@Composable
private fun logLevelAccent(palette: HeimdallPalette, level: io.heimdall.core.LogLevel): Color = when (level) {
    io.heimdall.core.LogLevel.ERROR -> palette.error
    io.heimdall.core.LogLevel.WARN -> palette.warning
    io.heimdall.core.LogLevel.INFO -> palette.onSurface
    io.heimdall.core.LogLevel.DEBUG, io.heimdall.core.LogLevel.VERBOSE -> palette.onSurfaceVariant
}

@Composable
private fun LogsTab(
    palette: HeimdallPalette,
    entries: List<io.heimdall.core.LogEntry>,
    crashes: List<io.heimdall.core.CrashRecord>,
    selectedCrash: io.heimdall.core.CrashRecord?,
    onSelectCrash: (io.heimdall.core.CrashRecord?) -> Unit,
) {
    if (selectedCrash != null) {
        CrashDetail(palette, selectedCrash) { onSelectCrash(null) }
        return
    }

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(LogFilter.ALL) }
    val normalizedQuery = query.trim().lowercase()
    val showCrashes = filter == LogFilter.ALL || filter == LogFilter.CRASHES
    val entryLevel = filter.level

    val filteredEntries = if (filter == LogFilter.CRASHES) {
        emptyList()
    } else {
        entries.filter { entry ->
            (entryLevel == null || entry.level == entryLevel) &&
                (normalizedQuery.isEmpty() ||
                    entry.tag.lowercase().contains(normalizedQuery) ||
                    entry.message.lowercase().contains(normalizedQuery) ||
                    entry.level.name.lowercase().contains(normalizedQuery))
        }
    }
    val filteredCrashes = if (!showCrashes) {
        emptyList()
    } else {
        crashes.filter {
            normalizedQuery.isEmpty() ||
                it.exceptionType.lowercase().contains(normalizedQuery) ||
                it.message.orEmpty().lowercase().contains(normalizedQuery)
        }
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            LogFilter.entries.forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { filter = option },
                    label = { Text(option.label) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        OutlinedTextField(
            query,
            { query = it },
            label = { Text("Search logs") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        if (filteredEntries.isEmpty() && filteredCrashes.isEmpty()) {
            EmptyState(palette, if (entries.isEmpty() && crashes.isEmpty()) "No logs recorded" else "No matching logs")
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                items(filteredCrashes) { crash ->
                    LogSurface(
                        palette = palette,
                        title = "${if (crash.isFatal) "FATAL" else "CRASH"}: ${crash.exceptionType}",
                        detail = crash.message ?: "No exception message",
                        accent = if (crash.isFatal) palette.error else palette.warning,
                        modifier = Modifier.clickable { onSelectCrash(crash) },
                    )
                }
                items(filteredEntries) { entry ->
                    LogSurface(
                        palette = palette,
                        title = "${entry.level.name}  ${entry.tag}",
                        detail = entry.message,
                        accent = logLevelAccent(palette, entry.level),
                    )
                }
            }
        }
    }
}

/** [level] is null for [ALL]/[CRASHES], which aren't a single [io.heimdall.core.LogLevel]. */
private enum class LogFilter(val label: String, val level: io.heimdall.core.LogLevel?) {
    ALL("All", null),
    CRASHES("Crashes", null),
    ERROR("Error", io.heimdall.core.LogLevel.ERROR),
    WARN("Warn", io.heimdall.core.LogLevel.WARN),
    INFO("Info", io.heimdall.core.LogLevel.INFO),
    DEBUG("Debug", io.heimdall.core.LogLevel.DEBUG),
    VERBOSE("Verbose", io.heimdall.core.LogLevel.VERBOSE),
}

@Composable
private fun LogSurface(palette: HeimdallPalette, title: String, detail: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, palette.border),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).then(modifier),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, color = accent, fontWeight = FontWeight.Bold, fontSize = HeimdallDesign.bodySize)
            Text(detail, color = palette.onSurface, fontSize = HeimdallDesign.bodySize, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun CrashDetail(palette: HeimdallPalette, crash: io.heimdall.core.CrashRecord, onBack: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Back to logs" }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = palette.onSurfaceVariant, modifier = Modifier.size(HeimdallDesign.iconSize))
                }
                Text(
                    crash.exceptionType,
                    color = palette.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = HeimdallDesign.bodySize,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = if (crash.isFatal) palette.error else palette.warning, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        if (crash.isFatal) "FATAL" else "CRASH",
                        color = palette.onSuccess,
                        fontWeight = FontWeight.Bold,
                        fontSize = HeimdallDesign.labelSize,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                }
                Text(
                    formatSessionTimestamp(crash.timestampMillis),
                    color = palette.onSurfaceVariant,
                    fontSize = HeimdallDesign.labelSize,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            crash.message?.let {
                Text(it, color = palette.onSurface, fontSize = HeimdallDesign.bodySize, modifier = Modifier.padding(bottom = 14.dp))
            }
            CopyableCodeBlock(palette, clipboard, "Stack trace", crash.stackTraceText)
        }
    }
}


@Composable
private fun FlagsTab(palette: HeimdallPalette, definitions: List<io.heimdall.core.FlagDefinition>) {
    var query by remember { mutableStateOf("") }
    val filteredDefinitions = definitions.filter {
        val value = query.trim().lowercase()
        value.isEmpty() || it.key.lowercase().contains(value) || it.label.lowercase().contains(value)
    }
    if (definitions.isEmpty()) {
        EmptyState(palette, "No feature flags registered")
        return
    }

    Column {
        OutlinedTextField(query, { query = it }, label = { Text("Search flags") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            item {
                Button(
                    onClick = { Heimdall.flags.resetAll() },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                ) { Text("Reset overrides") }
                if (Heimdall.flags.restartHandler != null) {
                    Button(
                        onClick = { Heimdall.flags.requestRestart() },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    ) { Text("Restart app") }
                }
            }
            items(filteredDefinitions) { flag ->
            val overrideValue = Heimdall.flags.overrideFor(flag.key)
            val currentValue = overrideValue ?: flag.default
            Surface(
                color = palette.surface,
                shape = RoundedCornerShape(HeimdallDesign.corner),
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(flag.label, color = palette.onSurface, fontWeight = FontWeight.Bold, fontSize = HeimdallDesign.bodySize)
                    Text(flag.key, color = palette.onSurfaceVariant, fontSize = HeimdallDesign.labelSize)
                    Text(
                        text = "Default: ${flag.default} | ${if (overrideValue != null) "Override: $overrideValue" else "No override"}",
                        color = palette.onSurfaceVariant,
                        fontSize = HeimdallDesign.labelSize,
                        modifier = Modifier.padding(top = 6.dp),
                    )

                    when (val value = currentValue) {
                        is FlagValue.BoolValue -> {
                            TextButton(
                                onClick = { Heimdall.flags.setOverride(flag.key, FlagValue.BoolValue(!value.value)) },
                            ) { Text(if (value.value) "Turn off" else "Turn on") }
                        }
                        is FlagValue.TextValue -> TextFlagEditor(palette, flag.key, value.value)
                        is FlagValue.NumberValue -> NumberFlagEditor(palette, flag.key, value.value)
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
private fun TextFlagEditor(palette: HeimdallPalette, key: String, current: String) {
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
private fun NumberFlagEditor(palette: HeimdallPalette, key: String, current: Double) {
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
private fun EmptyState(palette: HeimdallPalette, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = palette.onSurfaceVariant, fontSize = HeimdallDesign.bodySize)
    }
}
