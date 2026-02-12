package com.manjee.linkops.domain.usecase.favorite

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RemoveFavoriteUseCaseTest {

    private lateinit var fakeRepository: FakeFavoriteRepository
    private lateinit var useCase: RemoveFavoriteUseCase

    @BeforeTest
    fun setup() {
        fakeRepository = FakeFavoriteRepository()
        useCase = RemoveFavoriteUseCase(fakeRepository)
    }

    @Test
    fun `should remove existing favorite successfully`() = runTest {
        val added = fakeRepository.addFavorite("myapp://home", "Home").getOrThrow()

        val result = useCase(added.id)

        assertTrue(result.isSuccess)
        val remaining = fakeRepository.observeFavorites().first()
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun `should fail when removing non-existent favorite`() = runTest {
        val result = useCase("non-existent-id")

        assertTrue(result.isFailure)
        assertIs<NoSuchElementException>(result.exceptionOrNull())
    }

    @Test
    fun `should fail when repository fails`() = runTest {
        fakeRepository.shouldFail = true

        val result = useCase("any-id")

        assertTrue(result.isFailure)
    }

    @Test
    fun `should only remove the targeted favorite`() = runTest {
        fakeRepository.addFavorite("myapp://keep", "Keep")
        val toRemove = fakeRepository.addFavorite("myapp://remove", "Remove").getOrThrow()

        useCase(toRemove.id)

        val remaining = fakeRepository.observeFavorites().first()
        assertTrue(remaining.size == 1)
        assertTrue(remaining[0].uri == "myapp://keep")
    }
}
