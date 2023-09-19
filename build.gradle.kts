import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.1.3"
    id("io.spring.dependency-management") version "1.1.3"
    kotlin("jvm") version "1.8.22"
    kotlin("plugin.spring") version "1.8.22"
    kotlin("kapt") version "1.9.10"
    // I'll re-enable this when I get around to creating a custom config for it. For now, I'm just using IntelliJ's
    // built in format-on-save.
    // id("org.jlleitschuh.gradle.ktlint") version "11.5.1"
    application
}

application {
    mainClass.set("com.brydonleonard.gatekey.GateKeyApplicationKt")
    applicationName = "GateKey"
}

group = "com.brydonleonard"
version = "0.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.twilio.sdk:twilio:8.8.0")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.1.0")
    implementation("org.xerial:sqlite-jdbc")
    implementation("com.twilio.sdk:twilio:9.12.0")
    implementation(kotlin("reflect"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
