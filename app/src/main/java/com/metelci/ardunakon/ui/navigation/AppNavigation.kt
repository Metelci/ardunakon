package com.metelci.ardunakon.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using Kotlin Serialization.
 */
sealed interface AppRoute {
    /**
     * Main control screen for device interaction.
     */
    @Serializable
    data object Control : AppRoute

    /**
     * Onboarding/tutorial flow for first-time users.
     */
    @Serializable
    data object Onboarding : AppRoute
}

/**
 * Deep link URI pattern for the app.
 */
private const val DEEP_LINK_BASE_URI = "ardunakon://"

/**
 * Main navigation host composable for the app.
 *
 * @param navController The navigation controller to use.
 * @param startDestination The initial destination route.
 * @param onTakeTutorial Callback when user requests to take the tutorial.
 * @param onQuitApp Callback when user quits the app.
 * @param controlContent The composable content for the control screen.
 * @param onboardingContent The composable content for the onboarding flow.
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: AppRoute = AppRoute.Control,
    controlContent: @Composable (onTakeTutorial: () -> Unit) -> Unit,
    onboardingContent: @Composable (onComplete: () -> Unit, onSkip: () -> Unit) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<AppRoute.Control>(
            deepLinks = listOf(
                navDeepLink<AppRoute.Control>(basePath = "${DEEP_LINK_BASE_URI}control")
            )
        ) {
            controlContent {
                navController.navigate(AppRoute.Onboarding) {
                    launchSingleTop = true
                }
            }
        }

        composable<AppRoute.Onboarding>(
            deepLinks = listOf(
                navDeepLink<AppRoute.Onboarding>(basePath = "${DEEP_LINK_BASE_URI}onboarding")
            )
        ) {
            val navigateToControl: () -> Unit = {
                navController.navigate(AppRoute.Control) {
                    popUpTo(AppRoute.Control) { inclusive = true }
                    launchSingleTop = true
                }
            }
            onboardingContent(navigateToControl, navigateToControl)
        }
    }
}

/**
 * Extension function to navigate to a route with standard back stack handling.
 */
fun NavHostController.navigateSingleTop(route: AppRoute) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
    }
}
