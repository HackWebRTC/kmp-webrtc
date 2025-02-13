pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "kmp-webrtc"
include(":kmp-webrtc")

//if (System.getProperty("os.name") == "Mac OS X") {
//    include(":example:androidApp")
//}
