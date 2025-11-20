package com.hcgn2005ai.vidbox.plugin

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup

class VidBoxProvider : MainAPI() {
    override var mainUrl = "https://vidbox.cc"
    override var name = "VidBox by hcgn2005-ai"
    
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )
    
    override val hasMainPage = true
    override val hasQuickSearch = true

    // Main page with featured content
    override suspend fun getMainPage(): HomePageResponse {
        val items = ArrayList<HomePageList>()
        
        try {
            val document = app.get(mainUrl).document
            
            // Featured Movies
            val featured = document.select(".movie-item, .item, [class*='movie']").mapNotNull { element ->
                parseSearchResult(element)
            }
            
            if (featured.isNotEmpty()) {
                items.add(HomePageList("Featured Movies", featured))
            }
            
            // Latest Movies
            val latest = document.select(".latest-movies .item, .new-releases .item").mapNotNull { element ->
                parseSearchResult(element)
            }
            
            if (latest.isNotEmpty()) {
                items.add(HomePageList("Latest Movies", latest))
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return HomePageResponse(items)
    }

    // Search functionality
    override suspend fun search(query: String): List<SearchResponse> {
        return try {
            val searchUrl = "$mainUrl/search?q=${query.encodeURL()}"
            val document = app.get(searchUrl).document
            
            document.select(".movie-item, .search-result, .item").mapNotNull { element ->
                parseSearchResult(element)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Quick search
    override suspend fun quickSearch(query: String): List<SearchResponse> {
        return search(query)
    }

    // Load content details
    override suspend fun load(url: String): LoadResponse? {
        return try {
            val document = app.get(url).document
            
            val title = document.select("h1, .title").first()?.text() ?: "Unknown Title"
            val poster = document.select(".poster img, .cover img").attr("src")
            val description = document.select(".description, .synopsis, .plot").text()
            
            // Check if it's a series by looking for episodes
            val episodeElements = document.select(".episode-list a, .episode-item")
            val episodes = ArrayList<Episode>()
            
            episodeElements.forEachIndexed { index, episodeElement ->
                val episodeUrl = episodeElement.attr("href")
                val episodeName = episodeElement.text()
                
                if (episodeUrl.isNotEmpty()) {
                    episodes.add(
                        Episode(
                            data = fixUrl(episodeUrl),
                            name = episodeName.ifEmpty { "Episode ${index + 1}" },
                            episode = index + 1
                        )
                    )
                }
            }
            
            if (episodes.isNotEmpty()) {
                // It's a TV series
                TvSeriesLoadResponse(
                    name = title,
                    url = url,
                    apiName = this.name,
                    type = TvType.TvSeries,
                    posterUrl = fixUrl(poster),
                    plot = description,
                    episodes = episodes
                )
            } else {
                // It's a movie
                MovieLoadResponse(
                    name = title,
                    url = url,
                    apiName = this.name,
                    type = TvType.Movie,
                    posterUrl = fixUrl(poster),
                    plot = description
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Load video URLs
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return try {
            // If data is already a video URL
            if (data.contains(".mp4") || data.contains(".m3u8")) {
                callback(
                    ExtractorLink(
                        name = this.name,
                        source = "VidBox",
                        url = data,
                        quality = Qualities.Unknown.value,
                        isM3u8 = data.contains(".m3u8")
                    )
                )
                return true
            }
            
            // Otherwise extract from page
            val response = app.get(data).text
            
            // Look for video URLs in various patterns
            val videoPatterns = listOf(
                Regex("""file:\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
                Regex("""src:\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
                Regex("""<source\s+src=["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
                Regex("""videoUrl\s*=\s*["']([^"']+)["']""")
            )
            
            var foundLinks = false
            videoPatterns.forEach { pattern ->
                val matches = pattern.findAll(response)
                matches.forEach { match ->
                    val videoUrl = match.groupValues[1]
                    if (videoUrl.isNotEmpty()) {
                        val fullUrl = fixUrl(videoUrl)
                        callback(
                            ExtractorLink(
                                name = this.name,
                                source = "VidBox",
                                url = fullUrl,
                                quality = Qualities.Unknown.value,
                                isM3u8 = fullUrl.contains(".m3u8")
                            )
                        )
                        foundLinks = true
                    }
                }
            }
            
            foundLinks
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Helper function to parse search results
    private fun parseSearchResult(element: org.jsoup.nodes.Element): SearchResponse? {
        return try {
            val titleElement = element.select(".title, h3, h4, [class*='title']").first()
            val linkElement = element.select("a").first()
            val imageElement = element.select("img").first()
            
            val title = titleElement?.text() ?: "Unknown Title"
            var href = linkElement?.attr("href") ?: ""
            val poster = imageElement?.attr("src") ?: imageElement?.attr("data-src") ?: ""
            
            if (title.isNotEmpty() && href.isNotEmpty()) {
                MovieSearchResponse(
                    name = title,
                    url = fixUrl(href),
                    apiName = this.name,
                    type = TvType.Movie,
                    posterUrl = fixUrl(poster)
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Helper function to fix URLs
    private fun fixUrl(url: String): String {
        return when {
            url.isEmpty() -> ""
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> "$mainUrl$url"
            else -> "$mainUrl/$url"
        }
    }
}
