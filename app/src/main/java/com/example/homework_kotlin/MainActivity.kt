package com.example.homework_kotlin


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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.android.volley.toolbox.ImageRequest

import java.math.BigInteger
import java.util.ArrayList
import java.util.Random

const val FULL_ALPHA: Int = 255 shl 24
fun randomFullColor(rng:Random):Int {
    return BigInteger(24, rng).toInt() or FULL_ALPHA
}

class GeneratedByte (rng:Random) {
    val text = BigInteger(8, rng).toString()
    val color = randomFullColor(rng)
    val size = 30F+BigInteger(4, rng).toInt()
}

@Serializable
data class AppInstallState(
    // keep statistics since app was first opened
    var timesOpened:Int = 0,
    val savedFiles:ArrayList<String> = ArrayList<String>()
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
    else {
        saveInstallState(file, installState)
    }
    installState.timesOpened++
    return installState
}




data class AppSessionState (
    val rng : Random,
    val list : SnapshotStateList<GeneratedByte>,
    var installState: AppInstallState,
)


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Homework_kotlinTheme {
                NavigableViews(this.filesDir)
            }
        }
    }
}

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
}


@Composable
fun InputView(appState:AppSessionState, navigationLambdas:NavigationLambdas) {
    val result = remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                result.value = it
            }
        }
    )

    Column {
        Row {
            Button(onClick = {navigationLambdas.back()}) {
                Text("cancel")
            }
            Button(onClick = {navigationLambdas.back()}) {
                Text("confirm")
            }
        }
        Button(onClick = {
            launcher.launch(
                PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)
            )}
        ) {
            Text("select image")
        }
        result.value?.let {
            Image(painter = rememberAsyncImagePainter(result.value),
                null,
                modifier = Modifier.size(150.dp, 150.dp)
                    .padding(16.dp)
            )
        }

        var text by remember { mutableStateOf("") }
        TextField(text, {text = it})
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrimaryView(appState:AppSessionState, navigationLambdas:NavigationLambdas) {
    Column(verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        FlowRow(
            modifier = Modifier
                .weight(1F)
                .verticalScroll(rememberScrollState(0))
        ) {
            for (i in 1..100) {
                Text(i.toString(), Modifier.padding(10.dp), fontSize = 40.sp)
            }
        }

        Button(
            onClick = {navigationLambdas.newDiceSet()},
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .fillMaxWidth()
        ) {
            Text("new set of dice")
        }
        Row {
            Spacer(modifier = Modifier.size(10.dp))
            Button(
                onClick = {navigationLambdas.byteGen()},
                modifier = Modifier
                    .weight(1F)
            ) {
                Text("byte generator")
            }
            Spacer(modifier = Modifier.size(10.dp))
            Button(
                onClick = {navigationLambdas.statistics()},
                modifier = Modifier
                    .weight(1F)
            ) {
                Text("statistics")
            }
            Spacer(modifier = Modifier.size(10.dp))
        }
        Spacer(modifier = Modifier.size(20.dp))
    }
}



@Composable
fun ByteGenView(appState:AppSessionState, navigationLambdas:NavigationLambdas) {
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
            items(appState.list) {
                    item -> Text(item.text, color = Color(item.color), fontSize = item.size.sp)
            }
        }
        Spacer(modifier = Modifier.size(20.dp))
    }
}


@Composable
fun StatisticsView(appState:AppSessionState, navigationLambdas:NavigationLambdas) {
    Column (verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()) {

        Spacer(modifier = Modifier.size(20.dp))
        Button(onClick = {navigationLambdas.back()}
        ) {
            Text("back")
        }
        Text("Times opened: "+appState.installState.timesOpened.toString())
    }
}




class NavigationLambdas(navController:NavController) {
    val statistics = {navController.navigate(route="statistics")}
    val back = {navController.navigateUp()}
    val byteGen = {navController.navigate(route="byteGen")}
    val newDiceSet = {navController.navigate(route="newDiceSet")}
}

@Composable
fun NavigableViews(appFileDir:File) {
    val navController = rememberNavController()
    val installFile = File(appFileDir, "installState")

    val appState = AppSessionState(
        remember {Random()},
        remember {mutableStateListOf()},
        remember {loadInstallState(installFile)},
        //remember {Socket()}
    )
    val navigationLambdas = NavigationLambdas(navController)

    CustomEventListener {
        if (it==Lifecycle.Event.ON_PAUSE) {
            saveInstallState(installFile, appState.installState)
        }
    }
    NavHost(navController, startDestination = "primary", modifier = Modifier.layoutId(0)) {
        composable("primary") {
            PrimaryView(appState = appState, navigationLambdas = navigationLambdas)
        }
        composable("newDiceSet") {
            InputView(appState = appState, navigationLambdas = navigationLambdas)
        }
        composable("byteGen") {
            ByteGenView(appState = appState, navigationLambdas = navigationLambdas)
        }
        composable("statistics") {
            StatisticsView(appState = appState, navigationLambdas = navigationLambdas)
        }
    }
}

