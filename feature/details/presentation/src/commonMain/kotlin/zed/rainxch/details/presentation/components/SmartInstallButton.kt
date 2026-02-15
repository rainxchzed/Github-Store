package zed.rainxch.details.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.fletchmckee.liquid.liquefiable
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import zed.rainxch.core.domain.model.GithubAsset
import zed.rainxch.core.domain.model.GithubUser
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.details.presentation.DetailsAction
import zed.rainxch.details.presentation.DetailsState
import zed.rainxch.details.presentation.model.DownloadStage
import zed.rainxch.details.presentation.utils.LocalTopbarLiquidState
import zed.rainxch.details.presentation.utils.extractArchitectureFromName
import zed.rainxch.details.presentation.utils.isExactArchitectureMatch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SmartInstallButton(
    isDownloading: Boolean,
    isInstalling: Boolean,
    progress: Int?,
    primaryAsset: GithubAsset?,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier,
    state: DetailsState
) {
    val liquidState = LocalTopbarLiquidState.current

    val installedApp = state.installedApp
    val isInstalled = installedApp != null
    val isUpdateAvailable = installedApp?.isUpdateAvailable == true

    val enabled = remember(primaryAsset, isDownloading, isInstalling) {
        primaryAsset != null && !isDownloading && !isInstalling
    }

    val isActiveDownload = state.isDownloading || state.downloadStage != DownloadStage.IDLE

    // Determine button color and text based on install status
    val buttonColor = when {
        !enabled && !isActiveDownload -> MaterialTheme.colorScheme.surfaceContainer
        isUpdateAvailable -> MaterialTheme.colorScheme.tertiary
        isInstalled -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    val buttonText = when {
        !enabled && primaryAsset == null -> stringResource(Res.string.not_available)
        installedApp != null && installedApp.installedVersion != state.latestRelease?.tagName -> stringResource(
            Res.string.update_app
        )

        isUpdateAvailable -> stringResource(
            Res.string.update_to_version,
            installedApp.latestVersion.toString()
        )

        isInstalled -> stringResource(Res.string.reinstall)
        else -> stringResource(Res.string.install_latest)
    }

    Row(
        modifier = modifier,
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
                                    text = if (isUpdateAvailable) stringResource(Res.string.updating) else stringResource(
                                        Res.string.downloading
                                    ),
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
                                Text(
                                    text = if (isUpdateAvailable) stringResource(Res.string.updating) else stringResource(
                                        Res.string.installing
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
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
                            if (isUpdateAvailable) {
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
                                            isUpdateAvailable -> MaterialTheme.colorScheme.onTertiary.copy(
                                                alpha = 0.8f
                                            )

                                            isInstalled -> MaterialTheme.colorScheme.onSecondary.copy(
                                                alpha = 0.8f
                                            )

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
                                                isUpdateAvailable -> MaterialTheme.colorScheme.onTertiary.copy(
                                                    alpha = 0.8f
                                                )

                                                isInstalled -> MaterialTheme.colorScheme.onSecondary.copy(
                                                    alpha = 0.8f
                                                )

                                                else -> MaterialTheme.colorScheme.onPrimary.copy(
                                                    alpha = 0.8f
                                                )
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
}

@Preview
@Composable
fun SmartInstallButtonDownloadingPreview() {
    SmartInstallButton(
        isDownloading = true,
        isInstalling = false,
        progress = 45,
        primaryAsset = GithubAsset(
            id = 1L,
            name = "app-arm64-v8a.apk",
            contentType = "application/vnd.android.package-archive",
            size = 50_000_000L,
            downloadUrl = "https://example.com/app.apk",
            uploader = GithubUser(
                id = 1L,
                login = "developer",
                avatarUrl = "",
                htmlUrl = ""
            )
        ),
        onAction = {},
        state = DetailsState(
            isDownloading = true,
            downloadStage = DownloadStage.DOWNLOADING,
            downloadProgressPercent = 45
        )
    )
}