import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.vanniktech.mavenPublish)
    // alias will fail, see https://github.com/gradle/gradle/issues/20084
    id("org.jetbrains.kotlin.native.cocoapods")
}

version = Consts.releaseVersion
group = Consts.releaseGroup

kotlin {
//    jvm {
//        withJava()
//    }

    val appleTargets = mapOf(
        iosArm64() to "ios-arm64",
        iosSimulatorArm64() to "ios-arm64_x86_64-simulator",
        iosX64() to "ios-arm64_x86_64-simulator",
    )
    appleTargets.keys.forEach {
        it.compilations.getByName("main").cinterops {
            val webrtc by creating {
                definitionFile.set(project.file("src/appleMain/cinterop/WebRTC.def"))
                compilerOpts(
                    "-framework", "WebRTC",
                    "-F${project.rootProject.projectDir}/libs/apple/WebRTC.xcframework/${appleTargets[it]}"
                )
            }
        }
    }

    cocoapods {
        summary = "KMP wrapper for WebRTC."
        homepage = "https://github.com/HackWebRTC/kmp-webrtc"
        version = Consts.releaseVersion
        ios.deploymentTarget = libs.versions.iosDeploymentTarget.get()
        podfile = project.file("../example/iosApp/Podfile")
        framework {
            baseName = "kmp_webrtc"
            isStatic = true
        }
//        pod("kmp_xlog") {
//            version = libs.versions.kmpXlog.get()
//        }
    }

//    js(IR) {
//        browser {
//        }
//        binaries.executable()
//    }
//    mingwX64 {}
//    linuxX64 {}

    applyDefaultHierarchyTemplate()
    sourceSets {
        all {
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
            languageSettings.optIn("kotlin.experimental.ExperimentalNativeApi")
        }

        commonMain {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kmpXlog)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
//        jvmMain {
//            dependencies {
//            }
//        }
        appleMain {
            dependencies {
            }
        }
//        jsMain {
//            dependencies {
//            }
//        }
//        mingwMain {
//            dependencies {
//            }
//        }
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates(group.toString(), Consts.releaseName, version.toString())

    pom {
        name = "kmp-webrtc"
        description = "KMP wrapper for WebRTC."
        inceptionYear = "2025"
        url = "https://github.com/HackWebRTC/kmp-webrtc"
        licenses {
            license {
                name = "MIT"
                url = "https://opensource.org/licenses/MIT"
                distribution = "https://opensource.org/licenses/MIT"
            }
        }
        developers {
            developer {
                id = "Piasy"
                name = "Piasy Xu"
                url = "xz4215@gmail.com"
            }
        }
        scm {
            url = "https://github.com/HackWebRTC/kmp-webrtc"
            connection = "scm:git:git://github.com/HackWebRTC/kmp-webrtc.git"
            developerConnection = "scm:git:git://github.com/HackWebRTC/kmp-webrtc.git"
        }
    }
}
