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

if (System.getProperty("os.name") == "Mac OS X") {
    include(":example:androidApp")
//    include(":example:webApp")
}

include(":proto-client")
project(":proto-client").projectDir = File("../mediasoup/protoo-client-android/protoo-client")

include(":mediasoup-client")
project(":mediasoup-client").projectDir = File("../mediasoup/mediasoup-client-android/mediasoup-client")

include(":sdpparser")
project(":sdpparser").projectDir = File("../mediasoup/sdpparser/sdpparser")
