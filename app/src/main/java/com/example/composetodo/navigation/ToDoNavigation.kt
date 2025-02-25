package com.example.composetodo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.composetodo.presenter.TaskPresenter
import com.example.composetodo.view.screens.AddTaskScreen
import com.example.composetodo.view.screens.TaskListScreen

sealed class ToDoDestinations(val route: String) {
    object TaskList : ToDoDestinations("taskList")
    object AddTask : ToDoDestinations("addTask")
}

@Composable
fun ToDoNavigation(viewModel: TaskPresenter) {
    val navController = rememberNavController()

    NavHost(
        navController = navController, 
        startDestination = ToDoDestinations.TaskList.route
    ) {
        composable(ToDoDestinations.TaskList.route) {
            TaskListScreen(
                viewModel = viewModel,
                onNavigateToAddTask = {
                    navController.navigate(ToDoDestinations.AddTask.route)
                }
            )
        }
        composable(ToDoDestinations.AddTask.route) {
            AddTaskScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
} 