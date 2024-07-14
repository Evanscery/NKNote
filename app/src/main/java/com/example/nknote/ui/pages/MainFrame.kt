package com.example.nknote.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nknote.R
import com.example.nknote.models.entity.DrawerNavigationItem
import com.example.nknote.ui.theme.NKNoteTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.example.nknote.ui.components.SimpleTextField

@Preview("HomePage")
@Composable
fun AppPreview() {
    MainFrame()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFrame() {
    NKNoteTheme {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var currentNavigationIndex by remember { mutableStateOf(0) }
        //SideBar
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                if(drawerState.isClosed)
                    currentNavigationIndex=-1
                //if the drawer is opened,use return to close it instead of closing program
                BackHandler(drawerState.isOpen) {
                    scope.launch { drawerState.close() }
                }
                ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    /* Drawer content */
                    val navigationItems = listOf(
                        DrawerNavigationItem(text = "统计", icon = Icons.Filled.Person),
                        DrawerNavigationItem(text = "同步", icon = Icons.Filled.Refresh),
                        DrawerNavigationItem(text = "探索", icon = Icons.Filled.Face),
                        DrawerNavigationItem(text = "回收站", icon = Icons.Filled.Delete),
                    )

                    Surface(modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.25f),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ){
                        Row (verticalAlignment = Alignment.Bottom) {
                            Image(
                                painter = painterResource(R.drawable.bocchi),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .border(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                            )
                            Text(text = "Greetings",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 10.dp),
                                style = MaterialTheme.typography.headlineLarge,
                                textAlign = TextAlign.End)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 12.dp),
                        thickness = 2.dp)
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        navigationItems.forEachIndexed { index, drawerNavigationItem ->
                            NavigationDrawerItem(label = { Text(text = drawerNavigationItem.text,
                                color = MaterialTheme.colorScheme.onSecondaryContainer) },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    unselectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                icon = {
                                    Icon(
                                        drawerNavigationItem.icon,
                                        contentDescription = drawerNavigationItem.text
                                    )
                                },

                                selected = currentNavigationIndex == index,
                                onClick = {
                                    currentNavigationIndex = index
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }
                }
            },
            gesturesEnabled = drawerState.isOpen,

            //main part of the home page
            content = {
                AppHomePageMainContent(scope = scope,
                    drawerState = drawerState,
                    currentNavigationIndex = currentNavigationIndex,
                    )
            }
        )
    }
}

@Composable
//The composable function of the main page, to display the notes
fun AppHomePageMainContent(
    scope : CoroutineScope,
    drawerState: DrawerState,
    currentNavigationIndex : Int,
    modifier: Modifier = Modifier
){
    var searchState by remember { mutableStateOf(false) }
    var searchTextValue by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            BackHandler(searchState) {
                searchState=(!searchState)
            }
                if(!searchState)
                {
                    AppNavigationTopBar(scope = scope,
                        drawerState = drawerState,
                        onSearchButtonClick = { searchState=(!searchState) }
                    )
                }
                else
                {
                    AppSearchTopBar(
                        scope = scope,
                        drawerState = drawerState,
                        searchTextValue = searchTextValue,
                        onSearchTextValueChange = {searchTextValue = it},
                        onSearchButtonClick = { searchState=(!searchState) }
                    )
                }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {}
            )
            {
                Icon(Icons.Filled.Add, contentDescription = "")
            }
        },
        content = {
            it
            Column(modifier=Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center){
                Text(text = "Current choose is $searchState",
                    style = MaterialTheme.typography.titleLarge)

            }

        }
    )
}

@Composable
//The composable function of top bar
fun AppNavigationTopBar(
    scope : CoroutineScope,
    drawerState: DrawerState,
    onSearchButtonClick : () ->Unit,
    modifier: Modifier = Modifier
) {
    NKNoteTheme {
        Surface(color = MaterialTheme.colorScheme.primary,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ){
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.padding(12.dp))
                IconButton(onClick = {  }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null
                    )
                }
                Text(text = "2024/7/7", fontSize = 20.sp, lineHeight = 25.sp, textAlign = TextAlign.End)
                //TextField(value = "0", onValueChange ={} )
                Row(
                    modifier = modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End){
                    IconButton(onClick = onSearchButtonClick) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                }

            }
        }
    }
}

@Composable
fun AppSearchTopBar(
    scope : CoroutineScope,
    drawerState: DrawerState,
    searchTextValue : String,
    onSearchTextValueChange : (String)->Unit,
    onSearchButtonClick : () ->Unit,
    modifier: Modifier = Modifier
){
    NKNoteTheme {
        Surface(color = MaterialTheme.colorScheme.primary,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ){
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.padding(12.dp))
                TextField(value = searchTextValue,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = {Text("Search Note")},
                    onValueChange = onSearchTextValueChange,
                    modifier = Modifier.padding(5.dp))
//                SimpleTextField(value = searchTextValue,
//                    onValueChange = onSearchTextValueChange,
//                    singleLine = true
//                )
                Row(
                    modifier = modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End){
                    IconButton(onClick = onSearchButtonClick) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null
                        )
                    }
                }

            }
        }
    }
}