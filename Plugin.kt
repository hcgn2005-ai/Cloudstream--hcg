package com.mystream.plugin

import com.lagradost.cloudstream3.MainPlugin
import com.lagradost.cloudstream3.plugins.Plugin

/**
 * Main plugin class for MyStreamPlugin.
 * This plugin provides streaming capabilities for movies and TV shows.
 */
@Plugin
class MainPlugin : MainPlugin() {
    override val name: String = "MyStreamPlugin"
    override val version: String = "1.0.0"
    override val description: String = "Streams movies and TV shows from MyStream API."

    override fun registerProviders() {
        // Register the main provider
        registerProvider(MainProvider())
    }

    override fun onPluginLoaded() {
        // Optional: Initialization logic, e.g., logging or setup
        println("$name plugin loaded successfully.")
    }
}
