package com.hcgn2005ai.vidbox.plugin

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class VidBoxPlugin: Plugin() {
    override fun load(context: Context) {
        // Register the main provider
        registerMainAPI(VidBoxProvider())
    }
}
