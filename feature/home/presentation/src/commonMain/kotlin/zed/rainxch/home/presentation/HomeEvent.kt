package zed.rainxch.home.presentation

sealed interface HomeEvent {
    data object OnScrollToListTop : HomeEvent
}