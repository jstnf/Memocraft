plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "work.aemnet"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Resolves Paper's mojang-mapped server + dependencies for NMS access.
    paperweight.paperDevBundle("26.1.2.build.65-stable")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.runServer {
    minecraftVersion("26.1.2")
}
