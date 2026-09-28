plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "io.github.glocation87"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.129-stable")
    // Downloaded at runtime by NatureLoader, so it isn't bundled into the jar
    compileOnly("org.spongepowered:configurate-yaml:4.2.0")

    testImplementation("io.papermc.paper:paper-api:26.2.build.129-stable")
    testImplementation("org.spongepowered:configurate-yaml:4.2.0")
    // A fake server for tests, so Player, worlds and scheduling work without running Minecraft
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.2:4.116.1")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 25
        // Report every warning category, "processing" is noise from annotation processors we don't use
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    runServer {
        minecraftVersion("26.2")
        // EULA already accepted for the local test server
        jvmArgs("-Dcom.mojang.eula.agree=true")
        // 25565 is taken by the shared test-server
        args("--port", "25566")
    }

    test {
        useJUnitPlatform()
        // MockBukkit's ByteBuddy still uses sun.misc.Unsafe, silence the JDK 25 warning so real problems stand out
        jvmArgs("--sun-misc-unsafe-memory-access=allow")
    }
}
