package com.example.homework_kotlin


import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.homework_kotlin.ui.theme.Homework_kotlinTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

import java.util.Random
import kotlin.math.absoluteValue

const val FULL_ALPHA: Int = 255 shl 24
fun randomFullColor(rng:Random):Int {
    return rng.nextInt().absoluteValue or FULL_ALPHA
}

class GeneratedByte (rng:Random) {
    val value = rng.nextInt().absoluteValue%256
    val color = rng.nextFloat()*360
    val size = 30F+rng.nextFloat()*30
}

@Serializable
data class DiceCollection(
    var imagePath:String? = null,
    val dice:HashMap<Int,Int> = java.util.HashMap(),
)

@Serializable
data class AppInstallState(
    // keep statistics since app was first launched
    var timesLaunched:Int = 0,
    val diceCollections:HashMap<String, DiceCollection> = HashMap()
)
fun saveInstallState(file:File, installState:AppInstallState) {
    val writeStream = file.outputStream()
    val bytes = Json.encodeToString(installState).encodeToByteArray()
    writeStream.write(bytes)
    writeStream.close()
}
fun loadInstallState(file:File):AppInstallState {
    var installState = AppInstallState()
    if (file.exists()) {
        val readStream = file.inputStream()
        val buffer = ByteArray(file.length().toInt())
        readStream.read(buffer)
        readStream.close()
        val withUnknownKeys = Json {ignoreUnknownKeys=true}
        installState = withUnknownKeys.decodeFromString<AppInstallState>(buffer.decodeToString())
    }
    installState.timesLaunched++
    saveInstallState(file, installState)
    return installState
}


const val INSTALL_STATE_FILENAME = "installState"
data class AppSessionState (
    var seed: Long,
    val rng : Random,
    val list : SnapshotStateList<GeneratedByte>,
    var installState: AppInstallState? = null,
)
val appState = AppSessionState(
    0,
    Random(),
    mutableStateListOf(),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (appState.installState==null) {
            appState.seed = appState.rng.nextLong().absoluteValue%(1 shl 16)
            appState.rng.setSeed(appState.seed)
            val installFile = File(this.filesDir, INSTALL_STATE_FILENAME)
            appState.installState = loadInstallState(installFile)
        }
        setContent {
            Homework_kotlinTheme {
                NavigableViews(appState)
            }
        }
    }
}
/*
@Composable
fun CustomEventListener(onEvent:(event:Lifecycle.Event) -> Unit) {
    val eventHandler = rememberUpdatedState(newValue = onEvent)
    val lifecycleOwner = rememberUpdatedState(newValue = LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value ){
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer = LifecycleEventObserver{_, event ->
            eventHandler.value(event)
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}*/


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewDiceSetView(navigationLambdas:NavigationLambdas) {
    val context = LocalContext.current

    var amount by remember { mutableIntStateOf(1) }
    var sides by remember { mutableIntStateOf(6) }
    var dieString by remember { mutableStateOf("") }
    val dice = remember {mutableStateMapOf<Int,Int>()}

    fun updateDieString() {
        dieString = amount.toString()+"d"+sides.toString()
    }
    updateDieString()

    val image = remember {mutableStateOf<Uri?>(null) }
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                image.value = it
            }
        }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 30.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .weight(1F)
                //.padding(vertical = 20.dp)
                .verticalScroll(rememberScrollState(0))
        ) {
            dice.keys.sorted().forEach {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .padding(5.dp)
                        .drawWithCache {
                            val roundedPolygon = RoundedPolygon(
                            numVertices = 3 + (it-1)/3,
                            radius = size.minDimension / 2,
                            centerX = size.width / 2,
                            centerY = size.height / 2,
                            rounding = CornerRounding(
                                size.minDimension / 10f,
                                smoothing = 0.1f
                            )
                        )
                            val roundedPolygonPath = roundedPolygon.toPath().asComposePath()
                            onDrawBehind {
                                drawPath(roundedPolygonPath, color = (
                                        Color.hsl((0F+it*10)%360, .5F, .4F)
                                        /*
                                        if (it.key>9) Color.hsl(0F, .5F, .5F)
                                        else if (it.key>6) Color.hsl(20F, .5F, .5F)
                                        else if (it.key>4) Color.hsl(40F, .5F, .5F)
                                        else Color.hsl(60F, .5F, .5F)*/
                                        ))
                            }
                        }
                ) {
                    Row(modifier = Modifier.size(100.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            dice.getValue(it).toString()+"d"+it.toString(),
                            fontSize = 25.sp,
                            color = Color.White,
                        )
                    }
                }

            }

        }

        Row(
            verticalAlignment=Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 50.dp)
        ) {
            Column {
                TextButton(onClick={
                    amount++
                    updateDieString()
                }){
                    Text("+", fontSize = 40.sp)
                }
                TextButton(onClick={
                    if (amount>1) {
                        amount--
                        updateDieString()
                    }
                }
                ){
                    Text("-", fontSize = 40.sp)
                }
            }
            //Spacer(modifier = Modifier.weight(1F))
            Column(
                modifier = Modifier.weight(1F),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(dieString, fontSize = 60.sp)
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick={
                            if (dice.contains(sides)) {
                                var newAmount = dice.getValue(sides)
                                newAmount -= amount
                                if (newAmount<=0) {
                                    dice.remove(sides)
                                }
                                else {
                                    dice[sides] = newAmount
                                }
                            }
                        },
                        contentPadding = PaddingValues(5.dp),
                        shape = RectangleShape
                    ){
                        Text("subtract")
                    }
                    TextButton(onClick={
                        var newAmount = amount
                        if (dice.containsKey(sides)) {
                            newAmount += dice.getValue(sides)
                        }
                        dice[sides] = newAmount
                        },
                        contentPadding = PaddingValues(10.dp),
                        shape = RectangleShape
                    ){
                        Text("add")
                    }
                }
            }
            //Spacer(modifier = Modifier.weight(1F))
            Column {
                TextButton(onClick={
                    sides++
                    updateDieString()
                }){
                    Text("+", fontSize = 40.sp)
                }
                TextButton(onClick={
                    if (sides>2) {
                        sides--
                        updateDieString()
                    }
                }){
                    Text("-", fontSize = 40.sp)
                }
            }
        }


        TextButton(onClick = {
            imageLauncher.launch(
                PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)
            )},
            modifier = Modifier.height(if (image.value==null)50.dp else 150.dp).padding(5.dp).fillMaxWidth(),
            contentPadding = PaddingValues(0.dp),
            shape = RectangleShape
        ) {
            if (image.value==null) {
                Text("select image (optional)")
            }
            else {
                image.value?.let {
                    val painter = rememberAsyncImagePainter(it)
                    Image(painter = painter,
                        null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }


        var name by remember { mutableStateOf("") }
        TextField(name, {name=it}, label = {Text("name (required)")})

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = {navigationLambdas.back()}) {
                Text("cancel")
            }
            Button(onClick = {
                if (name.isNotEmpty()) {
                    val newCollection = DiceCollection()
                    // copy image to app files
                    image.value?.let {
                        var copiedFile:File? = null
                        it.path?.let { it1 ->//use the picked gallery path -> less duplicate images
                            {copiedFile = File(context.filesDir, it1)}
                        }
                        if (copiedFile==null) {
                            copiedFile = File(context.filesDir, name)
                        }
                        fileFromContentUri(context, it, copiedFile!!)
                        newCollection.imagePath = copiedFile!!.path
                    }
                    dice.forEach { (k, v) -> newCollection.dice[k] = v }
                    appState.installState!!.diceCollections[name] = newCollection
                    saveInstallState(File(context.filesDir, INSTALL_STATE_FILENAME), appState.installState!!)
                    navigationLambdas.back()
                }
            }) {
                Text("confirm")
            }
        }
    }
}





////// original from -> https://medium.com/@bdulahad/file-from-uri-content-scheme-ac51c10c8331
private fun fileFromContentUri(context: Context, contentUri: Uri, file:File) {
    file.createNewFile()
    try {
        val outputStream = FileOutputStream(file)
        val inputStream = context.contentResolver.openInputStream(contentUri)
        inputStream?.let {
            copy(inputStream, outputStream)
        }
        outputStream.flush()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
@Throws(IOException::class)
private fun copy(source: InputStream, target: OutputStream) {
    val buf = ByteArray(8192)
    var length: Int
    while (source.read(buf).also { length = it } > 0) {
        target.write(buf, 0, length)
    }
}
///////


@Composable
fun DiceCollectionCard(name:String, collection: DiceCollection) {
    Button (
        onClick = {},// TODO
        modifier = Modifier.padding(5.dp),
        contentPadding = PaddingValues(0.dp),
        shape = RectangleShape
    ) {
        Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
            collection.imagePath?.let{
                Image(
                    BitmapFactory.decodeFile(collection.imagePath).asImageBitmap(),
                    null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }
            Text(name)
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrimaryView(navigationLambdas:NavigationLambdas) {
    Column(verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 30.dp)
    ) {
        FlowRow(
            modifier = Modifier
                .weight(1F)
                .verticalScroll(rememberScrollState(0)),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val collections = appState.installState?.diceCollections
            collections?.keys?.sorted()?.forEach {
                DiceCollectionCard(it,collections.getValue(it))
            }
        }

        Button(
            onClick = {navigationLambdas.newDiceSet()},
            modifier = Modifier
                .padding(vertical = 5.dp)
                .fillMaxWidth()
        ) {
            Text("new collection")
        }
        Row {
            Button(
                onClick = {navigationLambdas.byteGen()},
                modifier = Modifier
                    .weight(1F)
                    .padding(end=5.dp)
            ) {
                Text("byte generator")
            }
            Button(
                onClick = {navigationLambdas.statistics()},
                modifier = Modifier
                    .weight(1F)
                    .padding(start=5.dp)
            ) {
                Text("statistics")
            }
        }
    }
}



@Composable
fun ByteGenView(navigationLambdas:NavigationLambdas) {
    Column (verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 30.dp)
                .fillMaxWidth()
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextButton(onClick = {navigationLambdas.back()}
            ) {
                Text("back")
            }
            Image(painter = painterResource(
                id = R.drawable.campfire_w_sword),
                "image",
                modifier = Modifier
                    .size(200.dp)
            )
            Button(onClick = {
                appState.list.add(GeneratedByte(appState.rng)) },
            ) {
                Text("generate bytes")
            }
        }
        LazyColumn (
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1F)
        ) {
            items(appState.list) {item ->
                //val color = Color(item.color)
                var binary = ""
                for (i in 0..7) {
                    binary += ((item.value shr 7-i)%2).toString()
                }
                Text(
                    String.format("%2H", item.value)+" : "+binary,
                    color = Color.hsl(item.color, 0.5F, 0.5F),
                    fontSize = item.size.sp/2,
                    //modifier = Modifier.padding(horizontal = 5.dp)
                )

            }
        }
    }
}


@OptIn(ExperimentalStdlibApi::class)
@Composable
fun StatisticsView(navigationLambdas:NavigationLambdas) {
    Column (verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 30.dp)
            .fillMaxWidth()
    ) {
        TextButton(onClick = {navigationLambdas.back()}
        ) {
            Text("back")
        }
        Text("Times launched: "+appState.installState?.timesLaunched.toString())
        Text("ByteGen seed: "+appState.seed.toString())
    }
}




class NavigationLambdas(navController:NavController) {
    val statistics = {navController.navigate(route="statistics")}
    val back = {navController.navigateUp()}
    val byteGen = {navController.navigate(route="byteGen")}
    val newDiceSet = {navController.navigate(route="newDiceSet")}
}

@Composable
fun NavigableViews(appState:AppSessionState) {
    val navController = rememberNavController()
    val navigationLambdas = NavigationLambdas(navController)

    /*CustomEventListener {
        /*if (it == Lifecycle.Event.ON_START) {
            //appState.installState.timesLaunched++
            saveInstallState(installFile, appState.installState)
        }*/
        /*if (appState.saveInstallState) { // check every event
            saveInstallState(installFile, appState.installState)
            appState.saveInstallState = false
        }*/
    }*/
    NavHost(navController, startDestination = "primary") {

        composable("primary") {
            PrimaryView(navigationLambdas = navigationLambdas)
        }
        composable("newDiceSet") {
            NewDiceSetView(navigationLambdas = navigationLambdas)
        }
        composable("byteGen") {
            ByteGenView(navigationLambdas = navigationLambdas)
        }
        composable("statistics") {
            StatisticsView(navigationLambdas = navigationLambdas)
        }
    }
}

