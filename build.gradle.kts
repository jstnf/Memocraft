plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "work.aemnet"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Resolves Paper's mojang-mapped server + dependencies for NMS access.
    paperweight.paperDevBundle("26.1.2.build.65-stable")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.1.2:4.113.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.xerial:sqlite-jdbc:3.49.1.0")
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

// paperweight adds mappedServerJar.jar to the test classpath for NMS access.
// That jar also registers META-INF/services for ServerBuildInfo, RegistryAccess, etc.
// MockBukkit ships its own implementations of those same services.
// adventure Services.service() throws when it finds more than one provider.
// Fix: produce a stripped copy of the server jar (META-INF/services removed) that
// is used only at test runtime, so NMS classes remain available but only
// MockBukkit's service providers are visible.
val strippedServerJar by tasks.registering(Zip::class) {
    dependsOn(tasks.named("paperweightUserdevSetup"))
    from(
        configurations.compileClasspath.get()
            .filter { it.name == "mappedServerJar.jar" }
            .map { zipTree(it) }
    ) {
        exclude("META-INF/services/**")
    }
    archiveFileName.set("mappedServerJar-stripped.jar")
    destinationDirectory.set(layout.buildDirectory.dir("tmp/strippedServerJar"))
}

tasks.test {
    dependsOn(strippedServerJar)
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    // Replace the original server jar with the stripped copy in the test classpath.
    classpath = files(strippedServerJar.get().outputs.files) +
            classpath.filter { it.name != "mappedServerJar.jar" }
}
