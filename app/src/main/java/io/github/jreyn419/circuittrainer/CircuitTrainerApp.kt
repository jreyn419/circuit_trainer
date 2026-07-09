package io.github.jreyn419.circuittrainer

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.jreyn419.circuittrainer.ui.edit.EditWorkoutScreen
import io.github.jreyn419.circuittrainer.ui.home.HomeScreen
import io.github.jreyn419.circuittrainer.ui.library.ExerciseLibraryScreen
import io.github.jreyn419.circuittrainer.ui.play.PlayerScreen
import io.github.jreyn419.circuittrainer.ui.settings.SettingsScreen

private const val NEW_WORKOUT_ID = "new"

@Composable
fun CircuitTrainerApp(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onCreateWorkout = { navController.navigate("edit/$NEW_WORKOUT_ID") },
                onEditWorkout = { id -> navController.navigate("edit/$id") },
                onPlayWorkout = { id -> navController.navigate("play/$id") },
                onOpenExercises = { navController.navigate("exercises") },
                onOpenSettings = { navController.navigate("settings") },
            )
        }
        composable("edit/{workoutId}") { entry ->
            val id = entry.arguments?.getString("workoutId")
            EditWorkoutScreen(
                workoutId = id?.takeUnless { it == NEW_WORKOUT_ID },
                onBack = { navController.popBackStack() },
                onOpenLibrary = { navController.navigate("exercises") },
            )
        }
        composable("exercises") {
            ExerciseLibraryScreen(onBack = { navController.popBackStack() })
        }
        composable("play/{workoutId}") { entry ->
            val id = entry.arguments?.getString("workoutId").orEmpty()
            PlayerScreen(
                workoutId = id,
                onExit = { navController.popBackStack() },
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
