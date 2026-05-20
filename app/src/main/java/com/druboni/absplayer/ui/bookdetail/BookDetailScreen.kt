package com.druboni.absplayer.ui.bookdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    itemId: String,
    onPlayClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(itemId) { viewModel.load(itemId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.item?.media?.metadata?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { viewModel.downloadBook(itemId) }) {
                        Icon(Icons.Default.Download, contentDescription = "Download")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.item != null) {
                ExtendedFloatingActionButton(
                    onClick = { onPlayClick(itemId) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    text = { Text(if (viewModel.progressPercent() > 0) "Resume" else "Play") }
                )
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
            uiState.item != null -> {
                val item = uiState.item!!
                val meta = item.media.metadata

                Column(
                    modifier = Modifier
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AsyncImage(
                            model = uiState.coverUrl,
                            contentDescription = meta.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(MaterialTheme.shapes.medium)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meta.title, style = MaterialTheme.typography.titleLarge)
                            if (meta.authorName != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(meta.authorName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (meta.seriesName != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(meta.seriesName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(viewModel.formatDuration(item.media.duration), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    val progressPct = viewModel.progressPercent()
                    if (progressPct > 0) {
                        Spacer(Modifier.height(16.dp))
                        Text("Progress: ${progressPct.toInt()}%", style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(progress = progressPct / 100f, modifier = Modifier.fillMaxWidth())
                    }

                    if (meta.description != null) {
                        Spacer(Modifier.height(16.dp))
                        Text("Description", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(meta.description, style = MaterialTheme.typography.bodyMedium)
                    }

                    val chapters = item.media.chapters
                    if (!chapters.isNullOrEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text("Chapters (${chapters.size})", style = MaterialTheme.typography.titleMedium)
                        chapters.take(10).forEach { chapter ->
                            Text(
                                "• ${chapter.title}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                        if (chapters.size > 10) {
                            Text("...and ${chapters.size - 10} more", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}
