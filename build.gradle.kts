plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "me.davidgomes"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
}
