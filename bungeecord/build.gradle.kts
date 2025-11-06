import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
}

applyPlatformAndCoreConfiguration()
applyCommonArtifactoryConfig()
applyShadowConfiguration()

repositories {
    mavenCentral()
    maven {
        name = "bungeecord"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    maven {
        url = uri("https://libraries.minecraft.net/")
    }
}

configurations {
    compileClasspath.get().extendsFrom(create("shadeOnly"))
}

dependencies {
    "compileOnly"("net.md-5:bungeecord-api:1.21-R0.4")
    "api"(project(":nuvotifier-api"))
    "api"(project(":nuvotifier-common"))
}


tasks.named<Copy>("processResources") {
    val internalVersion = project.ext["internalVersion"]
    inputs.property("internalVersion", internalVersion)
    filesMatching("bungee.yml") {
        expand("internalVersion" to internalVersion)
    }
}

tasks.named<Jar>("jar") {
    val projectVersion = project.version
    inputs.property("projectVersion", projectVersion)
    manifest {
        attributes("Implementation-Version" to projectVersion)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(project.configurations["shadeOnly"], project.configurations["runtimeClasspath"])

    dependencies {
        include(dependency(":nuvotifier-api"))
        include(dependency(":nuvotifier-common"))
    }
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}
