package com.example.composetodo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.composetodo.presenter.TaskPresenter
import com.example.composetodo.view.screens.AddTaskScreen
import com.example.composetodo.view.screens.CalendarScreen
import com.example.composetodo.view.screens.TaskListScreen
import java.time.LocalDate

sealed class ToDoDestinations(val route: String) {
    object TaskList : ToDoDestinations("taskList")
    object AddTask : ToDoDestinations("addTask?date={date}") {
        fun createRoute(date: LocalDate? = null) = if (date != null) {
            "addTask?date=$date"
        } else {
            "addTask"
        }
    }
    object EditTask : ToDoDestinations("editTask/{taskId}") {
        fun createRoute(taskId: Int) = "editTask/$taskId"
    }
    object Calendar : ToDoDestinations("calendar")
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
                    navController.navigate(ToDoDestinations.AddTask.createRoute())
                },
                onNavigateToCalendar = {
                    navController.navigate(ToDoDestinations.Calendar.route)
                },
                onNavigateToEditTask = { taskId ->
                    navController.navigate(ToDoDestinations.EditTask.createRoute(taskId))
                }
            )
        }

        composable(
            route = ToDoDestinations.AddTask.route,
            arguments = listOf(
                navArgument("date") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val dateStr = backStackEntry.arguments?.getString("date")
            val date = dateStr?.let { LocalDate.parse(it) }
            
            AddTaskScreen(
                viewModel = viewModel,
                initialDate = date,
                onNavigateBack = {
                    navController.popBackStack()
                },
                isEditMode = false
            )
        }

        composable(
            route = ToDoDestinations.EditTask.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getInt("taskId") ?: 0
            
            AddTaskScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                taskId = taskId,
                isEditMode = true
            )
        }

        composable(ToDoDestinations.Calendar.route) {
            CalendarScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddTask = { date ->
                    navController.navigate(ToDoDestinations.AddTask.createRoute(date))
                },
                onNavigateToEditTask = { taskId ->
                    navController.navigate(ToDoDestinations.EditTask.createRoute(taskId))
                }
            )
        }
    }
} 