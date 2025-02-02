package com.example.homework_kotlin


import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.ProvidableCompositionLocal
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.homework_kotlin.ui.theme.Homework_kotlinTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

import java.math.BigInteger
import java.util.Random

const val FULL_ALPHA: Int = 255 shl 24
fun randomFullColor(rng:Random):Int {
    return BigInteger(24, rng).toInt() or FULL_ALPHA
}

class GeneratedByte (rng:Random) {
    val value = BigInteger(8, rng).toInt()//.toString()
    val color = randomFullColor(rng)
    val size = 30F+BigInteger(4, rng).toInt()
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
    var filesDir: File? = null,
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
            appState.seed = BigInteger(16, appState.rng).toLong()
            appState.rng.setSeed(appState.seed)
            appState.filesDir = this.filesDir//

            val installFile = File(this.filesDir, INSTALL_STATE_FILENAME)
            //installFile.delete()// RESET
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
        modifier = Modifier.padding(10.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.weight(1F)) {

        }

        Row(verticalAlignment=Alignment.CenterVertically) {

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
            Spacer(modifier = Modifier.weight(1F))
            Text(dieString, fontSize = 60.sp)
            Spacer(modifier = Modifier.weight(1F))
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
        Button(onClick={
            var newAmount = amount
            if (dice.containsKey(sides)) {
                newAmount += dice.getValue(sides)
            }
            dice[sides] = newAmount
        }){
            Text("add to collection")
        }
        Button(onClick={
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
        }){
            Text("subtract from collection")
        }

        Button(onClick = {
            imageLauncher.launch(
                PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)
            )}
        ) {
            Text("select image")
        }
        image.value?.let {
            val painter = rememberAsyncImagePainter(it)
            Image(painter = painter,
                null,
                modifier = Modifier.size(150.dp, 150.dp)
                    .padding(16.dp)
            )
        }

        var name by remember { mutableStateOf("") }
        TextField(name, {name=it}, label = {Text("name (required)")})

        Row {
            TextButton(onClick = {navigationLambdas.back()}) {
                Text("cancel")
            }
            Button(onClick = {
                if (name.isNotEmpty()) {
                    val newCollection = DiceCollection()
                    // copy image to app files
                    image.value?.let {
                        val copiedFile = File(appState.filesDir, name)
                        fileFromContentUri(context, it, copiedFile)
                        newCollection.imagePath = copiedFile.path
                    }
                    dice.forEach { (k, v) -> newCollection.dice[k] = v }
                    appState.installState!!.diceCollections[name] = newCollection
                    saveInstallState(File(appState.filesDir, INSTALL_STATE_FILENAME), appState.installState!!)
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
    Column(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
        //if (!collection.imagePath.isNullOrBlank()) {
        collection.imagePath?.let{
            Image(
                BitmapFactory.decodeFile(collection.imagePath).asImageBitmap(),
                null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        }
        Text(name)
            //Text(collection.imagePath!!)
        //}
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrimaryView(navigationLambdas:NavigationLambdas) {
    Column(verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        FlowRow(
            modifier = Modifier
                .weight(1F)
                .verticalScroll(rememberScrollState(0)),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            appState.installState?.diceCollections?.forEach {
                DiceCollectionCard(it.key,it.value)
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
        Spacer(modifier = Modifier.size(20.dp))
    }
}



@Composable
fun ByteGenView(navigationLambdas:NavigationLambdas) {
    Column (verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.size(20.dp))
            Button(onClick = {navigationLambdas.back()}
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
                Row {
                    val color = Color(item.color)
                    var binary = ""
                    for (i in 0..7) {
                        binary += ((item.value shr 7-i)%2).toString()
                    }
                    Text(
                        item.value.toString(),
                        color = color,
                        fontSize = item.size.sp,
                        modifier = Modifier.padding(horizontal = 5.dp)
                    )
                    Spacer(modifier = Modifier.weight(1F))
                    Text(
                        binary,
                        color = color,
                        fontSize = item.size.sp,
                        modifier = Modifier.padding(horizontal = 5.dp)
                    )
                }

            }
        }
        Spacer(modifier = Modifier.size(20.dp))
    }
}


@OptIn(ExperimentalStdlibApi::class)
@Composable
fun StatisticsView(navigationLambdas:NavigationLambdas) {
    Column (verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()) {

        Spacer(modifier = Modifier.size(20.dp))
        Button(onClick = {navigationLambdas.back()}
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

