package eu.kanade.presentation.entries.anime.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileDownloadOff
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.presentation.entries.DotSeparatorText
import eu.kanade.tachiyomi.data.download.anime.model.AnimeDownload
import me.saket.swipe.SwipeableActionsBox
import me.saket.swipe.rememberSwipeableActionsState
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.ReadItemAlpha
import tachiyomi.presentation.core.components.material.SecondaryItemAlpha
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.selectedBackground
import kotlin.math.absoluteValue

@Composable
fun AnimeEpisodeListItem(
    title: String,
    date: String?,
    watchProgress: String?,
    scanlator: String?,
    seen: Boolean,
    bookmark: Boolean,
    selected: Boolean,
    downloadIndicatorEnabled: Boolean,
    downloadStateProvider: () -> AnimeDownload.State,
    downloadProgressProvider: () -> Int,
    episodeSwipeStartAction: LibraryPreferences.EpisodeSwipeAction,
    episodeSwipeEndAction: LibraryPreferences.EpisodeSwipeAction,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onDownloadClick: ((EpisodeDownloadAction) -> Unit)?,
    onEpisodeSwipe: (LibraryPreferences.EpisodeSwipeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val textAlpha = if (seen) ReadItemAlpha else 1f
    val textSubtitleAlpha = if (seen) ReadItemAlpha else SecondaryItemAlpha

    // Increase touch slop of swipe action to reduce accidental trigger
    val configuration = LocalViewConfiguration.current
    CompositionLocalProvider(
        LocalViewConfiguration provides object : ViewConfiguration by configuration {
            override val touchSlop: Float = configuration.touchSlop * 3f
        },
    ) {
        val start = getSwipeAction(
            action = episodeSwipeStartAction,
            seen = seen,
            bookmark = bookmark,
            downloadState = downloadStateProvider(),
            background = MaterialTheme.colorScheme.primaryContainer,
            onSwipe = { onEpisodeSwipe(episodeSwipeStartAction) },
        )
        val end = getSwipeAction(
            action = episodeSwipeEndAction,
            seen = seen,
            bookmark = bookmark,
            downloadState = downloadStateProvider(),
            background = MaterialTheme.colorScheme.primaryContainer,
            onSwipe = { onEpisodeSwipe(episodeSwipeEndAction) },
        )

        val swipeableActionsState = rememberSwipeableActionsState()
        LaunchedEffect(Unit) {
            // Haptic effect when swipe over threshold
            val swipeActionThresholdPx = with(density) { swipeActionThreshold.toPx() }
            snapshotFlow { swipeableActionsState.offset.value.absoluteValue > swipeActionThresholdPx }
                .collect { if (it) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
        }

        SwipeableActionsBox(
            modifier = Modifier.clipToBounds(),
            state = swipeableActionsState,
            startActions = listOfNotNull(start),
            endActions = listOfNotNull(end),
            swipeThreshold = swipeActionThreshold,
            backgroundUntilSwipeThreshold = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Row(
                modifier = modifier
                    .selectedBackground(selected)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                    .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        var textHeight by remember { mutableIntStateOf(0) }
                        if (!seen) {
                            Icon(
                                imageVector = Icons.Filled.Circle,
                                contentDescription = stringResource(MR.strings.unread),
                                modifier = Modifier
                                    .height(8.dp)
                                    .padding(end = 4.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (bookmark) {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = stringResource(
                                    MR.strings.action_filter_bookmarked,
                                ),
                                modifier = Modifier
                                    .sizeIn(
                                        maxHeight = with(LocalDensity.current) { textHeight.toDp() - 2.dp },
                                    ),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = textAlpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { textHeight = it.size.height },
                        )
                    }

                    Row {
                        ProvideTextStyle(
                            value = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.sp,
                                color = LocalContentColor.current.copy(alpha = textSubtitleAlpha),
                            ),
                        ) {
                            if (date != null) {
                                Text(
                                    text = date,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (watchProgress != null || scanlator != null) DotSeparatorText()
                            }
                            if (watchProgress != null) {
                                Text(
                                    text = watchProgress,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.alpha(ReadItemAlpha),
                                )
                                if (scanlator != null) DotSeparatorText()
                            }
                            if (scanlator != null) {
                                Text(
                                    text = scanlator,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                if (onDownloadClick != null) {
                    EpisodeDownloadIndicator(
                        enabled = downloadIndicatorEnabled,
                        modifier = Modifier.padding(start = 4.dp),
                        downloadStateProvider = downloadStateProvider,
                        downloadProgressProvider = downloadProgressProvider,
                        onClick = onDownloadClick,
                    )
                }
            }
        }
    }
}

private fun getSwipeAction(
    action: LibraryPreferences.EpisodeSwipeAction,
    seen: Boolean,
    bookmark: Boolean,
    downloadState: AnimeDownload.State,
    background: Color,
    onSwipe: () -> Unit,
): me.saket.swipe.SwipeAction? {
    return when (action) {
        LibraryPreferences.EpisodeSwipeAction.ToggleSeen -> swipeAction(
            icon = if (!seen) Icons.Outlined.Done else Icons.Outlined.RemoveDone,
            background = background,
            isUndo = seen,
            onSwipe = onSwipe,
        )
        LibraryPreferences.EpisodeSwipeAction.ToggleBookmark -> swipeAction(
            icon = if (!bookmark) Icons.Outlined.BookmarkAdd else Icons.Outlined.BookmarkRemove,
            background = background,
            isUndo = bookmark,
            onSwipe = onSwipe,
        )
        LibraryPreferences.EpisodeSwipeAction.Download -> swipeAction(
            icon = when (downloadState) {
                AnimeDownload.State.NOT_DOWNLOADED, AnimeDownload.State.ERROR -> Icons.Outlined.Download
                AnimeDownload.State.QUEUE, AnimeDownload.State.DOWNLOADING -> Icons.Outlined.FileDownloadOff
                AnimeDownload.State.DOWNLOADED -> Icons.Outlined.Delete
            },
            background = background,
            onSwipe = onSwipe,
        )
        LibraryPreferences.EpisodeSwipeAction.Disabled -> null
    }
}

@Composable
fun NextEpisodeAiringListItem(
    title: String,
    date: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                var textHeight by remember { mutableIntStateOf(0) }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textHeight = it.size.height },
                    modifier = Modifier.alpha(SecondaryItemAlpha),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.alpha(SecondaryItemAlpha)) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                ) {
                    Text(
                        text = date,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun swipeAction(
    onSwipe: () -> Unit,
    icon: ImageVector,
    background: Color,
    isUndo: Boolean = false,
): me.saket.swipe.SwipeAction {
    return me.saket.swipe.SwipeAction(
        icon = {
            Icon(
                modifier = Modifier.padding(16.dp),
                imageVector = icon,
                tint = contentColorFor(background),
                contentDescription = null,
            )
        },
        background = background,
        onSwipe = onSwipe,
        isUndo = isUndo,
    )
}

private val swipeActionThreshold = 56.dp
