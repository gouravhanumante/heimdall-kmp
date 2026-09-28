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
    var selectedNetworkRecord by remember { mutableStateOf<io.heimdall.core.NetworkRecord?>(null) }
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
                        modifier = Modifier.size(32.dp),
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
                            0 -> OverviewTab(networkRecords, logs, crashes, events, performance) { selectedTab = it }
                            1 -> if (selectedNetworkRecord == null) {
                                NetworkTab(networkRecords) { selectedNetworkRecord = it }
                            } else {
                                NetworkDetail(selectedNetworkRecord!!) { selectedNetworkRecord = null }
                            }
                            2 -> DatabaseTab(databaseSnapshots)
                            3 -> StorageTab(storageSnapshots)
                            4 -> LogsTab(logs, crashes)
                            5 -> FlagsTab(flagDefinitions)
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
    onNavigate: (Int) -> Unit,
) {
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
        performance.maxByOrNull { it.durationMillis }?.let { slowest ->
            Text(
                "Slowest: ${slowest.name} (${slowest.durationMillis} ms)",
                color = HeimdallDesign.warning,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
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
            performance.count { it.durationMillis >= 200 }
        if (issueCount > 0) {
            Text("Needs attention", color = HeimdallDesign.warning, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
            if (crashes.any { it.isFatal }) IssueSurface("Fatal crash recorded", HeimdallDesign.error) { onNavigate(4) }
            if (networkRecords.any { it.isError }) IssueSurface("Failed network request", HeimdallDesign.error) { onNavigate(1) }
            if (performance.any { it.durationMillis >= 200 }) IssueSurface("Slow operation detected", HeimdallDesign.warning) { onNavigate(0) }
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
    val table = snapshots.firstOrNull { it.databaseName == selectedDatabase }
        ?.tables?.firstOrNull { it.name == selectedTable }

    if (table != null && selectedDatabase != null) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedTable = null }, modifier = Modifier.semantics { contentDescription = "Back to tables" }) {
                    Text("←", color = HeimdallDesign.onSurfaceVariant, fontSize = 22.sp)
                }
                Text("$selectedDatabase / ${table.name}", color = HeimdallDesign.onSurface, fontWeight = FontWeight.Bold)
            }
            DatabaseTableGrid(table)
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
private fun DatabaseTableGrid(table: io.heimdall.core.DatabaseTable) {
    var query by remember { mutableStateOf("") }
    val filteredRows by produceState(initialValue = table.rows, query, table.rows) {
        value = withContext(Dispatchers.Default) {
            val normalized = query.trim().lowercase()
            if (normalized.isEmpty()) {
                table.rows
            } else {
                table.rows.filter { row -> row.any { it.orEmpty().lowercase().contains(normalized) } }
            }
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
    if (filteredEntries.isEmpty() && filteredCrashes.isEmpty()) {
        EmptyState("No logs recorded")
        return
    }

    Column {
        OutlinedTextField(query, { query = it }, label = { Text("Search logs") }, singleLine = true)
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
                        is FlagValue.TextValue -> Text(
                            text = "Text value: ${value.value}",
                            color = HeimdallDesign.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        is FlagValue.NumberValue -> Text(
                            text = "Number value: ${value.value}",
                            color = HeimdallDesign.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
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
