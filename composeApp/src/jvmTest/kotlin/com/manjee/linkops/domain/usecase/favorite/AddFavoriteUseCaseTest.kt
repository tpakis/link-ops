package com.manjee.linkops.domain.usecase.favorite

import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AddFavoriteUseCaseTest {

    private lateinit var fakeRepository: FakeFavoriteRepository
    private lateinit var useCase: AddFavoriteUseCase

    @BeforeTest
    fun setup() {
        fakeRepository = FakeFavoriteRepository()
        useCase = AddFavoriteUseCase(fakeRepository)
    }

    @Test
    fun `should add favorite successfully`() = runTest {
        val result = useCase("myapp://home", "Home Screen")

        assertTrue(result.isSuccess)
        val favorite = result.getOrNull()
        assertNotNull(favorite)
        assertEquals("myapp://home", favorite.uri)
        assertEquals("Home Screen", favorite.name)
    }

    @Test
    fun `should fail when adding duplicate URI`() = runTest {
        useCase("myapp://home", "Home Screen")
        val result = useCase("myapp://home", "Different Name")

        assertTrue(result.isFailure)
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }

    @Test
    fun `should fail when repository fails`() = runTest {
        fakeRepository.shouldFail = true
        fakeRepository.failureException = RuntimeException("Disk error")

        val result = useCase("myapp://home", "Home")

        assertTrue(result.isFailure)
        assertEquals("Disk error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `should generate unique favorite with timestamp`() = runTest {
        val result = useCase("myapp://home", "Home")

        assertTrue(result.isSuccess)
        val favorite = result.getOrThrow()
        assertTrue(favorite.id.isNotBlank())
        assertTrue(favorite.createdAt > 0)
    }
}
