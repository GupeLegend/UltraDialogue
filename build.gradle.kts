import java.net.URI

plugins {
    id("java")
}

group = "mc.gupe"
version = "1.1.3"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

// BeautyQuests and PlaceholderAPI are only needed to compile the optional hooks (mc.gupe.ultradialogue.hook):
// they are never on Maven Central under those coordinates for this Paper version, so they are fetched
// straight from their own download URLs into libs/, exactly like compilar.sh does.
val libs = layout.projectDirectory.dir("libs")
val optionalLibs = mapOf(
    "beautyquests-2.1.0-b144.jar" to "https://cdn.modrinth.com/data/ZbvwkXKZ/versions/JKDAEeVt/beautyquests-2.1.0%2Bbuild.144.jar",
    "placeholderapi.jar" to "https://repo.extendedclip.com/releases/me/clip/placeholderapi/2.11.7/placeholderapi-2.11.7.jar"
)

val downloadOptionalLibs = tasks.register("downloadOptionalLibs") {
    doLast {
        libs.asFile.mkdirs()
        for ((name, url) in optionalLibs) {
            val file = libs.file(name).asFile
            if (!file.exists()) {
                logger.lifecycle("Downloading $name...")
                URI(url).toURL().openStream().use { input -> file.outputStream().use { input.copyTo(it) } }
            }
        }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.74-stable")
    compileOnly(files(libs.file("beautyquests-2.1.0-b144.jar")))
    compileOnly(files(libs.file("placeholderapi.jar")))
}

tasks.compileJava {
    dependsOn(downloadOptionalLibs)
    options.encoding = "UTF-8"
    options.release = 25
}

tasks.processResources {
    filteringCharset = "UTF-8"
}

tasks.jar {
    archiveBaseName.set("UltraDialogue")
    archiveVersion.set(version.toString())
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
}
