package com.tripsplit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF0F6B5C), background = Color(0xFFF6F7F4), surface = Color(0xFFF6F7F4))) {
                App()
            }
        }
    }
}

@Composable
fun App(vm: TripVM = viewModel()) {
    val s = vm.screen
    BackHandler(s != Screen.Home) {
        vm.screen = if (s is Screen.Add) Screen.Detail(s.id) else Screen.Home
    }
    when (s) {
        Screen.Home -> HomeScreen(vm)
        Screen.Create -> CreateScreen(vm)
        is Screen.Detail -> vm.trips.find { it.id == s.id }?.let { DetailScreen(it, vm) } ?: HomeScreen(vm)
        is Screen.Add -> vm.trips.find { it.id == s.id }?.let { AddScreen(it, vm) } ?: HomeScreen(vm)
    }
}
