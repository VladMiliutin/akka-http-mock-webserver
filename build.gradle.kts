plugins {
    kotlin("jvm") version "1.8.21"
    application
}

group = "xyz.vladm"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
class Versions(val AkkaVersion: String, val ScalaBinary: String)
val versions = Versions("2.7.0", "2.13")

dependencies {
    implementation(platform("com.typesafe.akka:akka-http-bom_${versions.ScalaBinary}:10.5.2"))

    implementation("com.typesafe.akka:akka-actor-typed_${versions.ScalaBinary}:${versions.AkkaVersion}")
    implementation("com.typesafe.akka:akka-stream_${versions.ScalaBinary}:${versions.AkkaVersion}")
    implementation("com.typesafe.akka:akka-http_${versions.ScalaBinary}")

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