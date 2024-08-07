package com.example.nknote.ui.components

/*
* Class for navigation controller
*
*
*/
import android.provider.ContactsContract.CommonDataKinds.Note
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavArgs
import androidx.navigation.NavDirections
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nknote.ui.navigation.Destinations
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
            MainFrame(onNavToNoteEditPage = { navController.navigate(Destinations.NoteEditPage.route) },
                onNavToNoteCheckPage = {
                    navController.navigate("${Destinations.NoteCheckPage.route}/${it}"){ launchSingleTop = true }
                },
                onNavToRandomPage = {
                    navController.navigate(Destinations.RandomPage.route)
                }
            )
        }
        composable(Destinations.NoteEditPage.route){
            NoteEditPage({ navController.navigate(Destinations.HomePage.route){ launchSingleTop = true } })
        }

        composable(route = "${Destinations.NoteCheckPage.route}/{${Destinations.NoteCheckPage.args}}",
            arguments = listOf(navArgument(Destinations.NoteCheckPage.args){
            }))
        {
                NoteCheckPage(onNavBack = {navController.popBackStack()})
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

