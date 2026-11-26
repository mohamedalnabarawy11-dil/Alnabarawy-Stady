package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.core.configureAppCheck
import com.example.ui.MainViewModel
import com.example.ui.NabraawyAppRoot

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent BEFORE Compose UI / ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            NabraawyAppRoot(
                viewModel = viewModel,
                onExitApp = { finishAffinity() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.soundEngine.onForeground()
    }

    override fun onPause() {
        viewModel.soundEngine.onBackground()
        super.onPause()
    }
}
