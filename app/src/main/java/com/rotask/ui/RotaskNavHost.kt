package com.rotask.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rotask.RotaskApplication
import com.rotask.ui.home.HomeScreen
import com.rotask.ui.home.HomeViewModel
import com.rotask.ui.work.WorkScreen
import com.rotask.ui.work.WorkViewModel

@Composable
fun RotaskNavHost(application: RotaskApplication) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val vm: HomeViewModel = viewModel(
                factory = HomeViewModel.factory(application.repository)
            )
            HomeScreen(
                vm = vm,
                onStartWork = { taskId -> navController.navigate("work/$taskId") }
            )
        }
        composable(
            route = "work/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { entry ->
            val taskId = entry.arguments?.getLong("taskId") ?: 0L
            val vm: WorkViewModel = viewModel(
                factory = WorkViewModel.factory(
                    application.repository,
                    application.appScope,
                    application.soundPlayer,
                    taskId,
                )
            )
            WorkScreen(
                vm = vm,
                onFinish = { navController.popBackStack() }
            )
        }
    }
}
