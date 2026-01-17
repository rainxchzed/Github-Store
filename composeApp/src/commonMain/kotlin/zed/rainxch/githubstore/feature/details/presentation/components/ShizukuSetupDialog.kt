package zed.rainxch.githubstore.feature.details.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import githubstore.composeapp.generated.resources.Res
import githubstore.composeapp.generated.resources.close
import githubstore.composeapp.generated.resources.download_shizuku
import githubstore.composeapp.generated.resources.grant_permission
import githubstore.composeapp.generated.resources.grant_permission_to_github_store
import githubstore.composeapp.generated.resources.install_shizuku_app
import githubstore.composeapp.generated.resources.open_shizuku
import githubstore.composeapp.generated.resources.setup_shizuku
import githubstore.composeapp.generated.resources.shizuku_setup_complete
import githubstore.composeapp.generated.resources.shizuku_setup_description
import githubstore.composeapp.generated.resources.start_shizuku_service
import githubstore.composeapp.generated.resources.tap_start_in_shizuku
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

@Composable
fun ShizukuSetupDialog(
    isShizukuInstalled: Boolean,
    isShizukuRunning: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    onOpenShizukuApp: () -> Unit,
    onRefreshStatus: () -> Unit
) {
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            onRefreshStatus()
        }
    }

    LaunchedEffect(isShizukuInstalled, isShizukuRunning, hasPermission) {
        if (isShizukuInstalled && isShizukuRunning && hasPermission) {
            delay(500)
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(stringResource(Res.string.setup_shizuku))
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.shizuku_setup_description),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Step 1: Install Shizuku
                SetupStep(
                    number = 1,
                    title = stringResource(Res.string.install_shizuku_app),
                    isComplete = isShizukuInstalled,
                    action = if (!isShizukuInstalled) {
                        {
                            Button(onClick = onOpenShizukuApp) {
                                Text(stringResource(Res.string.download_shizuku))
                            }
                        }
                    } else null
                )

                // Step 2: Start Shizuku (only show if installed)
                if (isShizukuInstalled) {
                    SetupStep(
                        number = 2,
                        title = stringResource(Res.string.start_shizuku_service),
                        isComplete = isShizukuRunning,
                        action = if (!isShizukuRunning) {
                            {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(onClick = onOpenShizukuApp) {
                                        Text(stringResource(Res.string.open_shizuku))
                                    }
                                    Text(
                                        text = stringResource(Res.string.tap_start_in_shizuku),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else null
                    )
                }

                // Step 3: Grant permission (only show if running)
                if (isShizukuRunning) {
                    SetupStep(
                        number = 3,
                        title = stringResource(Res.string.grant_permission_to_github_store),
                        isComplete = hasPermission,
                        action = if (!hasPermission) {
                            {
                                Button(onClick = onRequestPermission) {
                                    Text(stringResource(Res.string.grant_permission))
                                }
                            }
                        } else null
                    )
                }

                if (isShizukuInstalled && isShizukuRunning && hasPermission) {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = stringResource(Res.string.shizuku_setup_complete),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        }
    )
}

@Composable
private fun SetupStep(
    number: Int,
    title: String,
    isComplete: Boolean,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = if (isComplete) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isComplete) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isComplete) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (isComplete) TextDecoration.LineThrough else null
            )

            if (!isComplete && action != null) {
                action()
            }
        }
    }
}