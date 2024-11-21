package ru.steet.mordsight

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Entity
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.mapview.MapView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.steet.mordsight.ui.theme.MordSightsTheme
import java.io.File
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
MapKitFactory.setApiKey("c87221c5-9dfb-46f6-ac1c-233838cf46d0")
        enableEdgeToEdge()
        setContent {
            MordSightsTheme {
                App()
            }
        }
    }
}

data class NavDest(
    val id: String,
    val icon: @Composable () -> Unit,
    val label: @Composable () -> Unit
)

val destinations = listOf(
    NavDest(
        "home",
        icon = {
            val res = painterResource(R.drawable.outline_home_24)
            Icon(res, null)
        },
        label = {
            Text(stringResource(R.string.home_page_navbar_button))
        },
    ),
    NavDest(
        "allsights",
        icon = {
            val res = painterResource(R.drawable.outline_list_alt_24)
            Icon(res, null)
        },
        label = {
            Text(stringResource(R.string.all_sights_page_navbar_button))
        },
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val ctx = LocalContext.current
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    var currentSight by remember { mutableStateOf<SightModel?>(null) }
    val viewModel: SightsViewModel = viewModel()

    val query by viewModel.query.collectAsState()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val file = File(ctx.filesDir, "data.json")
            if (!file.exists()) {
                file.createNewFile()
            }
            file.writeText("""
                []
            """.trimIndent())
            viewModel.loadSights(file)
        }
    }

    if (!viewModel.isSightsLoaded()) {
        CircularProgressIndicator(Modifier.fillMaxSize())
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        if (currentSight != null) {
                            // null check в котлине и на джетпаке просто шикарен
                            currentSight?.name?.let { Text(it) }
                        } else {
                            SearchBar(
                                inputField = {
                                    OutlinedTextField(
                                        query,
                                        {
                                            viewModel.setSearchQuery(it)
                                            viewModel.filterSights()
                                            if (navController.currentBackStackEntry?.id != "allsights") {
                                                navController.navigate("allsights")
                                            }
                                        },
                                        placeholder = {Text(stringResource(R.string.app_tittle))},
                                        shape = MaterialTheme.shapes.large,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedBorderColor = Color.Transparent
                                        )
                                    )
                                },
                                expanded = false,
                                onExpandedChange = {},

                                modifier = Modifier.fillMaxSize()
                            ) {}
                        }
                    },
                    navigationIcon = {
                        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                            if (currentBackStackEntry != null && navController.currentDestination?.route !in destinations.map { it.id }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.scale(1.5f).clickable { navController.popBackStack() })
                            } else {
                                val res = painterResource(R.drawable.ic_launcher_foreground)
                                Icon(res, null)
                            }
                        }
                    },
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    destinations.forEach { dest ->
                        NavigationBarItem(
                            currentBackStackEntry!=null && dest.id == navController.currentDestination?.route,
                            onClick = {
                                navController.navigate(dest.id)
                            },
                            dest.icon,
                            label = dest.label
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController,
                startDestination = "home",
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                composable("home") {
                    HomePage(
                        viewModel,
                        navController
                    ) {
                        currentSight = it
                        navController.navigate("sample")
                    }
                }
                composable("allsights") {
                    AllSightsScreen(viewModel, navController) {
                        currentSight = it
                        navController.navigate("sample")
                    }
                }
                composable("sample") {
                    if (currentSight != null)
                        SightDetails(currentSight!!, navController)
                    else
                        CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun AllSightsScreen(
    viewModel: SightsViewModel,
    navController: NavHostController,
    setCurrentSight: (SightModel) -> Unit
) {
    val sights by viewModel.filteredSights.collectAsState()
    LazyColumn {
        itemsIndexed(sights) { idx, item ->
            SightPreviewCard(item.name, item.brief, item.images) { setCurrentSight(item) }
        }
    }
}

@Composable
fun HomePage(
    viewModel: SightsViewModel,
    navController: NavController,
    setCurrentSight: (SightModel) -> Unit
) {
    val sightOfTheDay = viewModel.getDailySight()
    val allSights by viewModel.randomSights.collectAsState()
    LazyColumn {
        item {
            Text(
                stringResource(R.string.sight_of_the_day),
                modifier = Modifier.padding(vertical = 16.dp),
                style = MaterialTheme.typography.displaySmall
            )
        }
        item {
            OutlinedCard({ setCurrentSight(sightOfTheDay) }) {
                Column(
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    val image = sightOfTheDay.images.first()
                    val painter = rememberAsyncImagePainter(image)
                    val painterState by painter.state.collectAsState()
                    val nonloadRes = painterResource(R.drawable.ic_launcher_foreground)
                    val modifier = Modifier
                        .padding(
                            top = 16.dp, start = 16.dp, end = 16.dp, bottom = 4.dp
                        )
                        .clip(MaterialTheme.shapes.large)
                    when (painterState) {
                        is AsyncImagePainter.State.Loading -> CircularProgressIndicator()
                        is AsyncImagePainter.State.Success -> Image(painter, null, modifier)
                        else -> Image(nonloadRes, null)
                    }
                    Text(
                        sightOfTheDay.name+"\n"+(sightOfTheDay.brief?:""),
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }
        item {
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
        }
        itemsIndexed(allSights) { idx, item ->
            SightPreviewCard(
                item.name,
                item.brief,
                item.images.first()
            ) {
                setCurrentSight(item)
            }
        }
    }
}

@Composable
fun SightPreviewCard(
    title: String,
    brief: String? = null,
    image: Any? = null,
    onClick: () -> Unit
) {
    OutlinedCard (onClick, Modifier.fillMaxWidth().padding(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (image != null) {
                Box(
                    Modifier.size(96.dp)
                ) {
                    if (image !is Painter) {
                        val painter = rememberAsyncImagePainter(image)
                        val painterState by painter.state.collectAsState()
                        val nonloadRes = painterResource(R.drawable.ic_launcher_foreground)
                        when (painterState) {
                            is AsyncImagePainter.State.Empty -> Image(nonloadRes, null)
                            is AsyncImagePainter.State.Loading -> CircularProgressIndicator()
                            is AsyncImagePainter.State.Error -> Image(nonloadRes, null)
                            is AsyncImagePainter.State.Success -> Image(painter, null)
                        }
                    } else {
                        Image(image, null)
                    }
                }
            } else {
                Box(Modifier.padding(start = 32.dp).padding(vertical = 48.dp)) {}
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                if (brief != null) {
                    Text(brief)
                }
            }
        }
    }
}

class SightsViewModel : ViewModel() {
    private val _allSights = MutableStateFlow(listOf<SightModel>())
    val allSights = _allSights.asStateFlow()

    private val _randomSights = MutableStateFlow(listOf<SightModel>())
    val randomSights = _randomSights.asStateFlow()

    private val _filteredSights = MutableStateFlow(listOf<SightModel>())
    private val _query = MutableStateFlow("")
    val filteredSights = _filteredSights.asStateFlow()
    val query = _query.asStateFlow()

    fun loadSights(srcFile: File) {
        _allSights.update {
            val jsonText = srcFile.readText()
            Json.decodeFromString<List<SightModel>>(jsonText)
        }
        reloadDailySights()
    }

    fun reloadDailySights() {
        _randomSights.update {
            getRandomDailySights()
        }
    }

    fun isSightsLoaded() = _allSights.value.isNotEmpty()

    fun getSightById(id: String) =
        allSights.value.find { it.id == id }

    fun getDailySight(): SightModel {
        val date = Date()
        val fac = date.day.floorDiv(_allSights.value.size)
        val sightOfTheDay = _allSights.value[date.day.floorDiv(fac)]
        return sightOfTheDay
    }

    private fun getRandomDailySights(count: Int = 3): List<SightModel> {
        return _allSights.value.shuffled().take(count)
    }

    fun setSearchQuery(searchQuery: String) {
        _query.update { searchQuery }
    }

    fun filterSights() {
        val q = query.value.lowercase()
        _filteredSights.update { _allSights.value.filter {
            it.name.lowercase().contains(q) || it.brief?.lowercase()?.contains(q) == true
        } }
    }
}

@Serializable
data class SightModel(
    val id: String,
    val name: String,
    val images: List<String> = listOf(),
    val brief: String? = null,
    val description: String? = null,
    val links: List<Links> = listOf()
)

@Serializable
data class Links(
    val icon: LinkType,
    val label: String,
    val action: String,
) {
    enum class LinkType {
        Info, Route, Ticket
    }
}

val sights = listOf(
    SightModel(
        "aaa",
        "Aaa -- aaa",
        listOf(
            "https://developer.android.com/static/codelabs/basic-android-kotlin-compose-viewmodel-and-state/img/61eb7bcdcff42227.png",
            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQcGEdl9U61Xj6GTBOmpe3yRW1haMy7zOoXRw&s"
        ),
        ("Some of aaa"),
        ("Aaa is the one of oldest Mordovia sights"),
        listOf(
            Links(
                Links.LinkType.Route,
                "Route",
                "https://example.com"
            )
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SightDetails(sightModel: SightModel, navController: NavController) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                sightModel.name,
                modifier = Modifier.padding(vertical = 16.dp),
                style = MaterialTheme.typography.headlineLarge
            )
        }
        item {
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val carouselState = rememberCarouselState { sightModel.images.size }
                HorizontalMultiBrowseCarousel(
                    state = carouselState,
                    preferredItemWidth = 400.dp,
                    modifier = Modifier.clip(MaterialTheme.shapes.large)
                ) {
                    val image = sightModel.images[it]
                    val painter = rememberAsyncImagePainter(image)
                    val painterState by painter.state.collectAsState()
                    val nonloadRes = painterResource(R.drawable.ic_launcher_foreground)
                    when (painterState) {
                        is AsyncImagePainter.State.Loading -> CircularProgressIndicator()
                        is AsyncImagePainter.State.Success -> Image(painter, null)
                        else -> Image(nonloadRes, null)
                    }
                }
            }
        }

        item {
            Text(
                sightModel.description?:(""),
                modifier = Modifier.padding(vertical = 32.dp)
            )
        }

        itemsIndexed(sightModel.links) { idx, item ->
            val ctx = LocalContext.current
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedCard ({
                    val openIntent = Intent(Intent.ACTION_VIEW)
                    openIntent.setData(Uri.parse(item.action))
                    ctx.startActivity(openIntent)
                }, modifier = Modifier.fillMaxWidth(.6f)) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            when (item.icon) {
                                Links.LinkType.Info -> Icons.Default.Info
                                Links.LinkType.Route -> Icons.Default.Place
                                Links.LinkType.Ticket -> Icons.Default.MailOutline
                            }, null
                        )
                        Text(item.label)
                    }
                }
            }
        }
    }
}
