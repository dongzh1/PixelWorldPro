import org.gradle.jvm.tasks.Jar
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter

plugins {
    java
    id("com.github.johnrengelman.shadow") version ("7.1.2")
    kotlin("jvm") version "1.8.0"
    id("org.jetbrains.kotlin.plugin.lombok") version "1.8.0"
    `maven-publish`
}

group = "com.dongzh1.pixelworldpro"
version = "1.0.0"

repositories {
    mavenCentral()
    maven ("https://repo.papermc.io/repository/maven-public/")
    maven ("https://oss.sonatype.org/content/groups/public/")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://maven.xbaimiao.com/repository/releases/")
    maven {
        url = uri("https://maven.xbaimiao.com/repository/maven-private/")
        credentials {
            username = project.findProperty("BaiUser").toString()
            password = project.findProperty("BaiPassword").toString()
        }
    }
    mavenLocal()
}

dependencies {
    compileOnly("net.luckperms:api:5.4")
    compileOnly ("com.destroystokyo.paper:paper-api:1.14.1-R0.1-SNAPSHOT")
    implementation("com.xbaimiao:EasyLib:2.1.6")
    implementation(kotlin("stdlib-jdk8"))
//    implementation ("de.tr7zw:item-nbt-api:2.11.2")
    implementation ("com.j256.ormlite:ormlite-core:6.1")
    implementation ("com.j256.ormlite:ormlite-jdbc:6.1")
    implementation ("com.zaxxer:HikariCP:4.0.3")
    implementation ("redis.clients:jedis:3.7.0")
    implementation ("com.google.code.gson:gson:2.10")
    compileOnly(fileTree("libs"))
    compileOnly(dependencyNotation = "org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")

//    compileOnly ("com.mojang:authlib:1.5.21")
}

fun releaseTime() = LocalDate.now().format(DateTimeFormatter.ofPattern("y.M.d"))

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
    shadowJar {
        arrayListOf(
            "io.papermc.lib=papermc.lib",
            "com.cryptomorin.xseries=xseries",
            "com.zaxxer.hikari=hikari",
            "com.xbaimiao.easylib=easylib",
            "com.j256.ormlite=ormlite",
            "de.tr7zw=nbtapi",
            "de.tr7zw.changeme.nbtapi=nbtapi",
            "kotlin=kotlin",
            "org.jetbrains.annotations=jetbrains"
        ).forEach {
            val args = it.split("=")
            relocate(args[0], "${project.group}.shadow.${args[1]}")
        }
        dependencies {
            exclude(dependency("org.slf4j:"))
            exclude(dependency("com.google.code.gson:gson:"))
        }
        exclude("LICENSE")
        exclude("META-INF/*.SF")
        minimize()
        archiveClassifier.set("")
    }
    processResources {
        val props = ArrayList<Pair<String, Any>>()
        props.add("version" to "${releaseTime()}-$version")
        props.add("main" to "${project.group}.${project.name}")
        props.add("name" to project.name)
        expand(*props.toTypedArray())
    }
    artifacts {
        archives(shadowJar)
    }
}

tasks.register("sourcesJar", Jar::class.java) {
    this.group = "build"
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

publishing {
    repositories {
        maven {
            credentials {
                username = project.findProperty("BaiUser").toString()
                password = project.findProperty("BaiPassword").toString()
            }
            url = URI("https://maven.xbaimiao.com/repository/maven-private/")
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = "${project.version}"
            from(components["java"])
            artifact(tasks["sourcesJar"])
        }
    }
}
