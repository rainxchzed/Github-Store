package zed.rainxch.githubstore.app.app_state.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import zed.rainxch.githubstore.core.presentation.theme.GithubStoreTheme
import zed.rainxch.githubstore.network.RateLimitInfo
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Composable
fun RateLimitDialog(
    rateLimitInfo: RateLimitInfo?,
    isAuthenticated: Boolean,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit
) {
    val timeUntilReset = remember(rateLimitInfo) {
        rateLimitInfo?.let { (it.timeUntilReset() / 1000 / 60).toInt() } // minutes
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = when {
                    rateLimitInfo?.reset == Instant.DISTANT_FUTURE -> {
                        if (isAuthenticated) {
                            "Authentication failed. Please sign in again."
                        } else {
                            "Sign in to access GitLab features."
                        }
                    }

                    !isAuthenticated -> "Sign in to access GitLab features."
                    rateLimitInfo?.isExhausted == true -> "Rate limit exceeded. Wait until ${rateLimitInfo.reset} or sign in for higher limits."
                    else -> "Rate limit exceeded."
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when {
                        rateLimitInfo?.reset == Instant.DISTANT_FUTURE -> {
                            if (isAuthenticated) {
                                "Your session may have expired or the token is invalid."
                            } else {
                                "You've used all ${rateLimitInfo?.limit} free API requests."
                            }
                        }

                        isAuthenticated -> "You've used all ${rateLimitInfo?.limit} API requests."
                        else -> "You've used all ${rateLimitInfo?.limit} free API requests."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                if (rateLimitInfo?.reset != Instant.DISTANT_FUTURE) {
                    Text(
                        text = "Resets in $timeUntilReset minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!isAuthenticated || rateLimitInfo?.reset == Instant.DISTANT_FUTURE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 Sign in to get 5,000 requests per hour instead of 60!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSignIn) {
                Text(
                    text = if (isAuthenticated && rateLimitInfo?.reset == Instant.DISTANT_FUTURE) "Sign In Again" else "Sign In",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )
}

@Preview
@Composable
fun RateLimitDialogPreview() {
    GithubStoreTheme {
        RateLimitDialog(
            rateLimitInfo = null,
            isAuthenticated = false,
            onDismiss = {

            },
            onSignIn = {

            }
        )
    }
}