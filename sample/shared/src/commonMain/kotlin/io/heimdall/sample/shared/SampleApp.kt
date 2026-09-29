package io.heimdall.sample.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Shapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.ImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.heimdall.core.DatabaseInspector
import io.heimdall.core.DatabaseSnapshot
import io.heimdall.core.DatabaseTable
import io.heimdall.core.FlagDefinition
import io.heimdall.core.FlagValue
import io.heimdall.core.Heimdall
import io.heimdall.core.LogLevel
import io.heimdall.network.ktor.HeimdallKtor
import io.heimdall.ui.HeimdallOverlay
import io.heimdall.ui.HeimdallOverlayController
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

private enum class SampleScreen(val title: String, val glyph: String) {
    HOME("Home", "⌂"),
    DETAILS("Details", "□"),
    NETWORK("Network", "↗"),
    FEED("Feed", "▤"),
    DATABASE("Database", "▦"),
    STORAGE("Storage", "▥"),
    LOGS("Logs", "≡"),
}

private data class FeedItem(val id: Int, val title: String, val imageUrl: String)

private val SampleColors = lightColorScheme(
    primary = Color(0xFF9A5A00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0A3),
    onPrimaryContainer = Color(0xFF321A00),
    secondary = Color(0xFF176B8C),
    onSecondary = Color.White,
    surface = Color(0xFFF1F4EF),
    onSurface = Color(0xFF17212B),
    outline = Color(0xFF6E7C86),
)

@Composable
fun SampleApp(overlayController: HeimdallOverlayController, showOverlay: Boolean = true) {
    val client = remember { HttpClient { install(HeimdallKtor) } }
    val database = remember { SampleDatabaseInspector() }
    var screen by remember { mutableStateOf(SampleScreen.DETAILS) }

    DisposableEffect(client, database) {
        Heimdall.database.attach(database)
        Heimdall.flags.register(FlagDefinition("new_feed", "New feed", FlagValue.BoolValue(true)))
        onDispose { client.close() }
    }

    DisposableEffect(screen) {
        Heimdall.setCurrentScreen(screen.title)
        Heimdall.event("Screen opened")
        onDispose { Heimdall.setCurrentScreen(null) }
    }

    SampleBackHandler(enabled = true) {
        if (!overlayController.handleBack() && screen != SampleScreen.HOME) {
            screen = SampleScreen.HOME
        }
    }

    MaterialTheme(
        colorScheme = SampleColors,
        typography = Typography(),
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp),
        ),
    ) {
        val appContent: @Composable () -> Unit = {
            Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F4EF))) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 12.dp, top = 28.dp, bottom = 16.dp),
                ) {
                    if (screen != SampleScreen.HOME) {
                        TextButton(onClick = { screen = SampleScreen.HOME }) { Text("← Home") }
                    }
                    Text(
                        text = if (screen == SampleScreen.HOME) "Heimdall Sample" else screen.title,
                        fontSize = 28.sp,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                when (screen) {
                    SampleScreen.HOME -> HomeScreen { screen = it }
                    SampleScreen.DETAILS -> DetailsScreen()
                    SampleScreen.NETWORK -> NetworkScreen(client)
                    SampleScreen.FEED -> FeedScreen(client)
                    SampleScreen.DATABASE -> DatabaseScreen(database)
                    SampleScreen.STORAGE -> StorageScreen()
                    SampleScreen.LOGS -> LogsScreen()
                }
            }
        }
        if (showOverlay) {
            HeimdallOverlay(controller = overlayController, content = appContent)
        } else {
            appContent()
        }
    }
}

@Composable
private fun HomeScreen(onOpen: (SampleScreen) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Choose a sample flow", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.size(20.dp))
        SampleScreen.values().filter { it != SampleScreen.HOME }.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(bottom = 18.dp)) {
                row.forEach { destination -> SampleOption(destination) { onOpen(destination) } }
            }
        }
    }
}

@Composable
private fun SampleOption(destination: SampleScreen, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(88.dp).background(Color.White, RoundedCornerShape(16.dp)).clickable(onClick = onClick)
            .semantics { contentDescription = destination.title },
        contentAlignment = Alignment.Center,
    ) { Text(destination.glyph, fontSize = 30.sp, color = Color(0xFF168CC0)) }
}

@Composable
private fun NetworkScreen(client: HttpClient) {
    var response by remember { mutableStateOf("No request yet") }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Real API call", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("This request is captured by HeimdallKtor.", modifier = Modifier.padding(top = 8.dp))
        Button(onClick = {
            response = "Loading..."
            scope.launch {
                samplePlatformLog("SampleApi", "GET /todos/1 started")
                try {
                    response = client.get("https://jsonplaceholder.typicode.com/todos/1").bodyAsText()
                    samplePlatformLog("SampleApi", "GET /todos/1 completed: $response")
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Throwable) {
                    response = "Request failed: ${failure.message}"
                    samplePlatformLog("SampleApi", "GET /todos/1 failed: ${failure.message}")
                }
            }
        }, modifier = Modifier.padding(top = 16.dp)) { Text("Call API") }
        Text(response, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun DetailsScreen() {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("28") }
    var height by remember { mutableStateOf("1.75") }
    var points by remember { mutableStateOf("100") }
    var notifications by remember { mutableStateOf(true) }
    var saved by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Typed storage", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        OutlinedTextField(name, { name = it }, label = { Text("Name (text)") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        OutlinedTextField(age, { age = it }, label = { Text("Age (int)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(height, { height = it }, label = { Text("Height (float)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        OutlinedTextField(points, { points = it }, label = { Text("Points (long)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Text("Notifications (boolean)", modifier = Modifier.weight(1f))
            Switch(checked = notifications, onCheckedChange = { notifications = it })
        }
        Button(onClick = {
            val writer = Heimdall.storage.writerFor("sample_preferences")
            val writes = listOf(
                writer?.write("last_action", name),
                writer?.write("age", age),
                writer?.write("height", height),
                writer?.write("points", points),
                writer?.write("notifications", notifications.toString()),
            )
            saved = writer != null && writes.all { it == true }
            Heimdall.event("Details saved", mapOf("storage" to "sample_preferences"))
        }, modifier = Modifier.padding(top = 12.dp)) { Text("Save details") }
        if (saved) Text("Saved to SharedPreferences", color = Color(0xFF168C5B), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
@OptIn(coil3.annotation.ExperimentalCoilApi::class)
private fun FeedScreen(client: HttpClient) {
    var feedItems by remember { mutableStateOf(emptyList<FeedItem>()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Same client HeimdallKtor is installed on, so feed images show up in Heimdall.network too.
    val platformContext = LocalPlatformContext.current
    val imageLoader = remember(client) {
        ImageLoader.Builder(platformContext)
            .components { add(KtorNetworkFetcherFactory(client)) }
            .build()
    }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Feed and image URLs", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Loads remote feed data and image URLs into a list.", modifier = Modifier.padding(top = 8.dp))
        Button(onClick = {
            loading = true
            scope.launch {
                val body = runCatching { client.get("https://jsonplaceholder.typicode.com/photos?_limit=6").bodyAsText() }.getOrNull()
                feedItems = (1..6).map { id -> FeedItem(id, "Remote photo item $id", "https://picsum.photos/id/$id/120/80") }
                if (body == null) Heimdall.log(LogLevel.ERROR, "Feed", "Feed request failed")
                loading = false
            }
        }, modifier = Modifier.padding(top = 16.dp)) { Text(if (loading) "Loading..." else "Load feed") }
        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            items(feedItems) { item ->
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        imageLoader = imageLoader,
                        modifier = Modifier.size(width = 120.dp, height = 80.dp),
                    )
                    Text(item.title, fontWeight = FontWeight.Medium)
                    Text(item.imageUrl, fontSize = 12.sp, color = Color(0xFF4B6472))
                }
            }
        }
    }
}

@Composable
private fun DatabaseScreen(database: SampleDatabaseInspector) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Live database", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Rows are published through DatabaseInspector.", modifier = Modifier.padding(top = 8.dp))
        Button(onClick = {
            database.addRow()
            Heimdall.database.refresh(database.databaseName)
            Heimdall.event("Database row inserted")
        }, modifier = Modifier.padding(top = 16.dp)) { Text("Insert row") }
        Text("Rows: ${database.rowCount}", modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun StorageScreen() {
    var value by remember { mutableStateOf("not written") }
    Button(onClick = {
        value = "written in ${Heimdall.currentScreen}"
        val written = Heimdall.storage.writerFor("sample_preferences")?.write("last_action", value) == true
        if (!written) Heimdall.log(LogLevel.WARN, "Storage", "sample_preferences was not discovered")
        Heimdall.event("Storage value changed")
    }, modifier = Modifier.padding(24.dp)) { Text("Write storage value") }
}

@Composable
private fun LogsScreen() {
    Button(onClick = { Heimdall.log(LogLevel.INFO, "Sample", "Button tapped") }, modifier = Modifier.padding(24.dp)) {
        Text("Write log entry")
    }
}

private class SampleDatabaseInspector : DatabaseInspector {
    private val connection = BundledSQLiteDriver().open(":memory:")

    init {
        connection.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT NOT NULL, status TEXT NOT NULL)")
        connection.execSQL("INSERT INTO users (id, name, status) VALUES (1, 'Ada', 'active')")
        connection.execSQL("CREATE TABLE profiles (user_id INTEGER PRIMARY KEY, city TEXT NOT NULL, plan TEXT NOT NULL)")
        connection.execSQL("INSERT INTO profiles (user_id, city, plan) VALUES (1, 'London', 'pro')")
    }

    override val databaseName: String = "sample.db"
    val rowCount: Int
        get() {
            val statement = connection.prepare("SELECT COUNT(*) FROM users")
            return try {
            statement.step()
            statement.getLong(0).toInt()
            } finally {
                statement.close()
            }
        }

    fun addRow() {
        val id = rowCount + 1
        val statement = connection.prepare("INSERT INTO users (id, name, status) VALUES (?, ?, ?)")
        try {
            statement.bindLong(1, id.toLong())
            statement.bindText(2, "User $id")
            statement.bindText(3, "active")
            statement.step()
        } finally {
            statement.close()
        }
    }

    override fun snapshot(): DatabaseSnapshot = DatabaseSnapshot(
        databaseName,
        listOf(readUsers(), readProfiles()),
    )

    override fun query(sql: String, args: List<String>): DatabaseTable = readUsers()

    private fun readUsers(): DatabaseTable {
        val statement = connection.prepare("SELECT id, name, status FROM users ORDER BY id")
        return try {
            DatabaseTable(
            name = "users",
            columns = listOf("id", "name", "status"),
            rows = buildList {
                while (statement.step()) {
                    add(listOf(statement.getLong(0).toString(), statement.getText(1), statement.getText(2)))
                }
            },
            )
        } finally {
            statement.close()
        }
    }

    private fun readProfiles(): DatabaseTable {
        val statement = connection.prepare("SELECT user_id, city, plan FROM profiles ORDER BY user_id")
        return try {
            DatabaseTable(
                name = "profiles",
                columns = listOf("user_id", "city", "plan"),
                rows = buildList {
                    while (statement.step()) {
                        add(listOf(statement.getLong(0).toString(), statement.getText(1), statement.getText(2)))
                    }
                },
            )
        } finally {
            statement.close()
        }
    }
}
