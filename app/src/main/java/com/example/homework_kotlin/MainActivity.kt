package com.example.homework_kotlin


import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

import java.math.BigInteger
import java.util.Random

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
        findViewById<TextView>(R.id.text_byte).text = BigInteger(8, this.rng).toString()
        findViewById<TextView>(R.id.text_byte1).text = BigInteger(8, this.rng).toString()
        findViewById<TextView>(R.id.text_byte2).text = BigInteger(8, this.rng).toString()
        findViewById<TextView>(R.id.text_byte3).text = BigInteger(8, this.rng).toString()
    }
}
