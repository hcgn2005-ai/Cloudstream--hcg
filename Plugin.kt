package com.hcg2005ai.vidbox.plugin

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

/**
 * Main plugin class for hcg2005-ai VidBox Plugin.
 * This plugin provides streaming capabilities for movies and TV shows from vidbox.cc.
 * Created by: hcg2005-ai
 */
@CloudstreamPlugin
class Hcg2005AiVidBoxPlugin: Plugin() {
    override fun load(context: Context) {
        // Register all providers
        registerMainAPI(Hcg2005AiVidBoxProvider())
    }
}
