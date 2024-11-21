package ru.steet.mordsight

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
    var recompose by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val file = File(ctx.filesDir, "data.json")
            if (!file.exists()) {
                file.createNewFile()
            }
            file.writeText(srcsOFff)
            viewModel.loadSights(file)
            Log.d("TAGGGGGGG", "PPPP")
            recompose += "1"
        }
    }

    if (!viewModel.isSightsLoaded()) {
        CircularProgressIndicator(Modifier.fillMaxSize())
        Text(recompose)
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
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.scale(1.5f).clickable { navController.popBackStack();currentSight=null })
                            } else {
                                val res = painterResource(R.drawable.icon)
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
    val allllSights by viewModel.allSights.collectAsState()
    LazyColumn {
        itemsIndexed(if(sights.isEmpty()) allllSights else sights) { idx, item ->
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
        val fac = date.day.floorDiv(_allSights.value.size+1)+1
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

val srcsOFff = """
[
	{
		"id": "Музей боевого и трудового подвига в Саранске",
		"name": "Музей боевого и трудового подвига в Саранске",
		"images": ["https://avatars.mds.yandex.net/i?id=e76362d049256db79fbea8cc9a8d3285666fa62a-13013698-images-thumbs&n=13", "https://avatars.mds.yandex.net/i?id=02792cd6ddb9e434b0b1cc9de7bb845d1b726cc9-3481141-images-thumbs&n=13", "https://avatars.mds.yandex.net/i?id=8f1e9f860de3d1e406786ab4110e48fedbee5d39-3492565-images-thumbs&n=13", "https://avatars.mds.yandex.net/i?id=81fb642f43ddb26e4f299f6d61561e683eb6add4-5232252-images-thumbs&n=13"],
		"brief": "Музей на площади Победы в Мордовии.",
		"description": "Музей входит в состав мемориального комплекса на площади Победы и основан в 1995-м году. Здание выглядит достаточно оригинально: в плане оно очертаниями напоминает республику Мордовию на карте, а стены выложены из черно-оранжевой плитки, навевая ассоциации с Георгиевской лентой. Музейные фонды хранят свыше 40 тыс. предметов. Экспозиция включает военные реликвии и награды, форму, оружие, личные письма, вещи и фотографии солдат. Также в нее входят предметы искусства на военную тематику. Действует выставка образцов военной техники, в том числе танк Т-34 и зенитная пушка М-1. ",
		"links": [{"icon":"Info","label":":","action":"https://saransk.kassir.ru/muzei/mbuk-memorialnyiy-muzey-v-i-tp-1941-1945gg"}]
	},
	{
		"id": "Дом-музей «Этно-кудо» имени В. И. Ромашкина",
		"name": "Дом-музей «Этно-кудо» имени В. И. Ромашкина",
		"images": ["https://avatars.mds.yandex.net/i?id=8a2355017a2076ef6b0b205b1d946a0668a6e5f7-9028838-images-thumbs&n=13","https://avatars.mds.yandex.net/i?id=db83ac4c60e67955ef2348e1b24ee2407bfb9cc029d8ea03-12541995-images-thumbs&n=13", "https://avatars.mds.yandex.net/i?id=ee2161090ec1c04ff77e0fec8156700d15f02698-10471642-images-thumbs&n=13", "https://avatars.mds.yandex.net/i?id=6044358481293a7800e1aee323063b72d27cf405-9152791-images-thumbs&n=13"],
		"brief": "Музей в селе Подлесная Тавла.",
		"description": "Открыт в 2006 году в селе Подлесная Тавла. Посвящён фольклористу Ромашкину и его многолетнему труду. Часть комнат отведено под личные вещи исследователя, также являвшегося основателем фолк-коллектива «Торама», а часть – под этно-коллекцию. Музей принимает ежегодный фестиваль, где исполняют песни местных народностей. С экспозицией можно ознакомиться круглый год, за исключением выходных дней – понедельников."
		"links": [{"icon":"Info","label":":","action":"https://yandex.ru/maps/org/etno_kudo/1737083537/?ll=45.487282%2C54.098024&z=16"}]
	},
	{
		"id": "Национальный парк «Смольный»",
		"name": "Национальный парк «Смольный»",
		"images": ["https://avatars.mds.yandex.net/i?id=b6492aa25ee29480b274bd3e03822cb01a8dd0970ee952bd-11920176-images-thumbs&n=13", "https://avatars.mds.yandex.net/i?id=ff16305878225593ef6b042ed7545328f452911c-5235366-images-thumbs&n=13", "https://avatars.mds.yandex.net/i?id=7e0820e1fcaf9d5f33d448870c1044cd430be18f-5277129-images-thumbs&n=13", "https://avatars.mds.yandex.net/i?id=833466710666e0b1ab29de86bd07c69b8842f9bb-13094487-images-thumbs&n=13"],
		"brief": "Национальный парк.",
		"description": "Основан в 1995 году и занимает площадь в 35,5 тысяч га по левому берегу реки Алатырь. В лесном массиве и на пойме гнездятся редкие птицы, эта территория относится к особо значимой для орнитологов мира. Леса здесь как смешанные, так и сосновые, а также лиственные. Луга по большей части пойменные. Национальный парк несёт просветительскую функцию, поэтому тут проложены экомаршруты и проводятся экскурсии.",
		"links": [{"icon":"Info","label":":","action":"https://zapoved-mordovia.ru/ru/online-payment.html?ysclid=m3rdw3w5sx999704013"}]
	},
	{
		"id": "Пайгармский Параскево-Вознесенский монастырь",
		"name": "Пайгармский Параскево-Вознесенский монастырь",
		"images": ["https://avatars.mds.yandex.net/i?id=a85f8501f9df170192a358cdd884429a9c58bfb8-12421995-images-thumbs&n=13", "https://avatars.mds.yandex.net/i?id=c398965c74017b3091575f67630a013afcdb46507e23e49c-12941205-images-thumbs&n=13"],
		"brief": "Обитель в селе Пайгарма.",
		"description": "Обитель представляет собой архитектурный памятник XVIII в., расположенный в селе Пайгарма. Женский монастырь основан на месте, где, как считается, произошло чудо – явление иконы мученицы Параскевы. Икона была написана в XIX в. и по сей день хранится в обители, составляя одну из главных ее святынь. Считается также, что она покровительствует торговцам. На территории монастыря есть родники со святой целебной водой. Сюда приходят для лечения проблем со зрением, а также бесплодия. Святая обитель находится в красивой местности, в окружении лесов и озер. ",
		"links": [{"icon":"Info","label":":","action":"https://gulfstream64.ru/pajgarma-makarovka-ekskursiya-po-monastyryam-mordovii/?ysclid=m3re005xhw271857768"}]
	},
	{
		"id": "Церковь Николая Чудотворца",
		"name": "Церковь Николая Чудотворца",
		"images": ["https://avatars.mds.yandex.net/i?id=89a3581eeeb9bfabc9837c505c61ec66bfc031be-13529499-images-thumbs&n=13", "https://avatars.mds.yandex.net/i?id=4f7770a32dbe3e9f3497983a08c84dbc93cd8671-11409613-images-thumbs&n=13"],
		"brief": "Каменная церковь Николая Чудотворца в Посопе",
		"description": "Каменная церковь Николая Чудотворца в Посопе построена в 1897-1906 году вместо каменного здания XVIII века. Первоначально главный престол Петропавловский, придел Никольский. Церковь закрыта в 1937 году. Здание храма использовалась под склад, отдел культуры, музей. Никольская церковь возвращена верующим в 1990 году.",
		"links": [{"icon":"Info","label":":","action":"https://yandex.ru/maps/org/tserkov_nikolaya_chudotvortsa/1297805069/?ll=45.224722%2C54.185720&z=16"}]
	},
	{
		"id": "Мордовский государственный природный заповедник им. П.Г. Смидовича",
		"name": "Заповедник",
		"images": ["https://avatars.mds.yandex.net/i?id=085b7ede509fa64c3b75a18b06c7d71b7ff3ff14-12529607-images-thumbs&n=13", "https://avatars.mds.yandex.net/i?id=4d75e738616dc81a22a5030eb47332f1-4054908-images-thumbs&n=13"],
		"brief": "...",
		"description": "В этом заповеднике можно увидеть сочетание различных географических зон (таежных и широколиственных лесов и лесостепи), в которых расположен заповедник, обуславливает многообразие животного и растительного мира. Множество редких растений, грибов и животных встречается в Мордовском заповеднике, в том числе орхидеи венерин башмачок настоящий, неоттианта клобучковая, редчайшие лишайники лобария легочная и менегация пробуравленная, гриб-баран, красивейшая бабочка аполлон, симпатичные перепончатокрылые пчела-плотник и парнопес, могучие хищные птицы орлан-белохвост, большой подорлик, грациозный черный аист, реликтовое животное русская выхухоль и другие виды, занесенные в Красную книгу Российской Федерации. Леса Мордовского заповедника являются убежищем копытных и хищных животных — лося, оленя, кабана, куницы, рыси, бурого медведя, волка, лисицы.",
		"links": [{"icon":"Info","label":":","action":"https://turizmrm.ru/what-to-visit/nature/natural-objects/nature-reserve-named-smidovich"}]
	},
	{
		"id": "Дом-музей Ф. В. Сычкова",
		"name": "Дом-музей Ф. В. Сычкова",
		"images": ["https://avatars.mds.yandex.net/i?id=7b18d5be1390bb85430f60b446bf2253b5fcc96bd79cd015-11406357-images-thumbs&n=13", "https://avatars.mds.yandex.net/i?id=f9da6bc82b26073c56b2bfa03bd33d92d6533165-11467820-images-thumbs&n=13"],
		"brief": "Музей искусств",
		"description": "Располагается в селе Кочелаево с 1970 года. Экспозиция состоит из личных вещей художника, атрибутов и убранства, свойственных домам того времени, а также из работ мастера. Все комнаты восстанавливались по воспоминаниям супруги Сычкова, некоторые предметы обихода предоставлены его семьёй. Отдельной коллекцией идут награды, полученные Фёдором Васильевичем, в том числе за период войны.",
		"links": [{"icon":"Info","label":":","action":"https://saransk.kassir.ru/muzey/dom-muzey-f-v-syichkova"}]
	}

]
""".trimIndent()
