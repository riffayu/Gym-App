package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.AuthScreen
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.AiCoachRepository
import com.example.data.LiftLogRepository
import com.example.firebase.configureAppCheck
import com.example.ui.LiftLogViewModel
import com.example.ui.components.UserProfileAvatar
import com.example.ui.screens.AiCoachScreen
import com.example.ui.screens.PlaybookScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.SplitScreen
import com.example.ui.theme.LiftLogTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

const val ROUTE_SPLIT = "split_screen"
const val ROUTE_PLAYBOOK = "playbook_screen"
const val ROUTE_COACH = "ai_coach_screen"
const val ROUTE_PROGRESS = "progress_screen"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
)

private val TopLevelDestinations = listOf(
    NavDestinationItem(
        route = ROUTE_SPLIT,
        labelRes = R.string.nav_split,
        icon = Icons.Rounded.FitnessCenter,
        testTag = "nav_split"
    ),
    NavDestinationItem(
        route = ROUTE_PLAYBOOK,
        labelRes = R.string.nav_playbook,
        icon = Icons.Rounded.EventNote,
        testTag = "nav_playbook"
    ),
    NavDestinationItem(
        route = ROUTE_COACH,
        labelRes = R.string.nav_coach,
        icon = Icons.Rounded.AutoAwesome,
        testTag = "nav_coach"
    ),
    NavDestinationItem(
        route = ROUTE_PROGRESS,
        labelRes = R.string.nav_progress,
        icon = Icons.Rounded.Insights,
        testTag = "nav_progress"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            LiftLogTheme {
                LiftLogAppRoot()
            }
        }
    }
}

@Composable
private fun LiftLogAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val authUserFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authUserFlow.collectAsStateWithLifecycle(
        initialValue = Firebase.auth.currentUser
    )

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    val signedInUser = currentUser
    if (signedInUser == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(signedInUser.uid) {
            AuthenticatedLiftLogShell(
                user = signedInUser,
                onSignOut = {
                    signOut(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedLiftLogShell(
    user: FirebaseUser,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext

    val factory = remember(user.uid, appContext) {
        viewModelFactory {
            initializer {
                LiftLogViewModel(
                    repository = LiftLogRepository(appContext),
                    aiCoachRepository = AiCoachRepository(),
                    currentUserId = user.uid
                )
            }
        }
    }
    val viewModel: LiftLogViewModel = viewModel(key = user.uid, factory = factory)

    val exercisesState by viewModel.exercisesState.collectAsStateWithLifecycle()
    val workoutLogsState by viewModel.workoutLogsState.collectAsStateWithLifecycle()
    val bodyScansState by viewModel.bodyScansState.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val restTimerState by viewModel.restTimer.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_SPLIT
    val snackbarHostState = remember { SnackbarHostState() }
    val userPhotoUrl = user.photoUrl?.toString()

    LaunchedEffect(feedbackMessage) {
        val message = feedbackMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearFeedback()
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 640.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.app_name).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { navigateToTab(ROUTE_PROGRESS) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("topbar_profile_button")
                        ) {
                            UserProfileAvatar(
                                photoUrl = userPhotoUrl,
                                displayName = user.displayName ?: user.email,
                                size = 34.dp
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_SPLIT
                    ) {
                        composable(ROUTE_SPLIT) {
                            SplitScreen(
                                userDisplayName = user.displayName,
                                exercisesState = exercisesState,
                                workoutLogsState = workoutLogsState,
                                bodyScansState = bodyScansState,
                                selectedDay = selectedDay,
                                todaySessionDate = viewModel.todaySessionDate(),
                                restTimerState = restTimerState,
                                isMutating = isMutating,
                                onSelectDay = viewModel::selectDay,
                                onApplyBlueprint = viewModel::applyBlueprint,
                                onAddExercise = viewModel::addCustomExercise,
                                onUpdateExercise = viewModel::updateExercise,
                                onDeleteExercise = viewModel::deleteExercise,
                                onLogWorkingSet = { exercise, setNumber, weightKg, reps, rpe, notes ->
                                    viewModel.logWorkingSet(
                                        exercise = exercise,
                                        setNumber = setNumber,
                                        weightKg = weightKg,
                                        reps = reps,
                                        rpe = rpe,
                                        notes = notes
                                    )
                                },
                                onUpdateSetLog = viewModel::updateSetLog,
                                onToggleSetCompleted = viewModel::toggleSetCompleted,
                                onDeleteSetLog = viewModel::deleteSetLog,
                                onAddRest15s = { viewModel.addRestSeconds(15) },
                                onSkipRest = viewModel::skipRestTimer,
                                onNavigateToCoach = { navigateToTab(ROUTE_COACH) }
                            )
                        }

                        composable(ROUTE_PLAYBOOK) {
                            PlaybookScreen()
                        }

                        composable(ROUTE_COACH) {
                            AiCoachScreen(
                                bodyScansState = bodyScansState,
                                aiState = aiState,
                                isMutating = isMutating,
                                onGenerateProgram = viewModel::generateAiProgram,
                                onSaveScanOnly = viewModel::saveBodyScanOnly,
                                onApplyProgram = { preview ->
                                    viewModel.applyGeneratedProgram(preview) {
                                        navigateToTab(ROUTE_SPLIT)
                                    }
                                }
                            )
                        }

                        composable(ROUTE_PROGRESS) {
                            ProgressScreen(
                                userEmail = user.email,
                                userDisplayName = user.displayName,
                                userPhotoUrl = userPhotoUrl,
                                workoutLogsState = workoutLogsState,
                                bodyScansState = bodyScansState,
                                onUpdateSetLog = viewModel::updateSetLog,
                                onDeleteSetLog = viewModel::deleteSetLog,
                                onUpdateBodyScan = viewModel::updateBodyScan,
                                onDeleteBodyScan = viewModel::deleteBodyScan,
                                onSignOut = onSignOut
                            )
                        }
                    }
                }
            }
        }
    }
}
