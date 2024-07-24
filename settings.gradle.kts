pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
        }
        maven("https://maven.aliyun.com/repository/public")
    }
}
rootProject.name = "PixelWorldPro"
