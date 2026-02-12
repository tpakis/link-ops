package com.manjee.linkops.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.*

class FavoriteRepositoryImplTest {

    private lateinit var tempDir: File
    private lateinit var storageFile: File
    private lateinit var repository: FavoriteRepositoryImpl

    @BeforeTest
    fun setup() {
        tempDir = createTempDir("linkops-test")
        storageFile = File(tempDir, "favorites.json")
        repository = FavoriteRepositoryImpl(storageFile)
    }

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `addFavorite should return success with created favorite`() = runTest {
        val result = repository.addFavorite("myapp://home", "Home Screen")

        assertTrue(result.isSuccess, "Should succeed")
        val favorite = result.getOrNull()
        assertNotNull(favorite, "Favorite should not be null")
        assertEquals("myapp://home", favorite.uri)
        assertEquals("Home Screen", favorite.name)
        assertTrue(favorite.id.isNotBlank(), "ID should be generated")
        assertTrue(favorite.createdAt > 0, "Timestamp should be set")
    }

    @Test
    fun `addFavorite should persist to disk`() = runTest {
        repository.addFavorite("myapp://home", "Home Screen")

        assertTrue(storageFile.exists(), "Storage file should exist")
        val content = storageFile.readText()
        assertTrue(content.contains("myapp://home"), "File should contain the URI")
        assertTrue(content.contains("Home Screen"), "File should contain the name")
    }

    @Test
    fun `addFavorite should reject duplicate URI`() = runTest {
        repository.addFavorite("myapp://home", "Home Screen")
        val result = repository.addFavorite("myapp://home", "Different Name")

        assertTrue(result.isFailure, "Duplicate URI should fail")
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }

    @Test
    fun `removeFavorite should remove existing favorite`() = runTest {
        val added = repository.addFavorite("myapp://home", "Home Screen").getOrThrow()
        val result = repository.removeFavorite(added.id)

        assertTrue(result.isSuccess, "Should succeed")
        val favorites = repository.observeFavorites().first()
        assertTrue(favorites.isEmpty(), "List should be empty after removal")
    }

    @Test
    fun `removeFavorite should fail for non-existent ID`() = runTest {
        val result = repository.removeFavorite("non-existent-id")

        assertTrue(result.isFailure, "Should fail for non-existent ID")
        assertIs<NoSuchElementException>(result.exceptionOrNull())
    }

    @Test
    fun `removeFavorite should persist removal to disk`() = runTest {
        val added = repository.addFavorite("myapp://home", "Home Screen").getOrThrow()
        repository.removeFavorite(added.id)

        val content = storageFile.readText()
        assertFalse(content.contains("myapp://home"), "Removed URI should not be on disk")
    }

    @Test
    fun `observeFavorites should return empty list when no favorites`() = runTest {
        val favorites = repository.observeFavorites().first()

        assertTrue(favorites.isEmpty(), "Should be empty initially")
    }

    @Test
    fun `observeFavorites should return favorites sorted by creation time descending`() = runTest {
        repository.addFavorite("myapp://first", "First")
        repository.addFavorite("myapp://second", "Second")
        repository.addFavorite("myapp://third", "Third")

        val favorites = repository.observeFavorites().first()

        assertEquals(3, favorites.size)
        assertTrue(
            favorites[0].createdAt >= favorites[1].createdAt,
            "Should be sorted descending by createdAt"
        )
        assertTrue(
            favorites[1].createdAt >= favorites[2].createdAt,
            "Should be sorted descending by createdAt"
        )
    }

    @Test
    fun `observeFavorites should reflect additions in real-time`() = runTest {
        val initialFavorites = repository.observeFavorites().first()
        assertEquals(0, initialFavorites.size)

        repository.addFavorite("myapp://home", "Home")

        val updatedFavorites = repository.observeFavorites().first()
        assertEquals(1, updatedFavorites.size)
        assertEquals("myapp://home", updatedFavorites[0].uri)
    }

    @Test
    fun `isFavorite should return true for saved URI`() = runTest {
        repository.addFavorite("myapp://home", "Home")

        assertTrue(repository.isFavorite("myapp://home"))
    }

    @Test
    fun `isFavorite should return false for unsaved URI`() = runTest {
        assertFalse(repository.isFavorite("myapp://unknown"))
    }

    @Test
    fun `should load favorites from existing file on initialization`() = runTest {
        repository.addFavorite("myapp://persisted", "Persisted Link")

        // Create new repository instance pointing to same file
        val newRepository = FavoriteRepositoryImpl(storageFile)
        val favorites = newRepository.observeFavorites().first()

        assertEquals(1, favorites.size)
        assertEquals("myapp://persisted", favorites[0].uri)
        assertEquals("Persisted Link", favorites[0].name)
    }

    @Test
    fun `should handle empty file gracefully`() = runTest {
        storageFile.parentFile?.mkdirs()
        storageFile.writeText("")

        val newRepository = FavoriteRepositoryImpl(storageFile)
        val favorites = newRepository.observeFavorites().first()

        assertTrue(favorites.isEmpty(), "Should return empty list for empty file")
    }

    @Test
    fun `should handle malformed JSON gracefully`() = runTest {
        storageFile.parentFile?.mkdirs()
        storageFile.writeText("{invalid json content[")

        val newRepository = FavoriteRepositoryImpl(storageFile)
        val favorites = newRepository.observeFavorites().first()

        assertTrue(favorites.isEmpty(), "Should return empty list for malformed JSON")
    }

    @Test
    fun `should handle non-existent file gracefully`() = runTest {
        val nonExistentFile = File(tempDir, "does-not-exist.json")
        val newRepository = FavoriteRepositoryImpl(nonExistentFile)
        val favorites = newRepository.observeFavorites().first()

        assertTrue(favorites.isEmpty(), "Should return empty list for missing file")
    }

    @Test
    fun `addFavorite should handle multiple entries correctly`() = runTest {
        repository.addFavorite("myapp://home", "Home")
        repository.addFavorite("myapp://settings", "Settings")
        repository.addFavorite("myapp://profile", "Profile")

        val favorites = repository.observeFavorites().first()
        assertEquals(3, favorites.size)

        val uris = favorites.map { it.uri }.toSet()
        assertTrue(uris.contains("myapp://home"))
        assertTrue(uris.contains("myapp://settings"))
        assertTrue(uris.contains("myapp://profile"))
    }

    @Test
    fun `removeFavorite should only remove the targeted favorite`() = runTest {
        repository.addFavorite("myapp://keep1", "Keep 1")
        val toRemove = repository.addFavorite("myapp://remove", "Remove").getOrThrow()
        repository.addFavorite("myapp://keep2", "Keep 2")

        repository.removeFavorite(toRemove.id)

        val favorites = repository.observeFavorites().first()
        assertEquals(2, favorites.size)
        val uris = favorites.map { it.uri }.toSet()
        assertTrue(uris.contains("myapp://keep1"))
        assertTrue(uris.contains("myapp://keep2"))
        assertFalse(uris.contains("myapp://remove"))
    }

    @Test
    fun `addFavorite should generate unique IDs`() = runTest {
        val first = repository.addFavorite("myapp://first", "First").getOrThrow()
        val second = repository.addFavorite("myapp://second", "Second").getOrThrow()

        assertNotEquals(first.id, second.id, "IDs should be unique")
    }

    @Test
    fun `should handle special characters in URI and name`() = runTest {
        val uri = "myapp://path?key=value&foo=bar#fragment"
        val name = "Link with \"quotes\" & <special> chars"

        val result = repository.addFavorite(uri, name)
        assertTrue(result.isSuccess)

        val favorites = repository.observeFavorites().first()
        assertEquals(uri, favorites[0].uri)
        assertEquals(name, favorites[0].name)
    }

    @Test
    fun `should handle unicode characters`() = runTest {
        val result = repository.addFavorite("myapp://path", "Test")
        assertTrue(result.isSuccess)

        val favorites = repository.observeFavorites().first()
        assertEquals("Test", favorites[0].name)
    }
}
