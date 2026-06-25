package com.example.ui.screens

import android.widget.ImageView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.viewmodel.AppFilter
import com.example.viewmodel.AppInfo
import com.example.viewmodel.AppLockerViewModel
import com.example.viewmodel.SortOption
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

sealed class LazyListItem {
    data class Header(val title: String) : LazyListItem()
    data class App(val app: AppInfo) : LazyListItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppLockerViewModel,
    onNavigateToSettings: () -> Unit
) {
    val apps by viewModel.appList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val currentFilter by viewModel.appFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

    // Live Clock matching current UTC time on mockup header (Requirement/Design style)
    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(key1 = true) {
        while (true) {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            currentTimeString = ZonedDateTime.now(ZoneId.of("UTC")).format(formatter)
            kotlinx.coroutines.delay(60000)
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Group apps by starting letter or lock status
    val lazyListItems = remember(apps, sortOption) {
        val items = mutableListOf<LazyListItem>()
        if (sortOption == SortOption.ALPHA_ASC || sortOption == SortOption.ALPHA_DESC) {
            val groups = apps.groupBy { app ->
                val char = app.appName.firstOrNull()?.uppercaseChar()
                if (char != null && char.isLetter()) char.toString() else "#"
            }
            val sortedKeys = if (sortOption == SortOption.ALPHA_DESC) {
                groups.keys.sortedDescending()
            } else {
                groups.keys.sorted()
            }
            sortedKeys.forEach { key ->
                items.add(LazyListItem.Header(key))
                groups[key]?.forEach { app ->
                    items.add(LazyListItem.App(app))
                }
            }
        } else {
            // SortOption.LOCKED_FIRST
            val locked = apps.filter { it.isLocked }
            val unlocked = apps.filter { !it.isLocked }
            if (locked.isNotEmpty()) {
                items.add(LazyListItem.Header("🔒 LOCKED"))
                locked.forEach { app ->
                    items.add(LazyListItem.App(app))
                }
            }
            if (unlocked.isNotEmpty()) {
                items.add(LazyListItem.Header("🔓 UNLOCKED"))
                unlocked.forEach { app ->
                    items.add(LazyListItem.App(app))
                }
            }
        }
        items
    }

    val activeLetters = remember(apps) {
        apps.mapNotNull { app ->
            val char = app.appName.firstOrNull()?.uppercaseChar()
            if (char != null && char.isLetter()) char else null
        }.toSet()
    }

    val alphabet = remember { ('A'..'Z').toList() + '#' }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Status Bar Clock Simulator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentTimeString.ifEmpty { "09:41" },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📶", fontSize = 12.sp)
                Text("🔋", fontSize = 12.sp)
            }
        }

        // Samsung Style Header matching High Density mockup
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "App Locker",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "${apps.count { it.isLocked }} apps protected",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Settings button styled as secondary white pill
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .testTag("settings_button")
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Modern search bar and sorting options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search confidential apps...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("app_search_input")
            )

            // Sort Dropdown Button
            Box {
                Button(
                    onClick = { showSortMenu = true },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    val sortLabel = when (sortOption) {
                        SortOption.ALPHA_ASC -> "A-Z"
                        SortOption.ALPHA_DESC -> "Z-A"
                        SortOption.LOCKED_FIRST -> "Locked"
                    }
                    Text(
                        text = "Sort: $sortLabel",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Alphabetical (A to Z)") },
                        onClick = {
                            viewModel.setSortOption(SortOption.ALPHA_ASC)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Alphabetical (Z to A)") },
                        onClick = {
                            viewModel.setSortOption(SortOption.ALPHA_DESC)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Locked first") },
                        onClick = {
                            viewModel.setSortOption(SortOption.LOCKED_FIRST)
                            showSortMenu = false
                        }
                    )
                }
            }
        }

        // Quick Filter Pills matching mockup HTML specification
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterPill(
                text = "All apps",
                isSelected = currentFilter == AppFilter.ALL,
                onClick = { viewModel.setAppFilter(AppFilter.ALL) }
            )
            FilterPill(
                text = "Locked",
                isSelected = currentFilter == AppFilter.LOCKED,
                onClick = { viewModel.setAppFilter(AppFilter.LOCKED) }
            )
            FilterPill(
                text = "System",
                isSelected = currentFilter == AppFilter.SYSTEM,
                onClick = { viewModel.setAppFilter(AppFilter.SYSTEM) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Large High-Density rounded sheet card container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                // Suggested / Filter Sublabel indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (currentFilter) {
                                AppFilter.ALL -> "SUGGESTED FOR YOU"
                                AppFilter.LOCKED -> "PROTECTED APPLICATIONS"
                                AppFilter.SYSTEM -> "SYSTEM UTILITIES"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 3.dp)
                    }
                } else if (apps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🔒",
                                fontSize = 32.sp
                            )
                            Text(
                                text = "No apps found in this category",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 26.dp)
                                .testTag("app_list"),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            itemsIndexed(lazyListItems, key = { index, item ->
                                when (item) {
                                    is LazyListItem.Header -> "header_${item.title}_$index"
                                    is LazyListItem.App -> "app_${item.app.packageName}_$index"
                                }
                            }) { index, item ->
                                when (item) {
                                    is LazyListItem.Header -> {
                                        SectionHeader(title = item.title)
                                    }
                                    is LazyListItem.App -> {
                                        AppRow(
                                            app = item.app,
                                            onToggle = { viewModel.toggleAppLock(item.app) }
                                        )
                                        
                                        // Show divider only if the next item is also an App
                                        val showDivider = index < lazyListItems.lastIndex && 
                                                lazyListItems[index + 1] is LazyListItem.App
                                        if (showDivider) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.04f),
                                                modifier = Modifier.padding(horizontal = 20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AlphabetSidebar(
                            alphabet = alphabet,
                            activeLetters = activeLetters,
                            onLetterSelected = { letter ->
                                val targetIndex = lazyListItems.indexOfFirst { item ->
                                    when (item) {
                                        is LazyListItem.Header -> {
                                            if (letter == '#') {
                                                item.title == "#"
                                            } else {
                                                item.title.equals(letter.toString(), ignoreCase = true)
                                            }
                                        }
                                        is LazyListItem.App -> {
                                            val firstChar = item.app.appName.firstOrNull()?.uppercaseChar()
                                            if (letter == '#') {
                                                firstChar != null && !firstChar.isLetter()
                                            } else {
                                                firstChar == letter
                                            }
                                        }
                                    }
                                }
                                if (targetIndex >= 0) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(targetIndex)
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp)
                                .testTag("alphabet_sidebar")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AppRow(
    app: AppInfo,
    onToggle: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("app_row_${app.packageName}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // High Density icon outline backing
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f))
            ) {
                AndroidView(
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            try {
                                val pm = ctx.packageManager
                                val icon = pm.getApplicationIcon(app.packageName)
                                setImageDrawable(icon)
                            } catch (e: Exception) {
                                setImageResource(android.R.drawable.sym_def_app_icon)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize().padding(6.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = app.appName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }
        }

        // Modern flat tactile toggle switch matching High Density structure
        Switch(
            checked = app.isLocked,
            onCheckedChange = { onToggle() },
            modifier = Modifier.testTag("app_lock_switch_${app.packageName}"),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun AlphabetSidebar(
    alphabet: List<Char>,
    activeLetters: Set<Char>,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(22.dp)
            .background(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.03f),
                shape = RoundedCornerShape(11.dp)
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        alphabet.forEach { letter ->
            val isActive = activeLetters.contains(letter) || (letter == '#' && activeLetters.any { !it.isLetter() })
            Text(
                text = letter.toString(),
                fontSize = 10.sp,
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isActive) { onLetterSelected(letter) }
                    .padding(vertical = 1.dp)
                    .testTag("alphabet_letter_$letter"),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
