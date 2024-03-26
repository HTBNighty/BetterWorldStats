plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-paper") version "2.2.2"
}

repositories {
    mavenCentral()

    maven("https://papermc.io/repo/repository/maven-public/") {
        name = "papermc"
    }

    maven("https://ci.pluginwiki.us/plugin/repository/everything/") {
        name = "configmaster-repo"
    }

    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
        name = "placeholderapi"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    implementation("org.bstats:bstats-bukkit:3.0.2")
    implementation("me.clip:placeholderapi:2.11.5")

    implementation("net.kyori:adventure-platform-bukkit:4.3.2")
    implementation("net.kyori:adventure-text-minimessage:4.16.0")
    implementation("net.kyori:adventure-text-serializer-ansi:4.16.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.16.0")
    implementation("net.kyori:adventure-text-serializer-plain:4.16.0")
    implementation("net.kyori:adventure-text-logger-slf4j:4.16.0")

    implementation("com.github.thatsmusic99:ConfigurationMaster-API:v2.0.0-rc.1")
    implementation("com.github.ben-manes.caffeine:caffeine:2.9.3")
    implementation("com.google.auto.service:auto-service:1.1.1")
}

group = "me.xginko.betterworldstats"
version = "1.10.0"
description = "Show stats about server age, map size and unique player joins on your minecraft server."
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_1_8

runPaper.folia.registerTask();

tasks {
    runServer {
        minecraftVersion("1.20.4")
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    javadoc {
        options.encoding = "UTF-8"
    }

    processResources {
        filesMatching("plugin.yml") {
            expand(
                mapOf(
                    "name" to project.name,
                    "version" to project.version,
                    "description" to project.description!!.replace('"'.toString(), "\\\""),
                    "url" to "https://github.com/xGinko/BetterWorldStats"
                )
            )
        }
    }

    shadowJar {
        archiveFileName.set("BetterWorldStats-${version}.jar")
        relocate("net.kyori", "me.xginko.betterworldstats.libs.kyori")
        relocate("org.bstats", "me.xginko.betterworldstats.libs.bstats")
        relocate("io.github.thatsmusic99.configurationmaster", "me.xginko.betterworldstats.libs.configmaster")
        relocate("com.github.benmanes.caffeine", "me.xginko.betterworldstats.libs.caffeine")
    }
}