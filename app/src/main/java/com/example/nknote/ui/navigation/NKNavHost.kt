package com.example.nknote.ui.navigation

/*
* Class for navigation controller
*
*
*/
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.nknote.ui.pages.MainFrame
import com.example.nknote.ui.pages.NKRandomPage
import com.example.nknote.ui.pages.NoteCheckPage
import com.example.nknote.ui.pages.NoteEditPage

@Composable
fun NKNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Destinations.HomePage.route) {
        //home page main frame
        composable(route = Destinations.HomePage.route){
            MainFrame(
                onNavToNoteEditPage = { navController.navigate(Destinations.NoteEditPage.route) },
                onNavToNoteCheckPage = { noteId ->
                    navController.navigate("check/$noteId") {
                        launchSingleTop = true
                    }
                },
                onNavToRandomPage = {
                    navController.navigate(Destinations.RandomPage.route)
                }
            )
        }
        composable(Destinations.NoteEditPage.route){
            NoteEditPage({ navController.navigate(Destinations.HomePage.route){ launchSingleTop = true } })
        }

        composable(
            route = "check/{noteId}",
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getInt("noteId") ?: 0
            NoteCheckPage(
                onNavBack = { navController.popBackStack() },
                noteId = noteId
            )
        }

        composable(route = Destinations.RandomPage.route)
        {
            NKRandomPage(onNavBack = {navController.popBackStack()})
        }

    }
}

@Preview
@Composable
fun NKNavHostPreview() {
    NKNavHost()
}

