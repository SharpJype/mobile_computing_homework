package com.example.homework_kotlin


import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.homework_kotlin.ui.theme.Homework_kotlinTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

import androidx.navigation.NavController
import androidx.navigation.toRoute
import coil.compose.rememberAsyncImagePainter
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayList

import java.util.Random
import kotlin.math.absoluteValue

/*const val FULL_ALPHA: Int = 255 shl 24
fun randomFullColor(rng:Random):Int {
    return rng.nextInt().absoluteValue or FULL_ALPHA
}*/


const val INSTALL_STATE_FILENAME = "installState"
@Serializable
data class AppInstallState(
    // keep statistics since app was first launched
    var timesLaunched:Int = 0,
    val diceCollections:HashMap<String, DiceCollection> = HashMap()
)

data class AppSessionMutables(
    val bytes:SnapshotStateList<GeneratedByte> = mutableStateListOf(),
)
data class AppSessionState(
    var seed: Long,
    val rng: Random,
    val mutables: AppSessionMutables = AppSessionMutables(),
    var installState: AppInstallState? = null,
    var navigation: Navigation? = null,
)









class GeneratedByte (rng:Random) {
    val value = rng.nextInt().absoluteValue%256
    val color = rng.nextFloat()*360
    val size = 30F+rng.nextFloat()*30
}

@Serializable
data class Die(
    val sides:Short,
    var roll:Short,
)

@Serializable
data class DiceCollection(
    val name:String,
    var imagePath:String? = null,
    val dice:ArrayList<Die> = ArrayList(),
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




val appState = AppSessionState(
    0,
    Random(),
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
                NavigableViews()
            }
        }
    }
}

@Composable
fun CustomEventListener(onEvent:(event: Lifecycle.Event) -> Unit) {
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
}

@Composable
fun DieIcon(die:Die) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .padding(5.dp)
            .drawWithCache {
                val roundedPolygon = RoundedPolygon(
                    numVertices = 3 + (die.sides - 1) / 3,
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
                    drawPath(
                        roundedPolygonPath, color = (
                                Color.hsl((0F + die.sides * 10) % 360, .5F, .4F)
                                )
                    )
                }
            }
    ) {
        Row(modifier = Modifier.size(100.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                die.roll.toString(),
                fontSize = 30.sp,
                color = Color.White,
            )
        }
    }
}

@Composable
fun NewDiceCollectionView() {
    val context = LocalContext.current
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
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 30.dp).fillMaxHeight()
    ) {
        TextButton(onClick = {
            imageLauncher.launch(
                PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)
            )},
            modifier = Modifier
                .height(200.dp)//if (image.value == null) 50.dp else 150.dp)
                .padding(5.dp)
                .fillMaxWidth(),
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

        Column {
            TextField(name, {name=it},
                label = {Text("name (required)")},
                shape = RectangleShape,
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = {
                        appState.navigation!!.back()
                    }
                ) {
                    Text("cancel")
                }
                Button(onClick = {
                    if (name.isNotEmpty()) {
                        val newCollection = DiceCollection(name)
                        // copy image to app files
                        image.value?.let {
                            /*var copiedFile:File? = null
                            it.path?.let { it1 ->//use the picked gallery path -> less duplicate images
                                {copiedFile = File(context.filesDir, it1)}
                            }
                            if (copiedFile==null) {
                                copiedFile = File(context.filesDir, name)
                            }*/
                            val copiedFile = File(context.filesDir, name)// always make a new copy of image
                            fileFromContentUri(context, it, copiedFile)
                            newCollection.imagePath = copiedFile.path
                        }
                        appState.installState!!.diceCollections[name] = newCollection
                        saveInstallState(File(context.filesDir, INSTALL_STATE_FILENAME), appState.installState!!)
                        appState.navigation!!.back()
                    }
                }) {
                    Text("confirm")
                }
            }
        }

    }
}





@Composable
fun DiceCollectionCard(collection: DiceCollection, color:Color) {
    val defaultColor = ButtonDefaults.buttonColors()
    Button (
        onClick = {
            appState.navigation!!.diceCollection(collection.name)
        },
        modifier = Modifier.padding(5.dp),
        contentPadding = PaddingValues(0.dp),
        shape = RectangleShape,
        //enabled = collection.dice.size>0,
        colors = ButtonColors(
            containerColor = color,
            contentColor = defaultColor.contentColor,
            disabledContentColor = defaultColor.disabledContentColor,
            disabledContainerColor = defaultColor.disabledContainerColor
            )
    ) {
        Column(modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth()) {
            collection.imagePath?.let{
                Image(
                    BitmapFactory.decodeFile(collection.imagePath).asImageBitmap(),
                    null,
                    modifier = Modifier.padding(bottom = 5.dp).fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }
            Text(collection.name, fontSize = 20.sp)
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("DefaultLocale")
@Composable
fun DiceCollectionView(collection:DiceCollection) {
    var sides by remember { mutableIntStateOf(6) }
    val dice = remember {
        val list = mutableStateListOf<Die>()
        collection.dice.forEach {
            list.add(it)
        }
        list
    }
    fun rollDie(die:Die) {
        die.roll = (1+appState.rng.nextInt().absoluteValue%die.sides).toShort()
    }

    Column(verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 30.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState(0))
    ) {
        /*
        collection.dice.keys.sorted().forEach {
            val amount = collection.dice.getValue(it)
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState(0))
            ) {
                val sum = remember { mutableIntStateOf(0) }
                val mean = remember { mutableFloatStateOf(0F) }
                val median = remember { mutableIntStateOf(0) }
                TextButton(onClick = {
                    if (!rolls.contains(it)) rolls[it] = mutableIntListOf()
                    val list = rolls[it]!!
                    sum.intValue = 0
                    list.clear()
                    for (i in 1..amount) {
                        list.add(1+appState.rng.nextInt().absoluteValue%it)
                        sum.intValue += list.last()
                    }
                    list.sort()
                    mean.floatValue = sum.intValue.toFloat()/list.size
                    median.intValue = list[(if (list.size>1) list.size/2 else 0)]
                }
                ) {
                    DiceIcon(amount, it)
                }
                if (sum.intValue>0) {
                    Column() {
                        Text(sum.intValue.toString())
                        Text(String.format("%.3f", mean.floatValue))
                        Text(median.intValue.toString())
                    }
                }

            }
        }*/
        FlowRow(
            modifier = Modifier
                .weight(1F)
                .verticalScroll(rememberScrollState(0)),
            //horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            dice.forEach {
                TextButton(
                    onClick = {
                        if (dice.removeIf { die -> die === it }) {
                            collection.dice.remove(it)
                        }
                    }
                ) {
                    DieIcon(it)
                }
            }
        }

        Row(
            verticalAlignment=Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 50.dp)
        ) {
            TextButton(onClick={
                if (sides>2) {
                    sides--
                }
            }){
                Text("-", fontSize = 40.sp)
            }
            TextButton(onClick = {
                val newDie = Die(sides.toShort(), 0)
                rollDie(newDie)
                dice.add(newDie)
                collection.dice.add(newDie)
            }
            ){
                DieIcon(Die(sides.toShort(), sides.toShort()))
                //Text(sides.toString(), fontSize = 60.sp)
            }
            TextButton(onClick={
                sides++
            }){
                Text("+", fontSize = 40.sp)
            }
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(onClick = {
                dice.forEach {
                    rollDie(it)
                }
                dice.reverse()
                dice.reverse()
            }) {
                Text("reroll")
            }
            TextButton(onClick = {appState.navigation!!.back()}
            ) {
                Text("back")
            }
            TextButton(onClick = {
                dice.sortWith(comparator = {die1, die2 -> die1.roll-die2.roll})
                collection.dice.sortWith(comparator = {die1, die2 -> die1.roll-die2.roll})
            }) {
                Text("sort")
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrimaryView() {
    val count = remember { mutableIntStateOf(0) } // to detect updates to collections
    val collections = appState.installState!!.diceCollections
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
            if ((count.intValue>0) or (collections.size>0)) {
                collections.keys.sorted().forEach {
                    val collection = collections.getValue(it)
                    DiceCollectionCard(collection,
                        Color.hsl(240F, 0.3F, 0.4F, alpha = (if (collection.dice.size>0) 1F else .2F))
                    )
                }
            }

        }

        Row(
            modifier = Modifier
            .padding(vertical = 5.dp)
            .fillMaxWidth(),
        ) {
            Button(
                onClick = {
                    collections.keys.sorted().forEach { key ->
                        val collection = collections.getValue(key)
                        if (collection.dice.size==0) {
                            collection.imagePath?.let { // presumes unique image
                                File(it).delete()
                            }
                            collections.remove(key)
                        }
                    }
                    count.intValue = collections.size
                },
                modifier = Modifier
                    .weight(1F)
                    .padding(end = 5.dp)
            ) {
                Text("clean empty")
            }
            Button(
                onClick = {appState.navigation!!.newDiceSet()},
                modifier = Modifier
                    .weight(1F)
                    .padding(start = 5.dp)
            ) {
                Text("new collection")
            }
        }
        Row {
            Button(
                onClick = {appState.navigation!!.byteGen()},
                modifier = Modifier
                    .weight(1F)
                    .padding(end = 5.dp)
            ) {
                Text("byte generator")
            }
            Button(
                onClick = {appState.navigation!!.statistics()},
                modifier = Modifier
                    .weight(1F)
                    .padding(start = 5.dp)
            ) {
                Text("statistics")
            }
        }
    }
}



@Composable
fun ByteGenView() {
    Column (verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 30.dp)
                .fillMaxWidth()
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextButton(onClick = {appState.navigation!!.back()}
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
                appState.mutables.bytes.add(GeneratedByte(appState.rng)) },
            ) {
                Text("generate bytes")
            }
        }
        LazyColumn (
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1F)
        ) {
            items(appState.mutables.bytes) {item ->
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


@SuppressLint("DefaultLocale")
@Composable
fun StatisticsView() {
    Column (verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 30.dp)
            .fillMaxSize()
    ) {
        var string = "Launches: "+appState.installState?.timesLaunched.toString()
        string += "\nSeed: "+appState.seed.toString()

        val collections = appState.installState?.diceCollections!!
        string += String.format("\nCollections: %d", collections.size)
        var diceCount = 0
        collections.forEach {
            diceCount += it.value.dice.size
        }
        string += String.format("\nDice Total: %d", diceCount)
        Text(string)
        TextButton(onClick = {appState.navigation!!.back()}
        ) {
            Text("back")
        }
    }
}




class Navigation(navController:NavController) {
    private val controller = navController
    val statistics = {controller.navigate(route="statistics")}
    val back = {controller.navigateUp()}
    val byteGen = {controller.navigate(route="byteGen")}
    val newDiceSet = {controller.navigate(route="newDiceCollection")}
    fun diceCollection(name: String) {
        controller.navigate(DiceCollectionRoute(name))
    }
}

@Serializable
data class DiceCollectionRoute(val name: String)

@Composable
fun NavigableViews() {
    val context = LocalContext.current
    val navController = rememberNavController()
    appState.navigation = Navigation(navController)

    CustomEventListener {
        if (it==Lifecycle.Event.ON_PAUSE) {
            saveInstallState(File(context.filesDir, INSTALL_STATE_FILENAME), appState.installState!!)
        }
        /*if (it == Lifecycle.Event.ON_START) {
            //appState.installState.timesLaunched++
            saveInstallState(installFile, appState.installState)
        }*/
        /*if (appState.saveInstallState) { // check every event
            saveInstallState(installFile, appState.installState)
            appState.saveInstallState = false
        }*/
    }
    NavHost(navController, startDestination = "primary") {
        composable<DiceCollectionRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<DiceCollectionRoute>()
            val collection = appState.installState!!.diceCollections.getValue(route.name)
            DiceCollectionView(collection)
        }
        composable("primary") {PrimaryView()}
        composable("newDiceCollection") {NewDiceCollectionView()}
        composable("byteGen") {ByteGenView()}
        composable("statistics") {StatisticsView()}
    }
}

