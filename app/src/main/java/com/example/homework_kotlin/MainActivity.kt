package com.example.homework_kotlin


import android.content.res.Resources.Theme
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.findNavController
import com.example.homework_kotlin.ui.theme.Homework_kotlinTheme

import java.math.BigInteger
import java.util.Random

const val FULL_ALPHA: Int = 255 shl 24
fun randomFullColor(rng:Random):Int {
    return BigInteger(24, rng).toInt() or FULL_ALPHA
}

class MainActivity : ComponentActivity() {
    private val rng = Random()

    class GeneratedByte (rng:Random) {
        val text = BigInteger(8, rng).toString()
        val color = randomFullColor(rng)
        val size = 30F+BigInteger(4, rng).toInt()
    }

    var genList = ArrayList<GeneratedByte>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        this.setMainView()

    }

    private fun setMainView() {
        //setContentView(R.layout.main)
        setContent() {
            Homework_kotlinTheme {
                MainView(this)
            }
        }
        /*
        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            //this.generateBytes()
        }

        val buttonSecond = findViewById<Button>(R.id.buttonSecond)
        buttonSecond.setOnClickListener {
            this.setSecondView()
        }*/
    }

    private fun setSecondView() {
        setContentView(R.layout.second)
        val buttonBack = findViewById<Button>(R.id.buttonBack)
        buttonBack.setOnClickListener {
            this.setMainView()
        }
    }

    fun generateBytes() {
        //val new_view = TextView(this)
        //new_view.text = BigInteger(8, this.rng).toString()
        //new_view.gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
        //new_view.setTextSize(30F+BigInteger(4, this.rng).toInt())
        //new_view.setTextColor(randomFullColor(this.rng))
        this.genList.add(GeneratedByte(this.rng))
        this.setMainView()
        //findViewById<LinearLayout>(R.id.scroll).addView(new_view)
    }
}

@Composable
fun LazyColumnComp(listOfItems:List<MainActivity.GeneratedByte>) {
    LazyColumn () {
        items(listOfItems) {
            item -> Text(item.text, color = Color(item.color), fontSize = item.size.sp)
        }
    }
}

@Composable
fun MainView(context:MainActivity) {
    Column {
        Image(painter = painterResource(
            id = R.drawable.campfire_w_sword),
            "image")
        Button(onClick = {
            context.generateBytes()
        })
        { Text("generate bytes", modifier = Modifier.layoutId("buttonText")) }
        LazyColumnComp(context.genList)
    }
}

/*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldExample() {
    var presses by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.layoutId("scaffold"),
        topBar = {
            /*
            TopAppBar(title={ Text("title") },
                colors = TopAppBarColors(Color.Blue, Color.Green, Color.Red, Color.White, Color.Red))
        */
            Text("title")
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = "Bottom app bar",
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { presses++ }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier.padding(8.dp),
                text =
                """
                    This is an example of a scaffold. It uses the Scaffold composable's parameters to create a screen with a simple top app bar, bottom app bar, and floating action button.

                    It also contains some basic inner content, such as this text.

                    You have pressed the floating action button $presses times.
                """.trimIndent(),
            )
        }
    }
}*/