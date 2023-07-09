import com.github.rholder.gradle.task.OneJar

plugins {
    kotlin("jvm") version "1.8.21"
    id("com.github.onslip.gradle-one-jar") version "1.1.0"
    application
}

group = "xyz.vladm"
version = "1.0-SNAPSHOT"

buildscript {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("gradle.plugin.com.github.onslip:gradle-one-jar:1.1.0")
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
}

apply(plugin = "com.github.onslip.gradle-one-jar")
class Versions(val akkaVersion: String, val scalaBinary: String)
val versions = Versions("2.7.0", "2.13")

dependencies {
    implementation(platform("com.typesafe.akka:akka-http-bom_${versions.scalaBinary}:10.5.2"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.+")
    implementation("com.typesafe.akka:akka-actor-typed_${versions.scalaBinary}:${versions.akkaVersion}")
    implementation("com.typesafe.akka:akka-stream_${versions.scalaBinary}:${versions.akkaVersion}")
    implementation("com.typesafe.akka:akka-http_${versions.scalaBinary}")
    implementation("com.typesafe.akka:akka-http-jackson_${versions.scalaBinary}")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}

tasks.register("oneJar", OneJar::class) {
    mainClass = "xyz.vladm.akka_mock_server.MainKt"
    archiveName = "akka-mock-oneJar.jar"
}