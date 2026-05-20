package com.druboni.absplayer.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    itemId: String,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showChapters by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) { viewModel.initPlayer(itemId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (uiState.chapters.isNotEmpty()) {
                        IconButton(onClick = { showChapters = !showChapters }) {
                            Icon(Icons.Default.List, contentDescription = "Chapters")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onBack) { Text("Go Back") }
                }
            }
            else -> if (showChapters) {
                ChapterList(
                    chapters = uiState.chapters,
                    currentIndex = uiState.currentChapterIndex,
                    onChapterClick = { viewModel.seekToChapter(it) },
                    modifier = Modifier.padding(padding)
                )
            } else {
                PlayerControls(
                    uiState = uiState,
                    onPlayPause = viewModel::togglePlayPause,
                    onSeekTo = viewModel::seekTo,
                    onForward = { viewModel.seekForward() },
                    onBack = { viewModel.seekBack() },
                    onSpeedClick = { showSpeedMenu = true },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }

    if (showSpeedMenu) {
        SpeedPickerDialog(
            current = uiState.playbackSpeed,
            onSelect = { speed ->
                viewModel.setSpeed(speed)
                showSpeedMenu = false
            },
            onDismiss = { showSpeedMenu = false }
        )
    }
}

@Composable
private fun PlayerControls(
    uiState: PlayerUiState,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onForward: () -> Unit,
    onBack: () -> Unit,
    onSpeedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        AsyncImage(
            model = uiState.coverUrl,
            contentDescription = uiState.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(240.dp).clip(MaterialTheme.shapes.large)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(uiState.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, maxLines = 2)
            if (uiState.author != null) {
                Text(uiState.author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (uiState.currentChapterIndex >= 0 && uiState.chapters.isNotEmpty()) {
                Text(
                    uiState.chapters[uiState.currentChapterIndex].title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column {
            Slider(
                value = if (uiState.duration > 0) uiState.currentPosition.toFloat() / uiState.duration.toFloat() else 0f,
                onValueChange = { onSeekTo((it * uiState.duration).toLong()) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(uiState.currentPosition), style = MaterialTheme.typography.bodySmall)
                Text(formatTime(uiState.duration), style = MaterialTheme.typography.bodySmall)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onSpeedClick) {
                Text("${uiState.playbackSpeed}x", style = MaterialTheme.typography.labelLarge)
            }
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Replay30, contentDescription = "Back 30s", modifier = Modifier.size(32.dp))
            }
            FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(64.dp)) {
                Icon(
                    if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp)
                )
            }
            IconButton(onClick = onForward, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Forward30, contentDescription = "Forward 30s", modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(48.dp))
        }
    }
}

@Composable
private fun ChapterList(
    chapters: List<com.druboni.absplayer.data.api.model.Chapter>,
    currentIndex: Int,
    onChapterClick: (com.druboni.absplayer.data.api.model.Chapter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(chapters, key = { it.id }) { chapter ->
            val isActive = chapters.indexOf(chapter) == currentIndex
            ListItem(
                headlineContent = { Text(chapter.title, color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                supportingContent = { Text(formatTime((chapter.start * 1000).toLong())) },
                leadingContent = { if (isActive) Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.clickable { onChapterClick(chapter) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun SpeedPickerDialog(
    current: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback Speed") },
        text = {
            Column {
                speeds.forEach { speed ->
                    ListItem(
                        headlineContent = { Text("${speed}x") },
                        trailingContent = { if (speed == current) Icon(Icons.Default.Check, contentDescription = null) },
                        modifier = Modifier.clickable { onSelect(speed) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    else "$m:${s.toString().padStart(2, '0')}"
}
