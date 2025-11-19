package com.lagradost.cloudstream3.providers

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class VidboxProvider : MainAPI() {
    override var mainUrl = "https://vidbox.cc"
    override var name = "Vidbox"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)  // Adjust if it's mostly one type

    // Helper to extract video items from a page
    private fun extractVideos(document: org.jsoup.nodes.Document): List<SearchResponse> {
        return document.select("div.video-item, .thumb, .video-thumb").mapNotNull { element ->
            val title = element.select("h3, .title, a").text().trim().ifEmpty { null } ?: return@mapNotNull null
            val url = fixUrl(element.select("a").attr("href"))
            val poster = fixUrlNull(element.select("img").attr("src"))
            MovieSearchResponse(title, url, this.name, TvType.Movie, poster, null)
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/page/$page").document  // Assumes pagination like /page/1
        val items = extractVideos(document)
        return HomePageResponse(listOf(HomePageList("Featured Videos", items)), hasNext = items.isNotEmpty())
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/search?q=${query.encodeURIComponent()}").document
        return extractVideos(document)
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.select("h1, .video-title").text().trim()
        val description = document.select(".description, p").text().trim().ifEmpty { null }
        val poster = fixUrlNull(document.select("img.poster, .video-thumb img").attr("src"))
        // If it's a series, parse episodes; for now, treat as single movie
        return MovieLoadResponse(title, url, this.name, TvType.Movie, url, poster, null, description, null, null)
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        // Extract iframe embeds (common for Vidbox)
        document.select("iframe").forEach { iframe ->
            val iframeSrc = iframe.attr("src")
            if (iframeSrc.isNotEmpty()) {
                // Use CloudStream's loadExtractor to handle common players (e.g., Vidbox's own or embedded like MP4Upload)
                loadExtractor(iframeSrc, subtitleCallback, callback)
            }
        }
        // Fallback: Direct video sources if present
        document.select("video source, source").forEach { source ->
            val videoUrl = source.attr("src")
            if (videoUrl.isNotEmpty()) {
                callback(ExtractorLink(this.name, "Direct Stream", videoUrl, "", Qualities.Unknown.value, false))
            }
        }
        return true
    }
}
