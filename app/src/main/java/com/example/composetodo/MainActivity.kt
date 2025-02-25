package com.example.composetodo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.composetodo.navigation.ToDoNavigation
import com.example.composetodo.presenter.TaskPresenter
import com.example.composetodo.ui.theme.ComposeToDoTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TaskPresenter by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeToDoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ToDoNavigation(viewModel)
                }
            }
        }
    }
}