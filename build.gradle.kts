plugins {
    kotlin("jvm") version "1.8.21"
    application
}

group = "xyz.vladm"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
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
    jvmToolchain(11)
}

application {
    mainClass.set("MainKt")
}