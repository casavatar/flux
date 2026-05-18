package com.example.flux.navigation

sealed class FluxRoute(val route: String) {

    data object Library : FluxRoute("library")

    data object Export : FluxRoute("export")

    data class Reader(val bookId: String) : FluxRoute("reader/$bookId") {
        companion object {
            const val ROUTE = "reader/{bookId}"
            const val ARG = "bookId"
            const val DEEP_LINK = "flux://reader/{bookId}"
        }
    }
}
