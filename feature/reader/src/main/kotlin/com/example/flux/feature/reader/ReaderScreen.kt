package com.example.flux.feature.reader

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.flux.domain.model.NightMode
import com.example.flux.feature.reader.model.ReaderIntent
import com.example.flux.feature.reader.model.ReaderUiState

@Composable
fun ReaderScreen(
    bookId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { onNavigateBack() }
    }

    ReaderContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onIntent = viewModel::onIntent,
        getAnnotatedPage = viewModel::getAnnotatedPage,
        modifier = modifier,
    )
}

@Composable
internal fun ReaderContent(
    uiState: ReaderUiState,
    onNavigateBack: () -> Unit,
    onIntent: (ReaderIntent) -> Unit,
    getAnnotatedPage: (Int) -> AnnotatedString? = { null },
    modifier: Modifier = Modifier,
) {
    when (val state = uiState) {
        ReaderUiState.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ReaderUiState.Error -> {
            ReaderErrorContent(
                message = state.message,
                canDelete = state.canDelete,
                onNavigateBack = onNavigateBack,
                onDeleteBook = { onIntent(ReaderIntent.DeleteBook) },
                modifier = modifier,
            )
        }
        is ReaderUiState.Success -> {
            ReaderSuccessContent(
                state = state,
                onIntent = onIntent,
                getAnnotatedPage = getAnnotatedPage,
                modifier = modifier,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderErrorContent(
    message: String,
    canDelete: Boolean,
    onNavigateBack: () -> Unit,
    onDeleteBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Broken Book") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Can't open this book",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (canDelete) {
                Spacer(Modifier.height(32.dp))
                OutlinedButton(onClick = onDeleteBook) {
                    Text("Remove from library")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ReaderSuccessContent(
    state: ReaderUiState.Success,
    onIntent: (ReaderIntent) -> Unit,
    getAnnotatedPage: (Int) -> AnnotatedString?,
    modifier: Modifier = Modifier,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = state.currentPageIndex,
        pageCount = { state.totalPages },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onIntent(ReaderIntent.PageChanged(page))
        }
    }

    LaunchedEffect(state.currentPageIndex) {
        if (pagerState.currentPage != state.currentPageIndex && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(state.currentPageIndex)
        }
    }

    ReadingThemeWrapper(nightMode = state.nightMode, modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { pageIndex ->
                state.pages.getOrNull(pageIndex)?.let { page ->
                    ReaderPageContent(
                        page = page,
                        fontSizeSp = state.fontSizeSp,
                        annotatedText = getAnnotatedPage(pageIndex),
                        onTap = { onIntent(ReaderIntent.ToggleControls) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            AnimatedVisibility(
                visible = state.controlsVisible,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            ) {
                Surface(shadowElevation = 4.dp) {
                    TopAppBar(
                        title = {
                            Text(
                                text = state.book.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { onIntent(ReaderIntent.NavigateBack) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                }
            }

            AnimatedVisibility(
                visible = state.controlsVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            ) {
                Surface(shadowElevation = 4.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            LinearProgressIndicator(
                                progress = { (state.currentPageIndex + 1f) / state.totalPages },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.Transparent,
                            )
                            Text(
                                text = "${state.currentPageIndex + 1} / ${state.totalPages}",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Reading settings",
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        ReaderSettingsSheet(
            bionicEnabled = state.bionicEnabled,
            bionicIntensity = state.bionicIntensity,
            fontSizeSp = state.fontSizeSp,
            nightMode = state.nightMode,
            onIntent = onIntent,
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun ReadingThemeWrapper(
    nightMode: NightMode,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val (targetBg, targetContent) = when (nightMode) {
        NightMode.LIGHT -> Color(0xFFFFFFFF) to Color(0xFF1A1A1A)
        NightMode.DARK -> Color(0xFF1A1A2E) to Color(0xFFE8E8E8)
        NightMode.SEPIA -> Color(0xFFF4EAD5) to Color(0xFF3D2B1F)
        NightMode.SYSTEM -> if (systemDark) Color(0xFF1A1A2E) to Color(0xFFE8E8E8)
                            else Color(0xFFFFFFFF) to Color(0xFF1A1A1A)
    }

    val bgColor by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(durationMillis = 300),
        label = "readerBg",
    )
    val contentColor by animateColorAsState(
        targetValue = targetContent,
        animationSpec = tween(durationMillis = 300),
        label = "readerContent",
    )

    val view = LocalView.current
    val isLight = targetBg.luminance() > 0.5f

    SideEffect {
        if (!view.isInEditMode) {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = bgColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
        }
    }

    DisposableEffect(view) {
        if (!view.isInEditMode) {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        onDispose {
            if (!view.isInEditMode) {
                val window = (view.context as Activity).window
                WindowCompat.setDecorFitsSystemWindows(window, true)
                @Suppress("DEPRECATION")
                window.statusBarColor = android.graphics.Color.TRANSPARENT
            }
        }
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Surface(
            color = bgColor,
            contentColor = contentColor,
            modifier = modifier,
            content = content,
        )
    }
}
