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
                    navController.navigate(ToDoDestinations.AddTask.createRoute()) {
                        // Configuración para evitar múltiples instancias
                        launchSingleTop = true
                    }
                },
                onNavigateToCalendar = {
                    navController.navigate(ToDoDestinations.Calendar.route) {
                        // Configuración para evitar múltiples instancias
                        launchSingleTop = true
                    }
                },
                onNavigateToEditTask = { taskId ->
                    navController.navigate(ToDoDestinations.EditTask.createRoute(taskId)) {
                        // Configuración para evitar múltiples instancias
                        launchSingleTop = true
                    }
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
                    // Asegurarse de que existe una pantalla a la que volver
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        // Si no hay pantalla anterior, navegar a la lista de tareas
                        navController.navigate(ToDoDestinations.TaskList.route) {
                            popUpTo(ToDoDestinations.TaskList.route) { inclusive = true }
                        }
                    }
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
                    // Asegurarse de que existe una pantalla a la que volver
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        // Si no hay pantalla anterior, navegar a la lista de tareas
                        navController.navigate(ToDoDestinations.TaskList.route) {
                            popUpTo(ToDoDestinations.TaskList.route) { inclusive = true }
                        }
                    }
                },
                taskId = taskId,
                isEditMode = true
            )
        }

        composable(ToDoDestinations.Calendar.route) {
            CalendarScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    // Asegurarse de que existe una pantalla a la que volver
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        // Si no hay pantalla anterior, navegar a la lista de tareas
                        navController.navigate(ToDoDestinations.TaskList.route) {
                            popUpTo(ToDoDestinations.TaskList.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToAddTask = { date ->
                    navController.navigate(ToDoDestinations.AddTask.createRoute(date)) {
                        // Configuración para evitar múltiples instancias
                        launchSingleTop = true
                    }
                },
                onNavigateToEditTask = { taskId ->
                    navController.navigate(ToDoDestinations.EditTask.createRoute(taskId)) {
                        // Configuración para evitar múltiples instancias
                        launchSingleTop = true
                    }
                }
            )
        }
    }
} 