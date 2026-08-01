package com.example.ui.navigation

sealed class NavRoutes(val route: String) {
    object Dashboard : NavRoutes("dashboard")
    object Calendar : NavRoutes("calendar")
    object Decks : NavRoutes("decks")
    object DeckDetail : NavRoutes("deck_detail/{deckId}") {
        fun createRoute(deckId: Long) = "deck_detail/$deckId"
    }
    object FlashcardReview : NavRoutes("review/{deckId}") {
        fun createRoute(deckId: Long) = "review/$deckId"
    }
    object QuizEngine : NavRoutes("quiz/{deckId}") {
        fun createRoute(deckId: Long) = "quiz/$deckId"
    }
    object Statistics : NavRoutes("statistics")
}
