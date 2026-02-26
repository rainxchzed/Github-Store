package zed.rainxch.profile.presentation.components.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import zed.rainxch.core.presentation.components.GitHubStoreImage
import zed.rainxch.core.presentation.components.GithubStoreButton
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.profile.domain.model.UserProfile
import zed.rainxch.profile.presentation.ProfileAction
import zed.rainxch.profile.presentation.ProfileState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun LazyListScope.accountSection(
    state: ProfileState,
    onAction: (ProfileAction) -> Unit,
) {
    item {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.userProfile == null) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            } else {
                GitHubStoreImage(
                    imageModel = {
                        state.userProfile.imageUrl
                    },
                    modifier = Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )

                Spacer(Modifier.height(8.dp))
            }

            if (state.userProfile?.name != null) {
                Text(
                    text = state.userProfile.name,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "@${state.userProfile.username}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                state.userProfile.bio?.let { bio ->
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Sign in to GitHub",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Unlock the full experience. Manage your apps, sync your preference, and browser faster.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (state.userProfile != null) {
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Repos",
                        value = "24",
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        label = "Followers",
                        value = "1.2K",
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        label = "Following",
                        value = "56",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (state.userProfile == null) {
                Spacer(Modifier.height(8.dp))

                GithubStoreButton(
                    text = "Login",
                    onClick = {
                        onAction(ProfileAction.OnLoginClick)
                    },
                    modifier = Modifier
                        .width(480.dp)
                        .padding(horizontal = 8.dp)
                )
            }
        }

    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.secondary
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                maxLines = 1,
                style = MaterialTheme.typography.titleLargeEmphasized,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = label,
                maxLines = 1,
                style = MaterialTheme.typography.bodyLargeEmphasized,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountSectionPreview() {
    GithubStoreTheme {
        LazyColumn {
            accountSection(
                state = ProfileState(),
                onAction = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountSectionUserPreview() {
    GithubStoreTheme {
        LazyColumn {
            accountSection(
                state = ProfileState(
                    userProfile = UserProfile(
                        id = 1,
                        imageUrl = "",
                        name = "Octocat",
                        username = "the_octocat",
                        bio = " Language Savant. If your repository's language is being reported incorrectly, send us a pull request! ",
                        repositoryCount = 8,
                        followers = 21900,
                        following = 9
                    )
                ),
                onAction = { }
            )
        }
    }
}