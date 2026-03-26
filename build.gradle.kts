plugins {
    kotlin("jvm") version "1.8.22" apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        mavenLocal()
    }

    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(17)
    }
}

tasks.register("deploy") {
    group = "blink"
    description = "Publish blink-common and blink-gradle-plugin to Maven repository"
    dependsOn(":blink-common:publish", ":blink-gradle-plugin:publish")
}
