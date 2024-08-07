package com.example.nknote.ui.navigation

/**
 * TODO
 * Sealed class for compose navigation
 *
 *
 */
enum class Destinations(val route : String,
    val args : String = "itemId")  {
    HomePage(route ="HomePage"),
    NoteCheckPage(route = "NoteCheckPage"),
    NoteEditPage(route = "NoteEditPage"),
    RandomPage(route = "RandomPage");
}

