plugins {
    kotlin("jvm") version libs.versions.kotlin.core
    kotlin("plugin.serialization") version libs.versions.kotlin.core
    alias(libs.plugins.detekt)
    alias(libs.plugins.flyway)
    application
}

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        classpath(libs.postgresql)
        classpath(libs.flyway.postgresql)
    }
}

kotlin {
    jvmToolchain(21)
}

application {
    applicationName = "app"
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
    implementation(libs.hikari)
    implementation(libs.jdbi.core)
    implementation(libs.postgresql)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.nimbus.oauth2.sdk)
    implementation(libs.nimbus.jose.jwt)
    testImplementation(libs.bundles.testing)
    testImplementation(libs.bundles.http4k.testing)
}

flyway {
    baselineOnMigrate = true
    baselineVersion = "0"
    url = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/decision_matrix"
    user = System.getenv("DB_USER") ?: "decision_matrix"
    password = System.getenv("DB_PASSWORD") ?: "decision_matrix_password"
}
