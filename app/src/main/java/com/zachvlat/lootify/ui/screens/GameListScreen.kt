package com.zachvlat.lootify.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zachvlat.lootify.data.FreeGame
import com.zachvlat.lootify.ui.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListScreen(
    viewModel: GameViewModel = viewModel()
) {
    val games by viewModel.games.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedFilters by viewModel.selectedFilters.collectAsState()

    val context = LocalContext.current

    val availableSources = games.map { it.source }.distinct().sorted()
    val filteredGames = if (selectedFilters.isEmpty()) {
        games
    } else {
        games.filter { it.source in selectedFilters }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(selectedFilters) {
        listState.animateScrollToItem(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lootify") },
                actions = {
                    IconButton(onClick = { viewModel.refreshGames() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (availableSources.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilters.isEmpty(),
                        onClick = { viewModel.clearFilters() },
                        label = { Text("All") }
                    )
                    availableSources.forEach { source ->
                        FilterChip(
                            selected = source in selectedFilters,
                            onClick = { viewModel.toggleFilter(source) },
                            label = { Text(source) }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading && games.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    error != null && games.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Error: ${error}",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refreshGames() }) {
                                Text("Retry")
                            }
                        }
                    }
                    filteredGames.isEmpty() -> {
                        Text(
                            text = "No free games found",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredGames, key = { it.id }) { game ->
                                GameItem(game = game)
                            }
                        }
                    }
                }

                if (isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@Composable
fun GameItem(game: FreeGame) {
    val context = LocalContext.current

    Card(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(game.gameLink))
            context.startActivity(intent)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                SourceChip(source = game.source)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = game.gameLink,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (game.publishedDate.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = game.publishedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceChip(source: String) {
    val color = when (source) {
        "Steam" -> MaterialTheme.colorScheme.primary
        "Epic Games" -> MaterialTheme.colorScheme.secondary
        "GOG" -> MaterialTheme.colorScheme.tertiary
        "Itch.io" -> MaterialTheme.colorScheme.error
        "Microsoft" -> MaterialTheme.colorScheme.primary
        "Fanatical" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = source,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
