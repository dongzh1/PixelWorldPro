import org.gradle.jvm.tasks.Jar
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter

plugins {
    java
    id("com.github.johnrengelman.shadow") version ("7.1.2")
    kotlin("jvm") version "1.8.0"
    `maven-publish`
}

group = "com.dongzh1.pixelworldpro"
version = "1.3.11"

repositories {
    maven("https://repo.william278.net/releases/")
    maven("https://maven.aliyun.com/nexus/content/groups/public/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.wdsj.io/repository/minecraft/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io/")
    maven("https://repo.olziedev.com/")
    mavenCentral()
}

dependencies {
    //implementation("com.xbaimiao.ktor:ktor-plugins-bukkit:1.0.7")
    implementation(kotlin("stdlib-jdk8"))
    compileOnly("com.ghostchu:quickshop-api:5.2.0.5")
    compileOnly("com.zaxxer:HikariCP:4.0.3")
    compileOnly("com.j256.ormlite:ormlite-core:6.1")
    compileOnly("com.j256.ormlite:ormlite-jdbc:6.1")
    compileOnly("redis.clients:jedis:3.7.0")
    compileOnly("com.google.code.gson:gson:2.10")
    compileOnly("org.bouncycastle:bcprov-lts8on:2.73.3")
    implementation(fileTree("shadowLibs"))
    compileOnly(fileTree("libs"))
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-20211218.082619-371")
    compileOnly("com.googlecode.json-simple:json-simple:1.1")
    compileOnly("com.github.LoneDev6:API-ItemsAdder:3.6.1")
    compileOnly("net.william278.huskhomes:huskhomes-bukkit:4.7")
    compileOnly("com.olziedev:playerwarps-api:7.6.1")
}

fun releaseTime() = LocalDate.now().format(DateTimeFormatter.ofPattern("y.M.d"))

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
    shadowJar {
        arrayListOf(
            "kotlin=kotlin"
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
        archiveClassifier.set("")
    }
    processResources {
        val props = ArrayList<Pair<String, Any>>()
        props.add("version" to "$version")
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
            url = URI("https://maven.mcyzj.cn:445/repository/maven-private/")
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
