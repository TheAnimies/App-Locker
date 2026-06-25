package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.AppNavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppLockerViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: AppLockerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Recover singleton instance references (Manual DI / Service Locator)
        val appInstance = application as AppLockerApplication
        val factory = AppLockerViewModel.Factory(appInstance)
        viewModel = ViewModelProvider(this, factory)[AppLockerViewModel::class.java]

        setContent {
            MyApplicationTheme {
                AppNavGraph(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh permissions dynamically when returning to the layout
        if (::viewModel.isInitialized) {
            viewModel.refreshPermissions()
        }
    }
}
