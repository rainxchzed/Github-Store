package zed.rainxch.details.presentation.components.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import zed.rainxch.githubstore.core.presentation.res.*
import io.github.fletchmckee.liquid.liquefiable
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.details.presentation.DetailsAction
import zed.rainxch.details.presentation.DetailsState
import zed.rainxch.details.presentation.components.AppHeader
import zed.rainxch.details.presentation.components.SmartInstallButton
import zed.rainxch.details.presentation.utils.LocalTopbarLiquidState

fun LazyListScope.header(
    state: DetailsState,
    onAction: (DetailsAction) -> Unit,
) {
    item {
        val liquidState = LocalTopbarLiquidState.current

        if (state.repository != null) {
            AppHeader(
                author = state.userProfile,
                release = state.latestRelease,
                repository = state.repository,
                installedApp = state.installedApp,
                downloadStage = state.downloadStage,
                downloadProgress = state.downloadProgressPercent,
                modifier = Modifier.liquefiable(liquidState)
            )
        }
    }

    item {
        val liquidState = LocalTopbarLiquidState.current

        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            SmartInstallButton(
                isDownloading = state.isDownloading,
                isInstalling = state.isInstalling,
                progress = state.downloadProgressPercent,
                primaryAsset = state.primaryAsset,
                state = state,
                onAction = onAction,
            )

            DropdownMenu(
                expanded = state.isInstallDropdownExpanded,
                onDismissRequest = {
                    onAction(DetailsAction.OnToggleInstallDropdown)
                },
                offset = DpOffset(x = 0.dp, y = 20.dp),
            ) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = stringResource(Res.string.open_in_obtainium),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(Res.string.obtainium_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onAction(DetailsAction.OpenInObtainium)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Update,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    modifier = Modifier.liquefiable(liquidState)
                )

                Spacer(Modifier.height(8.dp))

                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = stringResource(Res.string.inspect_with_appmanager),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(Res.string.appmanager_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onAction(DetailsAction.OpenInAppManager)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    modifier = Modifier.liquefiable(liquidState)
                )
            }
        }
    }
}