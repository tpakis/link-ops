package com.manjee.linkops.domain.model

/**
 * Represents a user-saved favorite deep link for quick access
 */
data class Favorite(
    val id: String,
    val uri: String,
    val name: String,
    val createdAt: Long
) {
    val displayName: String
        get() = name.ifBlank { uri }
}
