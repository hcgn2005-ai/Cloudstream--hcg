package com.vidbox.plugin

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Main provider class for VidBoxPlugin.
 * Handles searching, loading home page content, and extracting video URLs from vidbox.cc.
 */
class MainProvider : MainAPI() {
    override val name: String = "VidBox"
    override val mainUrl: String = "https://vidbox.cc"
    override val supportedTypes: Set<TvType> = setOf(TvType.Movie, TvType.TvSeries)

    override val hasMainPage: Boolean = true
    override val hasSearch: Boolean = true

    /**
     * Loads the main page with categories like "Popular Movies" and "Top TV Shows".
     */
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        return try {
            val homePageList = mutableListOf<HomePageList>()

            // Load popular movies from /movies
            val movies = scrapeItems("$mainUrl/movies?page=$page")
            homePageList.add(HomePageList("Popular Movies", movies))

            // Load top TV shows from /tv
            val tvShows = scrapeItems("$mainUrl/tv?page=$page")
            homePageList.add(HomePageList("Top TV Shows", tvShows))

            HomePageResponse(homePageList)
        } catch (e: Exception) {
            println("Error loading main page: ${e.message}")
            HomePageResponse(emptyList())
        }
    }

    /**
     * Searches for content based on a query.
     */
    override suspend fun search(query: String): List<SearchResponse> {
        return try {
            scrapeItems("$mainUrl/search?q=$query")
        } catch (e: Exception) {
            println("Error searching: ${e.message}")
            emptyList()
        }
    }

    /**
     * Loads detailed information for a specific item (e.g., movie or TV show).
     */
    override suspend fun load(url: String): LoadResponse {
        return try {
            val doc = Jsoup.connect(url).get()
            val title = doc.select("h1.title, .movie-title").text() ?: "Unknown Title"
            val description = doc.select(".description, .plot").text() ?: "No description available."
            val posterUrl = doc.select("img.poster, .poster img").attr("src")?.let { fixUrl(it) }
            val type = if (url.contains("/tv/") || doc.select(".episodes").isNotEmpty()) TvType.TvSeries else TvType.Movie

            if (type == TvType.TvSeries) {
                // Load episodes from the page
                val episodes = mutableListOf<Episode>()
                doc.select(".episode, .ep-item").forEachIndexed { index, ep ->
                    val epTitle = ep.select(".ep-title").text() ?: "Episode ${index + 1}"
                    val epUrl = ep.select("a").attr("href")?.let { fixUrl(it) } ?: url
                    episodes.add(
                        Episode(
                            name = epTitle,
                            data = epUrl,
                            episode = index + 1,
                            season = 1  // Assume single season or parse if available
                        )
                    )
                }
                TvSeriesLoadResponse(
                    name = title,
                    url = url,
                    apiName = this.name,
                    type = type,
                    episodes = episodes,
                    posterUrl = posterUrl,
                    plot = description
                )
            } else {
                MovieLoadResponse(
                    name = title,
                    url = url,
                    apiName = this.name,
                    type = type,
                    dataUrl = url,
                    posterUrl = posterUrl,
                    plot = description
                )
            }
        } catch (e: Exception) {
            throw Exception("Failed to load content: ${e.message}")
        }
    }

    /**
     * Extracts video URLs from the loaded data.
     */
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return try {
            val doc = Jsoup.connect(data).get()
            // Look for iframe sources (common on vidbox.cc)
            doc.select("iframe").forEach { iframe ->
                val videoUrl = iframe.attr("src")?.let { fixUrl(it) }
                if (videoUrl != null && videoUrl.contains("vid")) {  // Filter for video sources
                    callback(
                        ExtractorLink(
                            source = this.name,
                            name = "${this.name} - ${iframe.attr("title") ?: "Stream"}",
                            url = videoUrl,
                            referer = mainUrl,
                            quality = Qualities.Unknown.value  // Adjust based on site (e.g., Qualities.HD.value)
                        )
                    )
                }
            }
            // If no iframes, check for direct video tags or scripts (extend as needed)
            true
        } catch (e: Exception) {
            println("Error loading links: ${e.message}")
            false
        }
    }

    // Helper: Scrape items (movies/TV) from a URL
    private suspend fun scrapeItems(url: String): List<SearchResponse> {
        val results = mutableListOf<SearchResponse>()
        try {
            val doc = Jsoup.connect(url).get()
            doc.select(".item, .movie-item, .tv-item").forEach { element ->
                val title = element.select(".title, h3").text() ?: "Unknown"
                val itemUrl = element.select("a").attr("href")?.let { fixUrl(it) } ?: ""
                val posterUrl = element.select("img").attr("src")?.let { fixUrl(it) }
                val type = if (itemUrl.contains("/tv/")) TvType.TvSeries else TvType.Movie
                if (itemUrl.isNotEmpty()) {
                    results.add(
                        SearchResponse(
                            name = title,
                            url = itemUrl,
                            apiName = this.name,
                            type = type,
                            posterUrl = posterUrl
                        )
                    )
                }
            }
        } catch (e: Exception) {
            println("Error scraping items from $url: ${e.message}")
        }
        return results
    }

    // Helper: Fix relative URLs to absolute
    private fun fixUrl(url: String): String {
        return if (url.startsWith("http")) url else "$mainUrl$url"
    }
}

