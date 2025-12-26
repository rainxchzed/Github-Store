package zed.rainxch.githubstore.feature.auth.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import githubstore.composeapp.generated.resources.Res
import githubstore.composeapp.generated.resources.app_icon
import githubstore.composeapp.generated.resources.ic_github
import githubstore.composeapp.generated.resources.ic_gitlab
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import zed.rainxch.githubstore.core.domain.model.ApiPlatform
import zed.rainxch.githubstore.core.domain.model.DeviceStart
import zed.rainxch.githubstore.core.presentation.components.GithubStoreButton
import zed.rainxch.githubstore.core.presentation.theme.GithubStoreTheme
import zed.rainxch.githubstore.core.presentation.utils.ObserveAsEvents

@Composable
fun AuthenticationRoot(
    onNavigateToHome: () -> Unit,
    viewModel: AuthenticationViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            AuthenticationEvents.OnNavigateToMain -> {
                onNavigateToHome()
            }
        }
    }

    AuthenticationScreen(
        state = state,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthenticationScreen(
    state: AuthenticationState,
    onAction: (AuthenticationAction) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(vertical = 32.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(Res.drawable.app_icon),
                contentDescription = null,
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(32.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(16.dp))

            when (val authState = state.loginState) {
                is AuthLoginState.LoggedOut -> {
                    StateLoggedOut(
                        state = state,
                        onAction = onAction
                    )
                }

                is AuthLoginState.DevicePrompt -> {
                    StateDevicePrompt(
                        state = state,
                        authState = authState,
                        onAction = onAction
                    )
                }

                is AuthLoginState.Pending -> {
                    CircularWavyProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Waiting for authorization...")
                }

                is AuthLoginState.LoggedIn -> {
                    Text("Signed in!", style = MaterialTheme.typography.titleLarge)

                    Spacer(Modifier.height(8.dp))

                    Text("You can now use the app. Redirecting...")
                }

                is AuthLoginState.Error -> {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Error: ${authState.message}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(Modifier.height(12.dp))

                    GithubStoreButton(
                        text = "Try again",
                        onClick = {
                            onAction(AuthenticationAction.StartLogin)
                        },
                        modifier = Modifier.fillMaxWidth(.7f)
                    )
                    Spacer(Modifier.weight(2f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StateDevicePrompt(
    state: AuthenticationState,
    authState: AuthLoginState.DevicePrompt,
    onAction: (AuthenticationAction) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = if (state.currentApiPlatform == ApiPlatform.Github) {
                "Enter this code on GitHub:"
            } else "Enter this code on GitLab:",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = authState.start.userCode,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            IconButton(
                shapes = IconButtonDefaults.shapes(),
                onClick = {
                    onAction(AuthenticationAction.CopyCode(authState.start))
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(
                    imageVector = if (state.copied) {
                        Icons.Default.DoneAll
                    } else Icons.Default.ContentCopy,
                    contentDescription = "Copy the code"
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        state.info?.let { info ->
            Text(
                text = info,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(16.dp))

        GithubStoreButton(
            text = if (state.currentApiPlatform == ApiPlatform.Github) {
                "Open GitHub"
            } else "Open GitLab",
            onClick = {
                onAction(AuthenticationAction.OpenPlatform(authState.start))
            },
            icon = {
                Icon(
                    painter = painterResource(
                        resource = if (state.currentApiPlatform == ApiPlatform.Github) {
                            Res.drawable.ic_github
                        } else Res.drawable.ic_gitlab
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
        )

        Spacer(Modifier.weight(2f))

    }
}

@Composable
fun StateLoggedOut(
    state: AuthenticationState,
    onAction: (AuthenticationAction) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Unlock the Full\nExperience",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Card(
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenWith,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "More Requests",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Sign in to get higher API rate limits and avoid interruptions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.weight(1f))

        GithubStoreButton(
            text = if (state.currentApiPlatform == ApiPlatform.Github) {
                "Sign in with Github"
            } else "Sign in with GitLab",
            onClick = {
                onAction(AuthenticationAction.StartLogin)
            },
            icon = {
                Icon(
                    painter = painterResource(
                        if (state.currentApiPlatform == ApiPlatform.Github) {
                            Res.drawable.ic_github
                        } else Res.drawable.ic_gitlab
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun Preview() {
    GithubStoreTheme {
        AuthenticationScreen(
            state = AuthenticationState(
                loginState = AuthLoginState.Error(
                    message = "Halo"
                )
            ),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun Preview1() {
    GithubStoreTheme {
        AuthenticationScreen(
            state = AuthenticationState(
                loginState = AuthLoginState.LoggedOut
            ),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun Preview2() {
    GithubStoreTheme {
        AuthenticationScreen(
            state = AuthenticationState(
                loginState = AuthLoginState.DevicePrompt(
                    DeviceStart(
                        deviceCode = "",
                        userCode = "2102-UHHUF",
                        verificationUri = "",
                        expiresInSec = 10

                    )
                )
            ),
            onAction = {}
        )
    }
}