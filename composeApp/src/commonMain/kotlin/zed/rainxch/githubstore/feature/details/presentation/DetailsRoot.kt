package zed.rainxch.githubstore.feature.details.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import githubstore.composeapp.generated.resources.Res
import githubstore.composeapp.generated.resources.navigate_back
import githubstore.composeapp.generated.resources.open_repository
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.githubstore.core.presentation.theme.GithubStoreTheme
import zed.rainxch.githubstore.core.presentation.utils.ObserveAsEvents
import zed.rainxch.githubstore.feature.details.presentation.components.sections.about
import zed.rainxch.githubstore.feature.details.presentation.components.sections.header
import zed.rainxch.githubstore.feature.details.presentation.components.sections.logs
import zed.rainxch.githubstore.feature.details.presentation.components.sections.author
import zed.rainxch.githubstore.feature.details.presentation.components.sections.stats
import zed.rainxch.githubstore.feature.details.presentation.components.sections.whatsNew
import zed.rainxch.githubstore.feature.details.presentation.components.states.ErrorState
import zed.rainxch.githubstore.feature.details.presentation.utils.LocalTopbarLiquidState
import zed.rainxch.githubstore.feature.details.presentation.utils.isLiquidTopbarEnabled

@Composable
fun DetailsRoot(
    onOpenRepositoryInApp: (repoId: Int) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: DetailsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is DetailsEvent.OnOpenRepositoryInApp -> {
                onOpenRepositoryInApp(event.repositoryId)
            }

            is DetailsEvent.InstallTrackingFailed -> {

            }

            is DetailsEvent.OnMessage -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    DetailsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = { action ->
            when (action) {
                DetailsAction.OnNavigateBackClick -> {
                    onNavigateBack()
                }

                is DetailsAction.OpenAuthorInApp -> {
                    // TODO will be implemented in future
                }

                else -> {
                    viewModel.onAction(action)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DetailsScreen(
    state: DetailsState,
    onAction: (DetailsAction) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val liquidTopbarState = rememberLiquidState()

    CompositionLocalProvider(
        value = LocalTopbarLiquidState provides liquidTopbarState
    ) {
        Scaffold(
            topBar = {
                DetailsTopbar(onAction, state, liquidTopbarState)
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.liquefiable(liquidTopbarState),
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            }
        ) { innerPadding ->

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator()
                }

                return@Scaffold
            }

            if (state.errorMessage != null) {
                ErrorState(state.errorMessage, onAction)

                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .liquefiable(liquidTopbarState)
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                header(
                    state = state,
                    onAction = onAction,
                )

                state.stats?.let { stats ->
                    stats(repoStats = stats)
                }

                state.readmeMarkdown?.let {
                    about(
                        readmeMarkdown = state.readmeMarkdown,
                        readmeLanguage = state.readmeLanguage
                    )
                }

                state.latestRelease?.let { latestRelease ->
                    whatsNew(latestRelease)
                }

                state.userProfile?.let { userProfile ->
                    author(
                        author = userProfile,
                        onAction = onAction
                    )
                }

                if (state.installLogs.isNotEmpty()) {
                    logs(state)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DetailsTopbar(
    onAction: (DetailsAction) -> Unit,
    state: DetailsState,
    liquidTopbarState: LiquidState
) {
    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(
                shapes = IconButtonDefaults.shapes(),
                onClick = {
                    onAction(DetailsAction.OnNavigateBackClick)
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.navigate_back),
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        actions = {
            Row (
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.repository != null) {
                    IconButton(
                        onClick = {
                            onAction(DetailsAction.OnToggleFavorite)
                        },
                        shapes = IconButtonDefaults.shapes(),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = if(state.isFavorite) {
                                Icons.Default.Favorite
                            } else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                        )
                    }
                }

                state.repository?.htmlUrl?.let {
                    IconButton(
                        shapes = IconButtonDefaults.shapes(),
                        onClick = {
                            onAction(DetailsAction.OpenRepoInBrowser)
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = stringResource(Res.string.open_repository),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.then(
            if (isLiquidTopbarEnabled()) {
                Modifier.liquid(liquidTopbarState) {
                    this.shape = CutCornerShape(0.dp)
                    this.frost = 8.dp
                    this.curve = .4f
                    this.refraction = .1f
                    this.dispersion = .2f
                }
            } else Modifier
        )
    )
}

@Preview
@Composable
private fun Preview() {
    GithubStoreTheme {
        DetailsScreen(
            state = DetailsState(
                isLoading = false
            ),
            onAction = {},
            snackbarHostState = SnackbarHostState()
        )
    }
}