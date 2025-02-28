package com.example.composetodo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
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
    data object TaskList : ToDoDestinations("taskList")
    
    data object AddTask : ToDoDestinations("addTask?date={date}") {
        fun createRoute(date: LocalDate? = null) = if (date != null) {
            "addTask?date=$date"
        } else {
            "addTask"
        }
    }
    
    data object EditTask : ToDoDestinations("editTask/{taskId}") {
        fun createRoute(taskId: Int) = "editTask/$taskId"
    }
    
    data object Calendar : ToDoDestinations("calendar")
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
                    navController.navigateSingleTop(ToDoDestinations.AddTask.createRoute())
                },
                onNavigateToCalendar = {
                    navController.navigateSingleTop(ToDoDestinations.Calendar.route)
                },
                onNavigateToEditTask = { taskId ->
                    navController.navigateSingleTop(ToDoDestinations.EditTask.createRoute(taskId))
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
                onNavigateBack = { navController.safeNavigateBack() },
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
                onNavigateBack = { navController.safeNavigateBack() },
                taskId = taskId,
                isEditMode = true
            )
        }

        composable(ToDoDestinations.Calendar.route) {
            CalendarScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.safeNavigateBack() },
                onNavigateToAddTask = { date ->
                    navController.navigateSingleTop(ToDoDestinations.AddTask.createRoute(date))
                },
                onNavigateToEditTask = { taskId ->
                    navController.navigateSingleTop(ToDoDestinations.EditTask.createRoute(taskId))
                }
            )
        }
    }
}

private fun NavController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

private fun NavController.safeNavigateBack() {
    if (previousBackStackEntry != null) {
        popBackStack()
    } else {
        navigate(ToDoDestinations.TaskList.route) {
            popUpTo(ToDoDestinations.TaskList.route) { inclusive = true }
        }
    }
} 