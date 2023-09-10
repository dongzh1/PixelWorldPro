import org.gradle.jvm.tasks.Jar
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter

plugins {
    java
    id("com.github.johnrengelman.shadow") version ("7.1.2")
    kotlin("jvm") version "1.8.0"
    id("com.xbaimiao.easylib") version ("1.1.0")
    `maven-publish`
}

group = "com.dongzh1.pixelworldpro"
version = "1.2.0"

easylib {
    version = "2.3.9"
    nbt = false
    hikariCP = true
    ormlite = true
    userMinecraftLib = false
    minecraftVersion = "1.12.2"
    isPaper = false
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/groups/public/")
}

dependencies {
    implementation("com.xbaimiao.ktor:ktor-plugins-bukkit:1.0.6")
    implementation(kotlin("stdlib-jdk8"))
    implementation("redis.clients:jedis:3.7.0")
    implementation("com.google.code.gson:gson:2.10")
    implementation("org.bouncycastle:bcprov-lts8on:2.73.3")
    implementation("org.json:json:20230227")
    compileOnly(fileTree("libs"))
    compileOnly("public:paper:1.16.5")
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
            "org.jetbrains.annotations=jetbrains",
            "com.google.code.gson=gson",
            "redis.clients.jedis=jedis",
            "org.json=json",
            "org.bouncycastle=bouncycastle",
            "org.apache.commons=common"
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
