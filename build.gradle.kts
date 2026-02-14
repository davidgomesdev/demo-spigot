plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    jacoco
}

group = "me.davidgomes"
version = "1.0-SNAPSHOT"

val paperVersion = "1.21.11-R0.1-SNAPSHOT"
val slf4jVersion = "2.0.16"

val junitVersion = "5.10.2"
val mockBukkitVersion = "4.101.0"
val mockkVersion = "1.14.9"

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
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.papermc.paper:paper-api:$paperVersion")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:$mockBukkitVersion")

    testRuntimeOnly("org.slf4j:slf4j-simple:$slf4jVersion")
}

tasks.test {
    useJUnitPlatform()
    // Paper uses byte buddy for its event system, which relies on dynamic class loading.
    // This is disabled by default in Java 21, so we need to re-enable it for the tests to work.
    jvmArgs("-XX:+EnableDynamicAgentLoading")

    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
        csv.required = false
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = BigDecimal.valueOf(0.7)
            }
        }
    }
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
