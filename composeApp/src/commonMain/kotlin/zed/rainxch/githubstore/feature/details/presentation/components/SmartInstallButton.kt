package zed.rainxch.githubstore.feature.details.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import githubstore.composeapp.generated.resources.Res
import githubstore.composeapp.generated.resources.architecture_compatible
import githubstore.composeapp.generated.resources.auto_update
import githubstore.composeapp.generated.resources.cancel_download
import githubstore.composeapp.generated.resources.downloading
import githubstore.composeapp.generated.resources.install_latest
import githubstore.composeapp.generated.resources.installing
import githubstore.composeapp.generated.resources.not_available
import githubstore.composeapp.generated.resources.percent
import githubstore.composeapp.generated.resources.reinstall
import githubstore.composeapp.generated.resources.shizuku_enabled
import githubstore.composeapp.generated.resources.show_install_options
import githubstore.composeapp.generated.resources.silent_install
import githubstore.composeapp.generated.resources.silent_reinstall
import githubstore.composeapp.generated.resources.silent_update
import githubstore.composeapp.generated.resources.update_app
import githubstore.composeapp.generated.resources.update_to_version
import githubstore.composeapp.generated.resources.updating
import githubstore.composeapp.generated.resources.verifying
import io.github.fletchmckee.liquid.liquefiable
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import zed.rainxch.githubstore.core.domain.model.GithubAsset
import zed.rainxch.githubstore.core.domain.model.GithubUser
import zed.rainxch.githubstore.feature.details.presentation.DetailsAction
import zed.rainxch.githubstore.feature.details.presentation.DetailsState
import zed.rainxch.githubstore.feature.details.presentation.DownloadStage
import zed.rainxch.githubstore.feature.details.presentation.utils.LocalTopbarLiquidState
import zed.rainxch.githubstore.feature.details.presentation.utils.extractArchitectureFromName
import zed.rainxch.githubstore.feature.details.presentation.utils.isExactArchitectureMatch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SmartInstallButton(
    isDownloading: Boolean,
    isInstalling: Boolean,
    progress: Int?,
    installProgress: Int?,
    primaryAsset: GithubAsset?,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier,
    state: DetailsState
) {
    val liquidState = LocalTopbarLiquidState.current

    val installedApp = state.installedApp
    val isInstalled = installedApp != null
    val isUpdateAvailable = installedApp?.isUpdateAvailable == true
    val isShizukuAvailable = state.isShizukuAvailable

    val enabled = remember(primaryAsset, isDownloading, isInstalling) {
        primaryAsset != null && !isDownloading && !isInstalling
    }

    val isActiveDownload = state.isDownloading || state.downloadStage != DownloadStage.IDLE

    val buttonColor = when {
        !enabled && !isActiveDownload -> MaterialTheme.colorScheme.surfaceContainer
        isUpdateAvailable -> MaterialTheme.colorScheme.tertiary
        isInstalled -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    val buttonText = when {
        !enabled && primaryAsset == null -> stringResource(Res.string.not_available)
        installedApp != null && installedApp.installedVersion != state.latestRelease?.tagName -> {
            if (isShizukuAvailable) {
                stringResource(Res.string.silent_update)
            } else {
                stringResource(Res.string.update_app)
            }
        }
        isUpdateAvailable -> stringResource(
            Res.string.update_to_version,
            installedApp.latestVersion
        )
        isInstalled -> {
            if (isShizukuAvailable) {
                stringResource(Res.string.silent_reinstall)
            } else {
                stringResource(Res.string.reinstall)
            }
        }
        else -> {
            if (isShizukuAvailable) {
                stringResource(Res.string.silent_install)
            } else {
                stringResource(Res.string.install_latest)
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Shizuku indicator
        if (isShizukuAvailable && !isActiveDownload) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(Res.string.shizuku_enabled),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ElevatedCard(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .background(
                        color = buttonColor,
                        shape = CircleShape
                    )
                    .clickable(
                        enabled = enabled,
                        onClick = {
                            if (!state.isDownloading && state.downloadStage == DownloadStage.IDLE) {
                                if (isUpdateAvailable) {
                                    onAction(DetailsAction.UpdateApp)
                                } else {
                                    onAction(DetailsAction.InstallPrimary)
                                }
                            }
                        }
                    )
                    .liquefiable(liquidState),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = buttonColor
                ),
                shape = if (state.isObtainiumEnabled || isActiveDownload) {
                    RoundedCornerShape(
                        topStart = 24.dp,
                        bottomStart = 24.dp,
                        topEnd = 6.dp,
                        bottomEnd = 6.dp
                    )
                } else CircleShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActiveDownload) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            when (state.downloadStage) {
                                DownloadStage.DOWNLOADING -> {
                                    Text(
                                        text = if (isUpdateAvailable) stringResource(Res.string.updating)
                                        else stringResource(Res.string.downloading),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${progress ?: 0}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                    )
                                }

                                DownloadStage.VERIFYING -> {
                                    Text(
                                        text = stringResource(Res.string.verifying),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                DownloadStage.INSTALLING -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (isUpdateAvailable) stringResource(Res.string.updating)
                                            else stringResource(Res.string.installing),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )

                                        // Show install progress if using Shizuku
                                        if (installProgress != null && isShizukuAvailable) {
                                            Text(
                                                text = "${installProgress}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }

                                DownloadStage.IDLE -> {}
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isShizukuAvailable) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = when {
                                            isUpdateAvailable -> MaterialTheme.colorScheme.onTertiary
                                            isInstalled -> MaterialTheme.colorScheme.onSecondary
                                            else -> MaterialTheme.colorScheme.onPrimary
                                        }
                                    )
                                } else if (isUpdateAvailable) {
                                    Icon(
                                        imageVector = Icons.Default.Update,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onTertiary
                                    )
                                } else if (isInstalled) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSecondary
                                    )
                                }

                                Text(
                                    text = buttonText,
                                    color = if (enabled) {
                                        when {
                                            isUpdateAvailable -> MaterialTheme.colorScheme.onTertiary
                                            isInstalled -> MaterialTheme.colorScheme.onSecondary
                                            else -> MaterialTheme.colorScheme.onPrimary
                                        }
                                    } else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            if (primaryAsset != null) {
                                val assetArch = extractArchitectureFromName(primaryAsset.name)
                                val systemArch = state.systemArchitecture

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = assetArch ?: systemArch.name.lowercase(),
                                        color = if (enabled) {
                                            when {
                                                isUpdateAvailable -> MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.8f)
                                                isInstalled -> MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f)
                                                else -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                            }
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    if (assetArch != null && isExactArchitectureMatch(
                                            assetName = primaryAsset.name.lowercase(),
                                            systemArch = systemArch
                                        )
                                    ) {
                                        Spacer(modifier = Modifier.width(4.dp))

                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = stringResource(Res.string.architecture_compatible),
                                            tint = if (enabled) {
                                                when {
                                                    isUpdateAvailable -> MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.8f)
                                                    isInstalled -> MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f)
                                                    else -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                                }
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            },
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isActiveDownload) {
                IconButton(
                    onClick = {
                        onAction(DetailsAction.CancelCurrentDownload)
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(
                        topStart = 6.dp,
                        bottomStart = 6.dp,
                        topEnd = 24.dp,
                        bottomEnd = 24.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.cancel_download),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else if (state.isObtainiumEnabled) {
                IconButton(
                    onClick = {
                        onAction(DetailsAction.OnToggleInstallDropdown)
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (enabled) {
                            buttonColor
                        } else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(
                        topStart = 6.dp,
                        bottomStart = 6.dp,
                        topEnd = 24.dp,
                        bottomEnd = 24.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(Res.string.show_install_options),
                        modifier = Modifier.size(24.dp),
                        tint = if (enabled) {
                            when {
                                isUpdateAvailable -> MaterialTheme.colorScheme.onTertiary
                                isInstalled -> MaterialTheme.colorScheme.onSecondary
                                else -> MaterialTheme.colorScheme.onPrimary
                            }
                        } else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }

        // Auto-update toggle (only show for installed apps with Shizuku)
        if (isInstalled && isShizukuAvailable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onAction(DetailsAction.ToggleAutoUpdate)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Update,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(Res.string.auto_update),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Switch(
                    checked = installedApp.autoUpdateEnabled,
                    onCheckedChange = {
                        onAction(DetailsAction.ToggleAutoUpdate)
                    }
                )
            }
        }
    }
}