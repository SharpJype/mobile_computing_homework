package com.example.homework_kotlin


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.homework_kotlin.ui.theme.Homework_kotlinTheme

import java.math.BigInteger
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

data class AppState (
    val rng : Random,
    val list : SnapshotStateList<GeneratedByte>,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent() {
            Homework_kotlinTheme {
                NavigatableViews()
            }
        }
    }
}



@Composable
fun PrimaryView(appState:AppState, secondaryNavigationLambda:() -> Unit) {
    Column (verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().layoutId("Main")) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {//modifier = Modifier.weight(1F),
            Spacer(modifier = Modifier.size(20.dp))
            Image(painter = painterResource(
                id = R.drawable.campfire_w_sword),
                "image",
                modifier = Modifier.size(200.dp)
            )
            TextButton(onClick = {secondaryNavigationLambda()}) {//context.setSecondView()
                Text("second view")
            }
            Button(onClick = {
                appState.list.add(GeneratedByte(appState.rng)) },
            ) {
                Text("generate bytes", modifier = Modifier.layoutId("buttonText"))
            }
        }
        LazyColumn (horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1F)) {
            items(appState.list) {
                    item -> Text(item.text, color = Color(item.color), fontSize = item.size.sp)
            }
        }
        Spacer(modifier = Modifier.size(20.dp))
    }
}

@Composable
fun SecondView(appState:AppState, primaryNavigationLambda:() -> Unit) {
    Column (verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()) {

        Spacer(modifier = Modifier.size(20.dp))
        Button(onClick = {
            primaryNavigationLambda()
        }) {
            Text("back")
        }
    }
}



@Composable
fun NavigatableViews() {
    val navController = rememberNavController()
    val appState = AppState(Random(), remember { mutableStateListOf<GeneratedByte>()})

    NavHost(navController, startDestination = "primary", modifier = Modifier.layoutId(0)) {
        composable("primary") { backStackEntry ->
            PrimaryView(
                appState = appState,
                secondaryNavigationLambda = {
                    navController.navigate(route = "secondary")
                }
            )
        }
        composable("secondary") {
            SecondView(
                appState = appState,
                primaryNavigationLambda = {
                    navController.navigateUp()
                    //navController.navigate(route = "primary") // CYCLICAL
                }
            )
        }
    }
}

