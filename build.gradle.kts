import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    `java-library`
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("de.eldoria.plugin-yml.paper") version "0.7.1"
}

group = "eu.byquanton.plugins"
version = "1.1.3"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper","paper-api","1.20.1-R0.1-SNAPSHOT")
    paperLibrary("org.incendo", "cloud-paper", "2.0.0-beta.10")
    paperLibrary("com.fasterxml.jackson.core", "jackson-databind", "2.18.2")
    paperLibrary("com.fasterxml.jackson.core", "jackson-annotations", "2.18.2")
    paperLibrary("com.fasterxml.jackson.core", "jackson-core", "2.18.2")
}

paper {
    main = "eu.byquanton.plugins.twitch_link.TwitchLinkPlugin"
    loader = "eu.byquanton.plugins.twitch_link.TwitchLinkPluginLoader"
    generateLibrariesJson = true
    foliaSupported = true
    apiVersion = "1.20"
    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    authors = listOf("byquanton")
}


tasks {
    runServer{
        minecraftVersion("1.21.8")
        downloadPlugins {
            modrinth("viaversion", "5.4.1")
            modrinth("viabackwards", "5.4.1")
            modrinth("luckperms", "v5.5.0-bukkit")
        }
    }
}