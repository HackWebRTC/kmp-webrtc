plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.android.library)
    alias(libs.plugins.cocoapods)
    alias(libs.plugins.allopen)
}

version = Consts.releaseVersion
group = Consts.releaseGroup

val projectRoot: String = project.projectDir.parent

kotlin {
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    macosX64()
    macosArm64()

    cocoapods {
        summary = "KMP wrapper for WebRTC."
        homepage = "https://github.com/HackWebRTC/kmp-webrtc"
        version = Consts.releaseVersion
        ios.deploymentTarget = libs.versions.iosDeploymentTarget.get()
        osx.deploymentTarget = "11.0"
        podfile = project.file("../example/iosApp/Podfile")
        framework {
            baseName = "kmp_webrtc"
            isStatic = true
        }

        pod("WebRTC") {
            source = path(project.file("$projectRoot/libs/apple/"))
        }

        pod("kmp_xlog") {
            version = libs.versions.kmpXlog.get()
        }
    }

    androidTarget {
        publishLibraryVariants("release")
    }

    js(IR) {
        browser {
        }
        binaries.executable()
    }

    mingwX64 {
        compilations.getByName("main").cinterops {
            val webrtc by creating {
                definitionFile.set(project.file("src/cppCommon/cinterop/WebRTC.def"))
                includeDirs {
                    allHeaders("$projectRoot/libs/windows_linux/include")
                }
            }
        }
        binaries {
            all {
                linkerOpts.addAll(listOf("-L$projectRoot/libs/windows/x64", "-lwin_pc_client.dll"))
                //freeCompilerArgs = listOf("-Xadd-light-debug=enable")
            }
            sharedLib {
                baseName = "kmp_webrtc"
            }
        }
    }

    linuxX64 {
        compilations.getByName("main").cinterops {
            val webrtc by creating {
                definitionFile.set(project.file("src/cppCommon/cinterop/WebRTC.def"))
                includeDirs {
                    allHeaders("$projectRoot/libs/windows_linux/include")
                }
            }
        }
        binaries {
            all {
                linkerOpts.addAll(listOf("-L$projectRoot/libs/linux/x64", "-llinux_pc_client"))
                //freeCompilerArgs = listOf("-Xadd-light-debug=enable")
            }
            sharedLib {
                baseName = "kmp_webrtc"
            }
        }
    }

    applyDefaultHierarchyTemplate()
    sourceSets {
        all {
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
            languageSettings.optIn("kotlin.experimental.ExperimentalNativeApi")
        }

        commonMain {
            dependencies {
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serialization.json)
                api(libs.kmpXlog)
                implementation(libs.mockative)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        appleMain {
            dependencies {
            }
        }
        androidMain {
            dependencies {
                implementation(files("../libs/android/webrtc.aar"))
                api(libs.androidx.lifecycle)
            }
        }
        jsMain {
            dependencies {
                api(libs.kotlin.stdlib.js)
                api(npm("webrtc-adapter", "9.0.1"))
            }
        }

        val cppCommon by creating {
            dependsOn(commonMain.get())
        }

        linuxMain {
            dependsOn(cppCommon)
        }
        mingwMain {
            dependsOn(cppCommon)
        }
    }
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = Consts.androidNS

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
    }

    kotlin {
        jvmToolchain(libs.versions.jvm.get().toInt())
    }
}

mavenPublishing {
    publishToMavenCentral()

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

allOpen {
    annotation("io.mockative.Mockable")
}
