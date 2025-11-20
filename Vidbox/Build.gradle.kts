plugins {
    kotlin("jvm") version "1.8.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"  // For fat JAR
}

group = "com.mystream.plugin"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")  // For CloudStream dependencies
}

dependencies {
    // CloudStream API
    implementation("com.github.recloudstream:cloudstream:3.4.0")  // Update to latest version

    // HTML parsing (for scraping)
    implementation("org.jsoup:jsoup:1.15.3")

    // Optional: For HTTP requests
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
}

tasks {
    shadowJar {
        archiveBaseName.set("MyStreamPlugin")
        archiveClassifier.set("")
        archiveVersion.set("")
    }
}

kotlin {
    jvmToolchain(11)  // Use Java 11+
}
