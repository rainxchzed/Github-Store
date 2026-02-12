package zed.rainxch.details.presentation.utils

import androidx.compose.runtime.Composable
import githubstore.feature.details.presentation.generated.resources.log_cancelled
import githubstore.feature.details.presentation.generated.resources.log_download_started
import githubstore.feature.details.presentation.generated.resources.log_downloaded
import githubstore.feature.details.presentation.generated.resources.log_error
import githubstore.feature.details.presentation.generated.resources.log_error_with_message
import githubstore.feature.details.presentation.generated.resources.log_install_started
import githubstore.feature.details.presentation.generated.resources.log_installed
import githubstore.feature.details.presentation.generated.resources.log_opened_appmanager
import githubstore.feature.details.presentation.generated.resources.log_prepare_appmanager
import githubstore.feature.details.presentation.generated.resources.log_update_started
import githubstore.feature.details.presentation.generated.resources.log_updated
import githubstore.feature.details.presentation.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.details.presentation.model.LogResult

@Composable
fun LogResult.asText(): String {
    return when (this) {
        LogResult.DownloadStarted ->
            stringResource(Res.string.log_download_started)

        LogResult.Downloaded ->
            stringResource(Res.string.log_downloaded)

        LogResult.InstallStarted ->
            stringResource(Res.string.log_install_started)

        LogResult.Installed ->
            stringResource(Res.string.log_installed)

        LogResult.Updated ->
            stringResource(Res.string.log_updated)

        LogResult.Cancelled ->
            stringResource(Res.string.log_cancelled)

        LogResult.OpenedInAppManager ->
            stringResource(Res.string.log_opened_appmanager)

        is LogResult.Error ->
            message?.let {
                stringResource(Res.string.log_error_with_message, it)
            } ?: stringResource(Res.string.log_error)

        is LogResult.Info -> message

        LogResult.PreparingForAppManager -> stringResource(Res.string.log_prepare_appmanager)
        LogResult.UpdateStarted -> stringResource(Res.string.log_update_started)
    }
}
