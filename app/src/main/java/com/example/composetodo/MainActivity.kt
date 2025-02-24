package com.example.composetodo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.composetodo.presenter.TaskViewModel
import com.example.composetodo.ui.screens.AddTaskScreen
import com.example.composetodo.ui.screens.TaskListScreen
import com.example.composetodo.ui.theme.ComposeToDoTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeToDoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ToDoApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun ToDoApp(viewModel: TaskViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "taskList") {
        composable("taskList") {
            TaskListScreen(
                viewModel = viewModel,
                onNavigateToAddTask = {
                    navController.navigate("addTask")
                }
            )
        }
        composable("addTask") {
            AddTaskScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}