package com.pangu.micalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToLong

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CalculatorApp() }
    }
}

@Composable
fun CalculatorApp() {
    var dark by remember { mutableStateOf(isSystemInDarkTheme()) }
    val scheme = if (dark) dynamicDarkColorScheme(androidx.compose.ui.platform.LocalContext.current)
                 else dynamicLightColorScheme(androidx.compose.ui.platform.LocalContext.current)
    MaterialTheme(colorScheme = scheme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            CalculatorScreen(dark = dark, onTheme = { dark = !dark })
        }
    }
}

@Composable
fun CalculatorScreen(dark: Boolean, onTheme: () -> Unit) {
    var expression by remember { mutableStateOf("0") }
    var result by remember { mutableStateOf("") }
    var justEvaluated by remember { mutableStateOf(false) }

    fun input(value: String) {
        if (justEvaluated && value.firstOrNull()?.isDigit() == true) expression = "0"
        justEvaluated = false
        expression = when {
            expression == "0" && value.firstOrNull()?.isDigit() == true -> value
            expression == "Error" -> value
            else -> expression + value
        }
        result = ""
    }
    fun clear() { expression = "0"; result = ""; justEvaluated = false }
    fun backspace() { if (expression.length <= 1) expression = "0" else expression = expression.dropLast(1); result = "" }
    fun calculate() {
        try {
            val value = SimpleEvaluator(expression).parse()
            result = if (value.isFinite()) {
                val rounded = value.roundToLong().toDouble()
                if (kotlin.math.abs(value - rounded) < 1e-10) rounded.toLong().toString() else value.toString()
            } else "Error"
            justEvaluated = true
        } catch (_: Exception) { result = "Error" }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Calculator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("MI-inspired · Material You", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onTheme) {
                Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Toggle theme")
            }
        }
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth().padding(bottom = 22.dp), horizontalAlignment = Alignment.End) {
            Text(expression, fontSize = 34.sp, lineHeight = 42.sp, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
            Spacer(Modifier.height(12.dp))
            Text(result.ifEmpty { " " }, fontSize = 27.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.End)
        }
        val rows = listOf(
            listOf("AC", "÷", "%", "⌫"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("±", "0", ".", "=")
        )
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    val isOperator = key in listOf("÷", "×", "−", "+", "=")
                    val isUtility = key in listOf("AC", "%", "⌫", "±")
                    val container = when {
                        key == "=" -> MaterialTheme.colorScheme.primary
                        isOperator -> MaterialTheme.colorScheme.secondaryContainer
                        isUtility -> MaterialTheme.colorScheme.surfaceContainerHigh
                        else -> MaterialTheme.colorScheme.surfaceContainer
                    }
                    val content = if (key == "=") MaterialTheme.colorScheme.onPrimary else if (isOperator) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                    Button(
                        onClick = {
                            when (key) {
                                "AC" -> clear()
                                "⌫" -> backspace()
                                "=" -> calculate()
                                "±" -> { expression = if (expression.startsWith("-")) expression.drop(1) else "-$expression" }
                                "÷" -> input("/")
                                "×" -> input("*")
                                "−" -> input("-")
                                else -> input(key)
                            }
                        },
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        if (key == "⌫") Icon(Icons.Default.Backspace, contentDescription = "Delete")
                        else Text(key, fontSize = if (key.length > 1) 19.sp else 25.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("Designed with Material 3", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private class SimpleEvaluator(private val source: String) {
    private var pos = 0
    fun parse(): Double { val v = expression(); if (pos < source.length) error("Unexpected input"); return v }
    private fun expression(): Double {
        var x = term()
        while (pos < source.length) {
            when (source[pos]) { '+' -> { pos++; x += term() }; '-' -> { pos++; x -= term() }; else -> return x }
        }
        return x
    }
    private fun term(): Double {
        var x = factor()
        while (pos < source.length) {
            when (source[pos]) {
                '*' -> { pos++; x *= factor() }
                '/' -> { pos++; val d = factor(); if (d == 0.0) error("Divide by zero"); x /= d }
                '%' -> { pos++; x /= 100.0 }
                else -> return x
            }
        }
        return x
    }
    private fun factor(): Double {
        if (pos < source.length && (source[pos] == '+' || source[pos] == '-')) {
            val sign = if (source[pos++] == '-') -1.0 else 1.0
            return sign * factor()
        }
        if (pos < source.length && source[pos] == '(') { pos++; val x = expression(); if (pos >= source.length || source[pos++] != ')') error("Missing )"); return x }
        val start = pos
        while (pos < source.length && (source[pos].isDigit() || source[pos] == '.')) pos++
        if (start == pos) error("Number expected")
        return source.substring(start, pos).toDouble()
    }
}
