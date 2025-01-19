package com.example.homework_kotlin


import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

import java.math.BigInteger
import java.util.Random

val FULL_ALPHA: Int = 255 shl 24
fun randomFullColor(rng:Random):Int {
    return BigInteger(24, rng).toInt() or FULL_ALPHA
}

class MainActivity : ComponentActivity() {
    private val rng = Random()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main)
        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
                this.generateBytes()
            }
    }

    private fun generateBytes() {
        val new_view = TextView(this)
        new_view.text = BigInteger(8, this.rng).toString()
        new_view.gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
        new_view.setTextSize(30F+BigInteger(4, this.rng).toInt())
        new_view.setTextColor(randomFullColor(this.rng))
        findViewById<LinearLayout>(R.id.list).addView(new_view)
    }
}

