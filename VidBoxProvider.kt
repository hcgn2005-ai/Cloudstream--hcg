package com.hcgn2005ai.vidbox.plugin

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup

class VidBoxProvider : MainAPI() {
    override var mainUrl = "https://vidbox.cc"
    override var name = "VidBox by hcgn2005-ai"
    
    override val supportedTypes = setOf(TvType.Movie)
    
    override suspend fun getMainPage(): HomePageResponse {
        val items = listOf(HomePageList("Test Section", listOf()))
        return HomePageResponse(items)
    }
    
    override suspend fun search(query: String): List<SearchResponse> {
        return try {
            val document = app.get("$mainUrl/search?q=${query.encodeURL()}").document
            document.select(".movie-item, .item, [class*='movie']").mapNotNull { element ->
                val title = element.select(".title, h3, h4").text()
                val href = element.select("a").attr("href")
                val poster = element.select("img").attr("src")
                
                if (title.isNotEmpty() && href.isNotEmpty()) {
                    MovieSearchResponse(
                        title,
                        fixUrl(href, mainUrl),
                        name,
                        TvType.Movie,
                        fixUrl(poster, mainUrl)
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun load(url: String): LoadResponse? {
        return try {
            val document = app.get(url).document
            val title = document.select("h1").text()
            val poster = document.select(".poster img").attr("src")
            
            MovieLoadResponse(
                title,
                url,
                name,
                TvType.Movie,
                fixUrl(poster, mainUrl),
                null,
                null
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun fixUrl(url: String, baseUrl: String): String {
        return when {
            url.isEmpty() -> ""
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            url.startsWith("/") -> "$baseUrl$url"
            else -> "$baseUrl/$url"
        }
    }
}
