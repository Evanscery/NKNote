package com.example.nknote.models.entity

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * TODO
 *
 * @property text Description of navigation destiny
 * @property Icon Icon of navigation
 */
data class DrawerNavigationItem(
        val text : String,
        val icon : ImageVector? = null,
        val iconResourceId : Int = 0
)

