plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "me.davidgomes"
version = "1.0-SNAPSHOT"

val paperVersion = "1.21.11-R0.1-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    maven("https://oss.sonatype.org/content/repositories/snapshots")

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveVersion = ""
}

kotlin {
    jvmToolchain(21)
}

tasks.register<Copy>("deployPlugin") {
    group = "deploy"
    description = "Copies to the plugins folder of the server"

    dependsOn(tasks.shadowJar)

    from(layout.buildDirectory.file("libs/demo-all.jar"))
    into(System.getenv("SERVER_PLUGINS_DIRECTORY"))
}
