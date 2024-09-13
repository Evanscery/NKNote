package com.example.nknote.ui.pages

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nknote.AppViewModelProvider
import com.example.nknote.R
import com.example.nknote.data.NoteItem
import com.example.nknote.models.entity.DrawerNavigationItem
import com.example.nknote.ui.theme.NKNoteTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

@Preview("HomePage")
@Composable
fun AppPreview() {
    MainFrame()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFrame(onNavToNoteEditPage : ()->Unit = {},
              onNavToNoteCheckPage : (String)->Unit = {},
              onNavToRandomPage : ()->Unit = {},
              viewModel: MainFrameViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    NKNoteTheme {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var currentNavigationIndex by remember { mutableStateOf(0) }
        val mainFrameUiState by viewModel.mainFrameUiState.collectAsState()
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
                        DrawerNavigationItem(text = stringResource(R.string.homepage_sidebar_stastics_chs), icon = Icons.Filled.Person),
                        DrawerNavigationItem(text = stringResource(R.string.homepage_sidebar_update_chs), icon = Icons.Filled.Refresh),
                        DrawerNavigationItem(text = stringResource(R.string.homepage_sidebar_explore_chs), icon = Icons.Filled.Face),
                        DrawerNavigationItem(text = stringResource(R.string.homepage_sidebar_recyclebin_chs), icon = Icons.Filled.Delete),
                        DrawerNavigationItem(text = "随机数",icon = null, iconResourceId = R.drawable.roll)
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
                                    if(drawerNavigationItem.iconResourceId ==0) {
                                        Icon(
                                            drawerNavigationItem.icon!!,
                                            contentDescription = drawerNavigationItem.text
                                        )
                                    }
                                    else{
                                        Icon(
                                            ImageBitmap.imageResource(id = drawerNavigationItem.iconResourceId),
                                            contentDescription = drawerNavigationItem.text
                                        )
                                    }
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
                    onNoteItemClicked = onNavToNoteCheckPage,
                    currentNavigationIndex = currentNavigationIndex,
                    mainFrameUiState = mainFrameUiState,
                    onNavClicked = { onNavToNoteEditPage() },
                    onItemDeleteClick = {
                        Log.d("asd","note:$it,should has been deleted")
                        scope.launch {
                            viewModel.deleteItemById(it)
                        }
                    }
                    )
            }
        )
        when(currentNavigationIndex)
        {
            4 ->{
                    currentNavigationIndex = 0
                    onNavToRandomPage()
            }
            else ->{
            }
        }
    }
}

@Composable
//The composable function of the main page, to display the notes
fun AppHomePageMainContent(
    scope : CoroutineScope,
    drawerState: DrawerState,
    onNoteItemClicked: (String) -> Unit = {},
    onNavClicked: () -> Unit = {},
    onItemDeleteClick: (String) -> Unit,
    currentNavigationIndex : Int,
    mainFrameUiState : MainFrameUiState,
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
                onClick = { onNavClicked.invoke() }
            )
            {
                Icon(Icons.Filled.Add, contentDescription = "")
            }
        },
        content = {
                innerPadding ->
            AppNoteListBody(
                itemList = mainFrameUiState.itemList,
                onItemClick = onNoteItemClicked,
                modifier = modifier.fillMaxSize(),
                onItemDeleteClick = onItemDeleteClick,
                contentPadding = innerPadding,
            )
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
                val currentDate = SimpleDateFormat("yyyy-MM-dd").format(Date())
                Text(text = currentDate, fontSize = 20.sp, lineHeight = 25.sp, textAlign = TextAlign.End)
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

@Composable
fun AppNoteListBody(itemList: List<NoteItem>,
                    onItemClick: (String) -> Unit,
                    onItemDeleteClick: (String) -> Unit,
                    modifier: Modifier = Modifier,
                    contentPadding: PaddingValues = PaddingValues(0.dp),)
{
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        if (itemList.isEmpty()) {
            Text(
                text = stringResource(R.string.homepage_no_note_description_chs),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(contentPadding),
            )
        } else {
            MainFrameNoteList(
                itemList = itemList,
                onItemClick = { onItemClick(it.id) },
                onItemMenuDeleteClick = {onItemDeleteClick(it.id)},
                contentPadding = contentPadding,
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_small))
            )
        }
    }
}

@Composable
fun MainFrameNoteList(
    itemList: List<NoteItem>,
    onItemClick: (NoteItem) -> Unit,
    onItemMenuDeleteClick: (NoteItem)->Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
)
{
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding
    ) {
        items(items = itemList, key = { it.id } ) { item ->
            MainFrameNoteItem(item = item,
                modifier = Modifier
                    .padding(dimensionResource(id = R.dimen.padding_small)),
                onItemClick = {onItemClick(item)},
                onMenuDeleteClick = {onItemMenuDeleteClick(item)})
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainFrameNoteItem(item: NoteItem,
                      onItemClick: (NoteItem) -> Unit,
                      onMenuDeleteClick : (NoteItem) ->Unit,
                      modifier: Modifier = Modifier)
{
    var menuExpanded by remember { mutableStateOf(false)}
    Card(
        shape = CardDefaults.outlinedShape,
        border = BorderStroke(2.dp,MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .padding(5.dp)
            .wrapContentSize()
    ){
        Box(
            modifier = Modifier.combinedClickable (
                onLongClick = {menuExpanded=true},
                onClick = {onItemClick(item)},
                            )
        ){
            Column(
                modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_large)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_small))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = item.date,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        noteFloatingMenu(
            menuExpanded = menuExpanded,
            onMenuExpandedStateChange = { menuExpanded = !menuExpanded },
            onMenuDeleteClick = {onMenuDeleteClick(item)})
        }
    }



@Composable
fun noteFloatingMenu(
    menuExpanded : Boolean = false,
    onMenuExpandedStateChange: ()-> Unit,
    onMenuDeleteClick: () -> Unit,
){
    DropdownMenu(expanded = menuExpanded,
        onDismissRequest = { onMenuExpandedStateChange() }) {
        DropdownMenuItem(onClick = { onMenuExpandedStateChange()
            onMenuDeleteClick()},
            text ={
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ){
                    Icon(Icons.Filled.Delete, contentDescription = "",tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.padding(2.dp))
                    Text(
                        text = stringResource(id = R.string.homepage_noteitem_menu_delete_chs),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

            },
            modifier = Modifier.wrapContentSize())
    }
}

