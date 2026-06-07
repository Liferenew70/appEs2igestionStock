package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.StockApp
import com.example.ui.StockViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val vm: StockViewModel = viewModel()
      val isDark by vm.isDarkMode.collectAsState()
      val customAccent by vm.customAccentHex.collectAsState()

      MyApplicationTheme(darkTheme = isDark, customAccentHex = customAccent) {
        StockApp(viewModel = vm)
      }
    }
  }
}

