package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.MainAppContainer
import com.example.ui.theme.ExpenseTrackerTheme
import com.example.viewmodel.ExpenseViewModel
import com.example.viewmodel.ExpenseViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels {
        val app = application as ExpenseApplication
        ExpenseViewModelFactory(
            application = app,
            repository = app.repository,
            database = app.database
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ExpenseTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainAppContainer(viewModel = viewModel)
                }
            }
        }
    }
}

