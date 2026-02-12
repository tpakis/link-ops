package com.manjee.linkops.domain.usecase.favorite

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserveFavoritesUseCaseTest {

    private lateinit var fakeRepository: FakeFavoriteRepository
    private lateinit var useCase: ObserveFavoritesUseCase

    @BeforeTest
    fun setup() {
        fakeRepository = FakeFavoriteRepository()
        useCase = ObserveFavoritesUseCase(fakeRepository)
    }

    @Test
    fun `should return empty list when no favorites`() = runTest {
        val favorites = useCase().first()
        assertTrue(favorites.isEmpty())
    }

    @Test
    fun `should return favorites sorted by creation time descending`() = runTest {
        fakeRepository.addFavorite("myapp://first", "First")
        fakeRepository.addFavorite("myapp://second", "Second")

        val favorites = useCase().first()

        assertEquals(2, favorites.size)
        assertTrue(favorites[0].createdAt >= favorites[1].createdAt)
    }

    @Test
    fun `should reflect additions from repository`() = runTest {
        fakeRepository.addFavorite("myapp://home", "Home")

        val favorites = useCase().first()

        assertEquals(1, favorites.size)
        assertEquals("myapp://home", favorites[0].uri)
        assertEquals("Home", favorites[0].name)
    }

    @Test
    fun `should reflect removals from repository`() = runTest {
        val added = fakeRepository.addFavorite("myapp://home", "Home").getOrThrow()
        fakeRepository.removeFavorite(added.id)

        val favorites = useCase().first()
        assertTrue(favorites.isEmpty())
    }
}
