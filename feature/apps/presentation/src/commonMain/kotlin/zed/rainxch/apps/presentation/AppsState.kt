package zed.rainxch.apps.presentation

import zed.rainxch.apps.presentation.model.AppItem
import zed.rainxch.apps.presentation.model.UpdateAllProgress

data class AppsState(
    val apps: List<AppItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isUpdatingAll: Boolean = false,
    val updateAllProgress: UpdateAllProgress? = null,
    val updateAllButtonEnabled: Boolean = true
)