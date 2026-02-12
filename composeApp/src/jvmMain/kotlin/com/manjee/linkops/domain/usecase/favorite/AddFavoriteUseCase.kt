package com.manjee.linkops.domain.usecase.favorite

import com.manjee.linkops.domain.model.Favorite
import com.manjee.linkops.domain.repository.FavoriteRepository

/**
 * Adds a deep link URI to the user's favorites
 */
class AddFavoriteUseCase(
    private val favoriteRepository: FavoriteRepository
) {
    /**
     * @param uri Deep link URI to save
     * @param name User-given display name
     * @return Result with the created Favorite or error
     */
    suspend operator fun invoke(uri: String, name: String): Result<Favorite> {
        return favoriteRepository.addFavorite(uri, name)
    }
}
