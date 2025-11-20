plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm") version "1.8.0"
}

group = "com.hcgn2005ai.vidbox"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.lagradost:cloudstream3:pre-release")
    implementation("org.jsoup:jsoup:1.17.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    
    jar {
        archiveFileName.set("hcgn2005-ai-vidbox-plugin.jar")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        
        from(sourceSets.main.get().output)
        
        // Include dependencies in the JAR
        from({
            configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
        })
        
        // Remove signature files to avoid conflicts
        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    }
}
