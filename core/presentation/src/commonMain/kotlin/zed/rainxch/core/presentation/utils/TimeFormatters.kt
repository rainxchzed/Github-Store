package zed.rainxch.core.presentation.utils

import androidx.compose.runtime.Composable
import zed.rainxch.githubstore.core.presentation.res.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Composable
fun formatUpdatedAt(isoInstant: String): String {
    val updated = Instant.parse(isoInstant)
    val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
    val diff: Duration = now - updated

    val hoursDiff = diff.inWholeHours
    val daysDiff = diff.inWholeDays

    return when {
        hoursDiff < 1 -> stringResource(Res.string.updated_just_now)
        hoursDiff < 24 -> stringResource(Res.string.updated_hours_ago, hoursDiff)
        daysDiff == 1L -> stringResource(Res.string.updated_yesterday)
        daysDiff < 7 -> stringResource(Res.string.updated_days_ago, daysDiff)
        else -> {
            val date = updated.toLocalDateTime(TimeZone.currentSystemDefault()).date
            stringResource(Res.string.updated_on_date, date.toString())
        }
    }
}
@OptIn(ExperimentalTime::class)
@Composable
fun formatReleasedAt(isoInstant: String): String {
    val updated = Instant.parse(isoInstant)
    val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
    val diff: Duration = now - updated

    val hoursDiff = diff.inWholeHours
    val daysDiff = diff.inWholeDays

    return when {
        hoursDiff < 1 -> stringResource(Res.string.released_just_now)
        hoursDiff < 24 -> stringResource(Res.string.released_hours_ago, hoursDiff)
        daysDiff == 1L -> stringResource(Res.string.released_yesterday)
        daysDiff < 7 -> stringResource(Res.string.released_days_ago, daysDiff)
        else -> {
            val date = updated.toLocalDateTime(TimeZone.currentSystemDefault()).date
            stringResource(Res.string.released_on_date, date.toString())
        }
    }
}

@OptIn(ExperimentalTime::class)
suspend fun formatUpdatedAt(epochMillis: Long): String {
    val updated = Instant.fromEpochMilliseconds(epochMillis)
    val now = Clock.System.now()
    val diff: Duration = now - updated

    val hoursDiff = diff.inWholeHours
    val daysDiff = diff.inWholeDays

    return when {
        hoursDiff < 1 ->
            getString(Res.string.updated_just_now)

        hoursDiff < 24 ->
            getString(Res.string.updated_hours_ago, hoursDiff)

        daysDiff == 1L ->
            getString(Res.string.updated_yesterday)

        daysDiff < 7 ->
            getString(Res.string.updated_days_ago, daysDiff)

        else -> {
            val date = updated
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
            getString(Res.string.updated_on_date, date.toString())
        }
    }
}
@OptIn(ExperimentalTime::class)
suspend fun formatAddedAt(epochMillis: Long): String {
    val updated = Instant.fromEpochMilliseconds(epochMillis)
    val now = Clock.System.now()
    val diff: Duration = now - updated

    val hoursDiff = diff.inWholeHours
    val daysDiff = diff.inWholeDays

    return when {
        hoursDiff < 1 ->
            getString(Res.string.added_just_now)

        hoursDiff < 24 ->
            getString(Res.string.added_hours_ago, hoursDiff)

        daysDiff == 1L ->
            getString(Res.string.added_yesterday)

        daysDiff < 7 ->
            getString(Res.string.added_days_ago, daysDiff)

        else -> {
            val date = updated
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
            getString(Res.string.added_on_date, date.toString())
        }
    }
}
