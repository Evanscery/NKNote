package com.example.nknote.ui.navigation

sealed class Destinations(val route: String) {
    object HomePage : Destinations("home")
    object NoteEditPage : Destinations("edit")
    object NoteCheckPage : Destinations("check/{noteId}") {
        const val args = "noteId"
    }
    object RandomPage : Destinations("random")
} 