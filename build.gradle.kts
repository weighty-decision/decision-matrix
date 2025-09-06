plugins {
    kotlin("jvm") version libs.versions.kotlin.core
    kotlin("plugin.serialization") version libs.versions.kotlin.core
    alias(libs.plugins.detekt)
    application
}

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
    }
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "decisionmatrix.AppKt"
}

repositories {
    mavenCentral()
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed", "standardOut", "standardError")
        }
    }
}

dependencies {

    implementation(platform(libs.http4k.bom))
    implementation(libs.bundles.http4k)
    implementation(libs.bundles.logging)
    implementation(libs.kotlinx.html)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jdbi.core)
    implementation(libs.sqlite.jdbc)
    implementation(libs.nimbus.oauth2.sdk)
    implementation(libs.nimbus.jose.jwt)
    testImplementation(libs.bundles.testing)
    testImplementation(libs.bundles.http4k.testing)
}
