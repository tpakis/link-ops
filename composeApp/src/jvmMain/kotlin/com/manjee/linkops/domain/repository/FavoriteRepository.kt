package com.manjee.linkops.domain.repository

import com.manjee.linkops.domain.model.Favorite
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing user's favorite deep links
 */
interface FavoriteRepository {
    /**
     * Observes all saved favorites in real-time
     * @return Flow of favorite list, sorted by creation time descending
     */
    fun observeFavorites(): Flow<List<Favorite>>

    /**
     * Adds a deep link to favorites
     * @param uri Deep link URI to save
     * @param name User-given display name
     * @return Result with the created Favorite or error
     */
    suspend fun addFavorite(uri: String, name: String): Result<Favorite>

    /**
     * Removes a favorite by ID
     * @param id Favorite ID to remove
     * @return Result with success or error
     */
    suspend fun removeFavorite(id: String): Result<Unit>

    /**
     * Checks if a URI is already saved as a favorite
     * @param uri Deep link URI to check
     * @return true if the URI is a favorite
     */
    suspend fun isFavorite(uri: String): Boolean
}
