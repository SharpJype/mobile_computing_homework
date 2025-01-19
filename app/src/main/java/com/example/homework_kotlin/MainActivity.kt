package com.example.homework_kotlin


import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.homework_kotlin.ui.theme.Homework_kotlinTheme

import java.lang.Math
import java.math.BigInteger
import java.util.Random

class MainActivity : ComponentActivity() {
    private var state = false
    private val rng = Random()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        this.toggle()
    }

    private fun toggle() {
        //this.state = !state
        //val num = Math.random()
        val num = BigInteger(8, this.rng)
        setContent {
            MainContentBlock(num)
        }
    }
}








@Composable
fun Element(string: String, modifier: Modifier = Modifier) {
    Text(
        text = "state: $string",
        modifier = modifier
    )
}

@Composable
fun MainContentBlock(x: Any) {
    Homework_kotlinTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Element(x.toString(), modifier = Modifier.padding(innerPadding))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MainContentBlock(false)
}