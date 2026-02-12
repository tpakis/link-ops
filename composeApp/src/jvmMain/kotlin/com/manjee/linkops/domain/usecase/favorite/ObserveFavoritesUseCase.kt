package com.manjee.linkops.domain.usecase.favorite

import com.manjee.linkops.domain.model.Favorite
import com.manjee.linkops.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes the user's saved favorite deep links
 */
class ObserveFavoritesUseCase(
    private val favoriteRepository: FavoriteRepository
) {
    /**
     * @return Flow of favorite list, sorted by creation time descending
     */
    operator fun invoke(): Flow<List<Favorite>> {
        return favoriteRepository.observeFavorites()
    }
}
