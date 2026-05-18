package com.example.flux.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.flux.feature.export.ExportScreen
import com.example.flux.feature.library.LibraryScreen
import com.example.flux.feature.reader.ReaderScreen
import com.example.flux.ui.theme.FluxTheme

@Composable
fun FluxNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = FluxRoute.Library.route,
        modifier = modifier,
    ) {
        composable(FluxRoute.Library.route) {
            LibraryScreen(
                onBookClick = { bookId ->
                    navController.navigate(FluxRoute.Reader(bookId).route)
                },
                onExportClick = {
                    navController.navigate(FluxRoute.Export.route)
                },
            )
        }

        composable(
            route = FluxRoute.Reader.ROUTE,
            arguments = listOf(
                navArgument(FluxRoute.Reader.ARG) { type = NavType.StringType },
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = FluxRoute.Reader.DEEP_LINK },
            ),
        ) { backStackEntry ->
            val bookId = requireNotNull(backStackEntry.arguments?.getString(FluxRoute.Reader.ARG)) {
                "bookId argument missing from Reader back-stack entry"
            }
            // Dynamic color suppressed in reader to allow sepia/night theme overrides
            FluxTheme(dynamicColor = false) {
                ReaderScreen(
                    bookId = bookId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }

        composable(FluxRoute.Export.route) {
            ExportScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
