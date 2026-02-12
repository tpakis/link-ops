package com.manjee.linkops.domain.usecase.favorite

import com.manjee.linkops.domain.model.Favorite
import com.manjee.linkops.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Fake implementation of FavoriteRepository for testing
 */
class FakeFavoriteRepository : FavoriteRepository {

    private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())

    var shouldFail = false
    var failureException: Exception = RuntimeException("Simulated failure")

    override fun observeFavorites(): Flow<List<Favorite>> {
        return _favorites.map { it.sortedByDescending { f -> f.createdAt } }
    }

    override suspend fun addFavorite(uri: String, name: String): Result<Favorite> {
        if (shouldFail) return Result.failure(failureException)

        if (_favorites.value.any { it.uri == uri }) {
            return Result.failure(IllegalArgumentException("URI already exists: $uri"))
        }

        val favorite = Favorite(
            id = UUID.randomUUID().toString(),
            uri = uri,
            name = name,
            createdAt = System.currentTimeMillis()
        )
        _favorites.value = _favorites.value + favorite
        return Result.success(favorite)
    }

    override suspend fun removeFavorite(id: String): Result<Unit> {
        if (shouldFail) return Result.failure(failureException)

        val existing = _favorites.value
        val updated = existing.filter { it.id != id }
        if (updated.size == existing.size) {
            return Result.failure(NoSuchElementException("Not found: $id"))
        }
        _favorites.value = updated
        return Result.success(Unit)
    }

    override suspend fun isFavorite(uri: String): Boolean {
        return _favorites.value.any { it.uri == uri }
    }
}
