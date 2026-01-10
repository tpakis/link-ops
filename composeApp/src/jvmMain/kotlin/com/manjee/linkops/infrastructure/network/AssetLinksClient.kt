package com.manjee.linkops.infrastructure.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * HTTP client for fetching assetlinks.json files
 */
class AssetLinksClient {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 10000
            connectTimeoutMillis = 5000
            socketTimeoutMillis = 10000
        }

        // Don't follow redirects automatically so we can detect them
        followRedirects = false
    }

    /**
     * Fetch assetlinks.json from a domain
     *
     * @param domain The domain (e.g., "example.com")
     * @return Response with content and metadata
     */
    suspend fun fetch(domain: String): AssetLinksResponse {
        val url = "https://$domain/.well-known/assetlinks.json"

        return try {
            val response: HttpResponse = httpClient.get(url) {
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.UserAgent, "LinkOps/1.0")
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val body = response.bodyAsText()
                    val contentType = response.contentType()?.toString() ?: ""
                    val wasRedirected = response.request.url.host != domain

                    AssetLinksResponse.Success(
                        content = body,
                        contentType = contentType,
                        wasRedirected = wasRedirected,
                        finalUrl = response.request.url.toString()
                    )
                }
                HttpStatusCode.NotFound -> {
                    AssetLinksResponse.NotFound(url)
                }
                HttpStatusCode.MovedPermanently,
                HttpStatusCode.Found,
                HttpStatusCode.TemporaryRedirect,
                HttpStatusCode.PermanentRedirect -> {
                    val location = response.headers[HttpHeaders.Location]
                    AssetLinksResponse.Redirect(
                        originalUrl = url,
                        redirectUrl = location
                    )
                }
                else -> {
                    AssetLinksResponse.HttpError(
                        statusCode = response.status.value,
                        message = response.status.description
                    )
                }
            }
        } catch (e: HttpRequestTimeoutException) {
            AssetLinksResponse.NetworkError(
                message = "Request timed out",
                cause = e
            )
        } catch (e: Exception) {
            AssetLinksResponse.NetworkError(
                message = e.message ?: "Unknown network error",
                cause = e
            )
        }
    }

    /**
     * Fetch with redirect following (for getting final content)
     */
    suspend fun fetchWithRedirects(domain: String, maxRedirects: Int = 3): AssetLinksResponse {
        var currentDomain = domain
        var redirectCount = 0

        while (redirectCount < maxRedirects) {
            when (val response = fetch(currentDomain)) {
                is AssetLinksResponse.Success -> return response
                is AssetLinksResponse.Redirect -> {
                    val redirectUrl = response.redirectUrl
                    if (redirectUrl == null) {
                        return AssetLinksResponse.NetworkError(
                            message = "Redirect without location header"
                        )
                    }
                    // Extract domain from redirect URL
                    val newDomain = try {
                        Url(redirectUrl).host
                    } catch (e: Exception) {
                        return AssetLinksResponse.NetworkError(
                            message = "Invalid redirect URL: $redirectUrl"
                        )
                    }
                    currentDomain = newDomain
                    redirectCount++
                }
                else -> return response
            }
        }

        return AssetLinksResponse.NetworkError(
            message = "Too many redirects (max: $maxRedirects)"
        )
    }

    fun close() {
        httpClient.close()
    }
}

/**
 * Response from fetching assetlinks.json
 */
sealed class AssetLinksResponse {
    data class Success(
        val content: String,
        val contentType: String,
        val wasRedirected: Boolean,
        val finalUrl: String
    ) : AssetLinksResponse()

    data class NotFound(
        val url: String
    ) : AssetLinksResponse()

    data class Redirect(
        val originalUrl: String,
        val redirectUrl: String?
    ) : AssetLinksResponse()

    data class HttpError(
        val statusCode: Int,
        val message: String
    ) : AssetLinksResponse()

    data class NetworkError(
        val message: String,
        val cause: Throwable? = null
    ) : AssetLinksResponse()
}
