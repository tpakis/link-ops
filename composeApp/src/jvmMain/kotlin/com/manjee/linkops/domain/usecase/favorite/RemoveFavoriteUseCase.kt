package com.manjee.linkops.domain.usecase.favorite

import com.manjee.linkops.domain.repository.FavoriteRepository

/**
 * Removes a favorite deep link by ID
 */
class RemoveFavoriteUseCase(
    private val favoriteRepository: FavoriteRepository
) {
    /**
     * @param id Favorite ID to remove
     * @return Result with success or error
     */
    suspend operator fun invoke(id: String): Result<Unit> {
        return favoriteRepository.removeFavorite(id)
    }
}
