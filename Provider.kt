package com.hcg2005ai.vidbox.plugin

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup

/**
 * VidBox Provider for CloudStream
 * Created by: hcg2005-ai
 * Source: https://vidbox.cc/
 */
class Hcg2005AiVidBoxProvider : MainAPI() {
    override var mainUrl = "https://vidbox.cc"
    override var name = "hcg2005-ai VidBox"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val hasChromecastSupport = true
    
    override var lang = "en"
    
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    // Main page with featured content
    override suspend fun getMainPage(): HomePageResponse {
        val items = ArrayList<HomePageList>()
        
        try {
            // Try to get featured content
            val document = app.get(mainUrl).document
            
            // Featured section - adjust selectors based on actual website
            val featuredElements = document.select(".featured-movies .movie-item, .slider-item, .featured-item")
            val featuredList = featuredElements.mapNotNull { element ->
                parseSearchResult(element)
            }
            
            if (featuredList.isNotEmpty()) {
                items.add(HomePageList("Featured Content", featuredList))
            }
            
            // Latest movies section
            val latestElements = document.select(".latest-movies .movie-item, .new-releases .item, .movie-list .item")
            val latestList = latestElements.mapNotNull { element ->
                parseSearchResult(element)
            }
            
            if (latestList.isNotEmpty()) {
                items.add(HomePageList("Latest Releases", latestList))
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return HomePageResponse(items)
    }

    // Helper function to parse search results
    private fun parseSearchResult(element: org.jsoup.nodes.Element): SearchResponse? {
        return try {
            val titleElement = element.select(".title, h3, h4, [class*='title']").first()
            val linkElement = element.select("a").first()
            val imageElement = element.select("img").first()
            
            val title = titleElement?.text() ?: "Unknown Title"
            var href = linkElement?.attr("href") ?: ""
            val poster = imageElement?.attr("src") ?: ""
            
            // Ensure href is absolute URL
            if (href.isNotEmpty() && !href.startsWith("http")) {
                href = if (href.startsWith("/")) {
                    mainUrl + href
                } else {
                    "$mainUrl/$href"
                }
            }
            
            if (title.isNotEmpty() && href.isNotEmpty()) {
                MovieSearchResponse(
                    name = title,
                    url = href,
                    apiName = this.name,
                    type = TvType.Movie, // Default to movie, adjust in load() if needed
                    posterUrl = poster
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Search functionality
    override suspend fun search(query: String): List<SearchResponse> {
        val results = ArrayList<SearchResponse>()
        
        try {
            val searchUrl = "$mainUrl/search?q=${query.encodeURL()}"
            val document = app.get(searchUrl).document
            
            document.select(".movie-item, .search-result, .item, [class*='movie']").forEach { element ->
                parseSearchResult(element)?.let { results.add(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return results
    }

    // Quick search for suggestions
    override suspend fun quickSearch(query: String): List<SearchResponse> {
        return search(query)
    }

    // Load content details
    override suspend fun load(url: String): LoadResponse? {
        return try {
            val document = app.get(url).document
            
            val title = document.select("h1, .title, [class*='title']").first()?.text() ?: "Unknown Title"
            val poster = document.select(".poster img, .cover img, [class*='poster'] img").attr("src")
            val description = document.select(".description, .synopsis, .plot, [class*='desc']").text()
            
            // Try to extract year from various possible locations
            val yearText = document.select(".year, .release-date, [class*='year']").text()
            val year = Regex("\\d{4}").find(yearText)?.value?.toIntOrNull()
            
            // Check if it's a series by looking for episodes
            val episodeElements = document.select(".episode-list a, .episode-item, [class*='episode'] a")
            val episodes = ArrayList<Episode>()
            
            episodeElements.forEachIndexed { index, episodeElement ->
                val episodeUrl = episodeElement.attr("href")
                val episodeName = episodeElement.text()
                
                if (episodeUrl.isNotEmpty()) {
                    val fullEpisodeUrl = if (episodeUrl.startsWith("http")) episodeUrl else "$mainUrl$episodeUrl"
                    episodes.add(
                        Episode(
                            data = fullEpisodeUrl,
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
                    posterUrl = poster,
                    year = year,
                    plot = description,
                    episodes = episodes
                )
            } else {
                // It's a movie - look for direct video links
                val videoElements = document.select("video, [class*='video'] source, [data-video]")
                val movieLinks = ArrayList<Episode>()
                
                videoElements.forEachIndexed { index, videoElement ->
                    val videoUrl = videoElement.attr("src") ?: videoElement.attr("data-video")
                    if (videoUrl.isNotEmpty()) {
                        movieLinks.add(
                            Episode(
                                data = videoUrl,
                                name = "Play",
                                episode = 1
                            )
                        )
                    }
                }
                
                // If no direct video links found, use the detail page URL
                if (movieLinks.isEmpty()) {
                    movieLinks.add(
                        Episode(
                            data = url,
                            name = "Play Movie",
                            episode = 1
                        )
                    )
                }
                
                MovieLoadResponse(
                    name = title,
                    url = url,
                    apiName = this.name,
                    type = TvType.Movie,
                    posterUrl = poster,
                    year = year,
                    plot = description,
                    episodes = movieLinks
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
            // If data is already a direct video URL
            if (data.contains(".mp4") || data.contains(".m3u8")) {
                callback(
                    ExtractorLink(
                        name = this.name,
                        source = "hcg2005-ai VidBox",
                        url = data,
                        quality = Qualities.Unknown.value,
                        isM3u8 = data.contains(".m3u8")
                    )
                )
                return true
            }
            
            // Otherwise, extract from the page
            val response = app.get(data).text
            
            // Look for various video URL patterns
            val videoPatterns = listOf(
                Regex("""file:\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
                Regex("""src:\s*["']([^"']+\.(?:mp4|m3u8)[^"']*)["']"""),
                Regex("""video\s+src=["']([^"']+)["']"""),
                Regex("""<source\s+src=["']([^"']+\.(?:mp4|m3u8)[^"']*)["']""")
            )
            
            var foundLinks = false
            videoPatterns.forEach { pattern ->
                val matches = pattern.findAll(response)
                matches.forEach { match ->
                    val videoUrl = match.groupValues[1]
                    if (videoUrl.isNotEmpty() && (videoUrl.contains("http") || videoUrl.startsWith("//"))) {
                        val fullUrl = if (videoUrl.startsWith("//")) "https:$videoUrl" else videoUrl
                        callback(
                            ExtractorLink(
                                name = "hcg2005-ai VidBox",
                                source = this.name,
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
}
