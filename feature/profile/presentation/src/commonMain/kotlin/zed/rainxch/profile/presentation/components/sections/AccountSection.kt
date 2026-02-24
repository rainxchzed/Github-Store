package zed.rainxch.profile.presentation.components.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import zed.rainxch.core.presentation.components.GitHubStoreImage
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.profile.presentation.ProfileAction
import zed.rainxch.profile.presentation.ProfileState

fun LazyListScope.accountSection(
    state: ProfileState,
    onAction: (ProfileAction) -> Unit,
) {
    item {
        Column (
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GitHubStoreImage(
                imageModel = {
                    if (state.userProfile == null) {
                        Icons.Outlined.AccountCircle
                    } else {
                        state.userProfile.imageUrl
                    }
                },
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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