package zed.rainxch.profile.presentation.components.sections

import androidx.compose.foundation.lazy.LazyListScope
import zed.rainxch.profile.presentation.ProfileAction
import zed.rainxch.profile.presentation.ProfileState

fun LazyListScope.profile(
    state: ProfileState,
    onAction: (ProfileAction) -> Unit,
) {
    accountSection(
        state = state,
        onAction = onAction
    )
}