package com.example.storyforge.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF121212),  // True dark background
    surface = Color(0xFF1E1E1E),     // Slightly lighter surfaces
    onBackground = Color.White,      // Text on dark bg must be light!
    onSurface = Color.White,         // Text on surfaces must be light!
    onPrimary = Color.Black,         // Text on primary buttons
    onSecondary = Color.Black,       // Text on secondary elements
    onTertiary = Color.Black         // Text on tertiary elements
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),  // Light background
    surface = Color(0xFFFFFBFE),     // Surface matches background
    onBackground = Color(0xFF1C1B1F), // Dark text on light bg
    onSurface = Color(0xFF1C1B1F),    // Dark text on surfaces
    onPrimary = Color.White,          // Text on primary buttons
    onSecondary = Color.White,        // Text on secondary elements
    onTertiary = Color.White          // Text on tertiary elements
)

@Composable
fun StoryForgeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme, // Always use dark
        typography = Typography,
        content = content
    )
}

// [Existing theme code...]
// Replace all previews with just this:
@Preview(showBackground = true)
@Composable
fun AppPreview() {
    StoryForgeTheme { // No darkTheme parameter needed
        Surface(modifier = Modifier.fillMaxSize()) {
            Text(
                "Forced Dark Mode Active",
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}