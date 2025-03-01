package com.example.homework_kotlin


import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

import androidx.navigation.NavController
import androidx.navigation.toRoute
import coil.compose.rememberAsyncImagePainter
import com.example.homework_kotlin.ui.theme.DefaultTheme

import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

import java.util.Random
import kotlin.collections.ArrayList
import kotlin.math.absoluteValue

// CONSTANTS
const val INSTALL_STATE_FILENAME = "installState"
const val NOTIFICATION_CHANNEL_ID = "main"


// BASE CLASSES
@Serializable
data class AppInstallState(
    // keep statistics since app was first launched
    var timesLaunched:Int = 0,
    val diceCollections:HashMap<String, DiceCollection> = HashMap()
)


class AppNavigation {
    var controller:NavController? = null
    val statistics = {controller?.navigate(route="statistics")}
    val back = {controller?.navigateUp()}
    val byteGen = {controller?.navigate(route="byteGen")}
    val newDiceSet = {controller?.navigate(route="newDiceCollection")}
    fun diceCollection(name: String) {
        controller?.navigate(DiceCollectionRoute(name))
    }
}

class AppNotifications {
    var save:()->Unit = {}
}

data class AppSessionState(
    var seed: Long,
    val rng: Random,
    val bytes:SnapshotStateList<Int> = mutableStateListOf(),
    var initialized:Boolean = false,
    var installState: AppInstallState = AppInstallState(),
    var navigation:AppNavigation = AppNavigation(),
    var sensors:AppSensors? = null,
    val notifications:AppNotifications = AppNotifications()
)



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




/// PERMISSIONS
@Composable
fun askPermission(permission: String, onRefused: ()->Unit={}, onGranted: ()->Unit={}):()->Unit {
    val result = remember { mutableStateOf<Boolean?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        result.value = it
        if (it) onGranted()
        else onRefused()
    }
    return {
        when (result.value) {
            null -> launcher.launch(permission)
            true -> onGranted()
            else -> onRefused()
        }
    }
}

fun checkPermission(context: Context, permission: String, onRefused:()->Unit = {}, onGranted:()->Unit = {}) {
    when (PackageManager.PERMISSION_GRANTED) {
        ContextCompat.checkSelfPermission(context, permission) -> onGranted()
        else -> onRefused()
    }
}

// NOTIFICATIONS
private fun createNotificationChannel(context: Context) {
    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is not in the Support Library.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "diceBoxNotifications", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "description"
        }
        // Register the channel with the system.
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

@SuppressLint("DefaultLocale", "MissingPermission")
@Composable
fun prepareNotification(title:String, body:String, timeout:Long? = null, id:Int = 0):()->Unit {
    val context = LocalContext.current
    val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    val pendingActionIntent: PendingIntent =
        PendingIntent.getActivity(context, 1, openIntent, PendingIntent.FLAG_IMMUTABLE)

    val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.chat)
        .setContentTitle(title)
        .setContentText(body+(if (timeout!=null) String.format("\nTimeout in %d seconds", timeout/1000) else ""))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setColor(MaterialTheme.colorScheme.primary.toArgb()) // use primary color
        .setCategory(Notification.CATEGORY_EVENT)
        .setSilent(true)
        .addAction(R.drawable.gamepad, "OPEN", pendingActionIntent)

    if (timeout!=null) builder.setTimeoutAfter(timeout)
    return {
        checkPermission(context, "android.permission.POST_NOTIFICATIONS") {
            with(NotificationManagerCompat.from(context)) {
                notify(id, builder.build())
            }
        }
    }
}


// SENSORS
data class SensorDataPoint(val x:Float, val y:Float, val z:Float)
class AppSensors(context: Context) : SensorEventListener {
    //val output = ArrayDeque<SensorDataPoint>()
    private val manager:SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val grav:Sensor? = manager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    var gravData:MutableState<SensorDataPoint?> = mutableStateOf(null)

    fun stop() {
        manager.unregisterListener(this)
    }
    fun start() {
        grav?.let {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //println(accuracy.toString())
    }
    @SuppressLint("DefaultLocale")
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            //println(it.sensor.name+" "+it.sensor.type.toString())
            when (it.sensor.type) {
                Sensor.TYPE_GRAVITY ->
                {
                    gravData.value = SensorDataPoint(it.values[0], it.values[1], it.values[2])
                }
                else -> {}
            }

        }
    }
}





// FILE OPERATION FUNCTIONS
fun saveInstallState(file:File) {
    val writeStream = file.outputStream()
    val bytes = Json.encodeToString(appState.installState).encodeToByteArray()
    writeStream.write(bytes)
    writeStream.close()
}
fun loadInstallState(file:File) {
    if (file.exists()) {
        val readStream = file.inputStream()
        val buffer = ByteArray(file.length().toInt())
        readStream.read(buffer)
        readStream.close()
        val withUnknownKeys = Json {ignoreUnknownKeys=true}
        appState.installState = withUnknownKeys.decodeFromString<AppInstallState>(buffer.decodeToString())
    }
    appState.installState.timesLaunched++
    saveInstallState(file)
}

////// original from -> https://medium.com/@bdulahad/file-from-uri-content-scheme-ac51c10c8331
private fun fileFromContentUri(context: Context, contentUri: Uri, file:File) {
    file.createNewFile()
    try {
        val outputStream = FileOutputStream(file)
        val inputStream = context.contentResolver.openInputStream(contentUri)
        inputStream?.let {
            copy(it, outputStream)
        }
        outputStream.flush()
    } catch (e: Exception) {
        //e.printStackTrace()
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









val fontSize0 = 15.sp
val fontSize1 = 20.sp
val fontSize2 = 30.sp
val longSidePadding = 50.dp
val shortSidePadding = 25.dp
val appState = AppSessionState(
    0,
    Random(),
)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DefaultTheme {
                Surface {
                    NavigableViews()
                }
            }
        }
    }
}











@Composable
fun LifecycleEventListener(onEvent:(event: Lifecycle.Event) -> Unit) {
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
    val dieSize = 95.dp
    val diePadding = 5.dp
    Box(
        modifier = Modifier
            .size(dieSize)
            .padding(diePadding)
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
                                Color.hsl((50F + die.sides * 10) % 360, .5F, .5F)
                                )
                    )
                }
            }
    ) {
        Text(
            die.roll.toString(),
            fontSize = fontSize2,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .fillMaxSize()
                .padding(top=(dieSize+diePadding)/4),
            textAlign = TextAlign.Center,
        )
    }
}


val NewDiceCollectionViewImageUri = mutableStateOf<Uri?>(null)
@Composable
fun NewDiceCollectionView() {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val imageUri = remember {NewDiceCollectionViewImageUri}
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                imageUri.value = it
            }
        }
    )
    val buttonImage = @Composable {
        Button(
            onClick = {
                imageLauncher.launch(
                    PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier
                .aspectRatio(1F)
                .fillMaxSize(),
            contentPadding = PaddingValues(0.dp),
            shape = RectangleShape,
            colors = (
                    if (imageUri.value != null) ButtonColors(Color.Transparent, Color.Transparent, Color.Transparent, Color.Transparent)
                    else ButtonDefaults.buttonColors()
                    ),
        ) {
            if (imageUri.value==null) {
                Text(
                    "select image (optional)",
                    fontSize = fontSize1,
                )
            }
            else {
                imageUri.value?.let {
                    val painter = rememberAsyncImagePainter(it)
                    Image(painter = painter,
                        null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    val textField = @Composable {
        var name by remember { mutableStateOf("") }
        TextField(
            name, { name = it },
            label = {
                Text(
                    "enter name (required)",
                    fontSize = fontSize1,
                )
            },
            shape = RectangleShape,
            modifier = Modifier
                .padding(vertical = 10.dp)
                .fillMaxWidth(),
        )

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(
                onClick = {
                    appState.navigation.back()
                },
            ) {
                Text(
                    "cancel",
                    fontSize = fontSize1,
                )
            }
            Button(
                onClick = {
                    if (name.isNotEmpty()) {
                        val newCollection = DiceCollection(name)
                        // copy image to app files
                        val imageFile = File(context.filesDir, name)
                        if (imageFile.exists()) imageFile.delete() // if overwriting, delete the old image if it exists
                        imageUri.value?.let {
                            fileFromContentUri(context, it, imageFile) // make a new copy of image to save with the app files
                            newCollection.imagePath = imageFile.path
                        }
                        appState.installState.diceCollections[name] = newCollection
                        saveInstallState(File(context.filesDir, INSTALL_STATE_FILENAME))
                        appState.notifications.save()
                        appState.navigation.back()
                    }
                },
            ) {
                Text(
                    "confirm",
                    fontSize = fontSize1,
                )
            }
        }
    }

    if (landscape) {
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = longSidePadding, vertical = shortSidePadding)
                .fillMaxSize()
        ) {
            Box (
                modifier = Modifier
                    .weight(1F)
            ) {
                buttonImage()
            }
            Column (
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1F)
            ) {
                textField()
            }
        }
    }
    else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .padding(horizontal = shortSidePadding, vertical = longSidePadding)
                .fillMaxSize()
        ) {
            Box (
                modifier = Modifier
                    .weight(1F)
            ) {
                buttonImage()
            }
            Column (
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1F)
            ) {
                textField()
            }
        }
    }

}





@Composable
fun DiceCollectionCard(collection: DiceCollection) {
    Button (
        onClick = {
            appState.navigation.diceCollection(collection.name)
        },
        modifier = Modifier.padding(5.dp),
        contentPadding = PaddingValues(0.dp),
        shape = RectangleShape,
        colors = ButtonColors(
            (if (collection.dice.size==0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary),
            MaterialTheme.colorScheme.onPrimary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Column(modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth()) {
            collection.imagePath?.let{
                if (File(it).exists()) { // not null and exists
                    Image(
                        BitmapFactory.decodeFile(it).asImageBitmap(),
                        null,
                        modifier = Modifier
                            .padding(bottom = 5.dp)
                            .fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                }

            }
            Text(
                collection.name,
                fontSize = fontSize1,
            )
        }
    }
}

// remember button states through orientation changes
var diceCollectionViewSides = 6
val diceCollectionViewChanges = mutableStateOf(false)
fun resetDiceCollectionView() { // when exiting the collection view
    diceCollectionViewSides = 6
    diceCollectionViewChanges.value = false
}

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("DefaultLocale")
@Composable
fun DiceCollectionView(collection:DiceCollection) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val dice = remember {
        val list = mutableStateListOf<Die>()
        collection.dice.forEach {
            list.add(it)
        }
        list
    }
    var sides by remember { mutableIntStateOf(diceCollectionViewSides) }
    fun rollDie(die:Die) {
        die.roll = (1+appState.rng.nextInt().absoluteValue%die.sides).toShort()
    }

    val diceTable = @Composable {
        FlowRow(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState(0)),
        ) {
            dice.forEach {
                TextButton(
                    onClick = {
                        if (dice.removeIf { die -> die === it }) {
                            collection.dice.remove(it)
                            diceCollectionViewChanges.value = true
                        }
                    },
                ) {
                    DieIcon(it)
                }
            }
        }
    }

    val buttonRoll = @Composable {
        TextButton(
            onClick = {
                dice.clear()
                collection.dice.forEach {rollDie(it);dice.add(it)}
                diceCollectionViewChanges.value = true
            },
        ) {
            Text("re-roll",
                fontSize = fontSize1,
                textAlign = TextAlign.Left,
                modifier = Modifier.defaultMinSize(minWidth = 80.dp)
            )
        }
    }
    val buttonClose = @Composable {
        TextButton(
            onClick = {
                if (diceCollectionViewChanges.value) {
                    saveInstallState(File(context.filesDir, INSTALL_STATE_FILENAME))
                    appState.notifications.save()
                }
                appState.navigation.back()
            },
        ) {
            Text((if (diceCollectionViewChanges.value) "save & close" else "close"),
                fontSize = fontSize1,
                textAlign = TextAlign.Center,
                modifier = Modifier.defaultMinSize(minWidth = 80.dp)
            )
        }
    }
    val buttonSort = @Composable {
        TextButton(
            onClick = {
                collection.dice.sortWith(comparator = {die1, die2 -> die1.roll-die2.roll})
                dice.clear()
                collection.dice.forEach {dice.add(it)}
                diceCollectionViewChanges.value = true
            },
        ) {
            Text("sort",
                fontSize = fontSize1,
                textAlign = TextAlign.Right,
                modifier = Modifier.defaultMinSize(minWidth = 80.dp)
            )
        }
    }
    val diceAdder = @Composable {
        Row(
            verticalAlignment=Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = {if (sides>2) {sides--;diceCollectionViewSides--}},
            ) {
                Text(
                    "-",
                    fontSize = fontSize2,
                )
            }
            TextButton(
                onClick = {
                    val newDie = Die(sides.toShort(), 0)
                    rollDie(newDie)
                    dice.add(newDie)
                    collection.dice.add(newDie)
                    diceCollectionViewChanges.value = true
                },
            ){
                DieIcon(Die(sides.toShort(), sides.toShort()))
                //Text(sides.toString(), fontSize = 60.sp)
            }
            TextButton(onClick={sides++;diceCollectionViewSides++},
            ){
                Text(
                    "+",
                    fontSize = fontSize2,
                )
            }
        }
    }

    if (landscape) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = longSidePadding, vertical = shortSidePadding)
                .fillMaxSize()
        ) {
            Column (
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1F)
            ) {
                diceAdder()
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    buttonRoll()
                    buttonClose()
                    buttonSort()
                }
            }
            Box (
                modifier = Modifier.weight(1F)
            ) {
                diceTable()
            }
        }
    }
    else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = shortSidePadding, vertical = longSidePadding)
                .fillMaxSize()
        ) {
            Box (
                modifier = Modifier.weight(1F)
            ) {
                diceTable()
            }
            diceAdder()
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                buttonRoll()
                buttonClose()
                buttonSort()
            }
        }
    }
}



@SuppressLint("DefaultLocale")
@Composable
fun ByteGenView() {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val content = @Composable {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth((if (landscape) .5F else 1F))
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = {appState.navigation.back()}
                ) {
                    Text(
                        "back",
                        fontSize = fontSize1,
                    )
                }
                TextButton(onClick = {appState.bytes.clear()}
                        ) {
                    Text(
                        "clear",
                        fontSize = fontSize1,
                    )
                }
            }
            Image(painter = painterResource(
                id = R.drawable.campfire_w_sword),
                "image",
                modifier = Modifier
                    .size(200.dp)
                    .padding(vertical = 10.dp)
            )
            Button(
                onClick = {
                    appState.bytes.add(appState.rng.nextInt().absoluteValue%256)
                },
                modifier = Modifier.padding(vertical = 5.dp)
            ) {
                Text(
                    "generate bytes",
                    fontSize = fontSize1,
                )
            }
        }
        LazyColumn (
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
            ) {
            items(appState.bytes) { value ->
                var binary = ""
                for (i in 0..7) {
                    binary += ((value shr 7-i)%2).toString()
                }
                Text(
                    String.format("%3d : %2H : %s", value, value, binary),
                    fontSize = fontSize2,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }

    if (!landscape) {
        Column (verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = shortSidePadding, vertical = longSidePadding)
                .fillMaxSize()
        ) {content()}
    }
    else {
        Row (horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .padding(horizontal = longSidePadding, vertical = shortSidePadding)
                .fillMaxSize()
        ) {content()}
    }
}


@SuppressLint("DefaultLocale")
@Composable
fun StatisticsView() {
    val gravData = remember { appState.sensors!!.gravData }
    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = shortSidePadding, vertical = longSidePadding)
            .fillMaxSize()
    ) {
        TextButton(
            onClick = {
                appState.navigation.back()
            },
        ) {
            Text(
                "back",
                fontSize = fontSize1,
            )
        }
        val stringBuilder = StringBuilder()
        stringBuilder.append("Launches: ")
        stringBuilder.append(appState.installState.timesLaunched.toString())
        stringBuilder.append("\nSeed: ")
        stringBuilder.append(appState.seed.toString())
        val collections = appState.installState.diceCollections
        stringBuilder.append(String.format("\nCollections: %d", collections.size))
        var diceCount = 0
        collections.forEach {
            diceCount += it.value.dice.size
        }
        stringBuilder.append(String.format("\nCollection Dice: %d", diceCount))
        gravData.value?.let {
            stringBuilder.append(String.format("\nGravity XYZ: %.3f, %.3f, %.3f", it.x, it.y, it.z))
        }
        Text(
            stringBuilder.toString(),
            fontSize = fontSize1,
            lineHeight = fontSize1 * 1.5,
        )
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrimaryView() {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val collections = appState.installState.diceCollections
    var count by remember { mutableIntStateOf(collections.size) } // to detect updates to collections

    resetDiceCollectionView()
    NewDiceCollectionViewImageUri.value = null // reset

    val buttonModifier0 = Modifier
        .fillMaxWidth((if (landscape) 1F else .5F))
    val buttonModifier1 = Modifier
        .padding(start = 10.dp)
        .fillMaxWidth(1F)
    val buttons0 = @Composable {
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
                // save instantly if something was cleared
                if (count!=collections.size) {
                    saveInstallState(File(context.filesDir, INSTALL_STATE_FILENAME))
                    appState.notifications.save()
                }
                count = collections.size
            },
            modifier = buttonModifier0
        ) {
            Text(
                "clear empty",
                fontSize = fontSize0,
            )
        }
        Button(
            onClick = {appState.navigation.newDiceSet()},
            modifier = (if (landscape) buttonModifier0 else buttonModifier1)
        ) {
            Text(
                "new collection",
                fontSize = fontSize0,
            )
        }
    }
    val buttons1 = @Composable {
        Button(
            onClick = {appState.navigation.byteGen()},
            modifier = buttonModifier0
        ) {
            Text(
                "byte generator",
                fontSize = fontSize0,
            )
        }
        Button(
            onClick = {appState.navigation.statistics()},
            modifier = (if (landscape) buttonModifier0 else buttonModifier1)
        ) {
            Text(
                "statistics",
                fontSize = fontSize0,
            )
        }
    }
    val cards = @Composable {
        FlowRow(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState(0)),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if ((count>0) or (collections.size>0)) {
                collections.keys.sorted().forEach {
                    val collection = collections.getValue(it)
                    DiceCollectionCard(collection)
                }
            }

        }
    }

    if (landscape) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = longSidePadding, vertical = shortSidePadding)
        ) {
            Column (
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1F)
            ) {
                buttons0()
                buttons1()
            }
            Box(modifier = Modifier
                .weight(2F)
            ) {
                cards()
            }
        }
    }
    else {
        Column(verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = shortSidePadding, vertical = longSidePadding)
        ) {
            Box(modifier = Modifier.weight(1F)) {
                cards()
            }
            Row(
                modifier = Modifier
                    .padding(vertical = 5.dp)
                    .fillMaxWidth(),
            ) {
                buttons0()
            }
            Row {
                buttons1()
            }
        }
    }


}




@Serializable
data class DiceCollectionRoute(val name: String)

@Composable
fun NavigableViews() {
    val context = LocalContext.current
    val navController = rememberNavController()
    appState.navigation.controller = navController
    appState.sensors = AppSensors(context)
    val startBackgroundService = {// needs foreground permissions to work indefinitely
        context.startService(Intent(context, BackgroundService::class.java))
    }
    val stopBackgroundService = {
        context.stopService(Intent(context, BackgroundService::class.java))
    }
    val saveNotification = prepareNotification("Saved", "Saved changes to collections and their dice", timeout=10000)
    val saveNotificationPermission = askPermission("android.permission.POST_NOTIFICATIONS") {saveNotification()}
    appState.notifications.save = {
        saveNotificationPermission()
        saveNotification()
    }

    if (!appState.initialized) { // initialize
        createNotificationChannel(context)
        appState.seed = appState.rng.nextLong().absoluteValue%(1 shl 16)
        appState.rng.setSeed(appState.seed)
        val installFile = File(context.filesDir, INSTALL_STATE_FILENAME)
        loadInstallState(installFile)
        appState.initialized = true
    }
    LifecycleEventListener {
        //println(it.targetState.name+" "+it.name)
        when (it){
            Lifecycle.Event.ON_RESUME ->
            {
                stopBackgroundService()
                appState.sensors!!.start()
            }
            Lifecycle.Event.ON_PAUSE ->
            {
                startBackgroundService()
                appState.sensors!!.stop()
            }
            Lifecycle.Event.ON_DESTROY ->
            {
                stopBackgroundService()
                appState.sensors!!.stop()
            }
            else -> {}
        }
    }
    NavHost(navController, startDestination = "primary") {
        composable<DiceCollectionRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<DiceCollectionRoute>()
            val collection = appState.installState.diceCollections.getValue(route.name)
            DiceCollectionView(collection)
        }
        composable("primary") {PrimaryView()}
        composable("newDiceCollection") {NewDiceCollectionView()}
        composable("byteGen") {ByteGenView()}
        composable("statistics") {StatisticsView()}
    }
}
