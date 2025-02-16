plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "${Consts.androidNS}.android"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "${Consts.androidNS}.android"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = Consts.releaseVersion

        sourceSets.getByName("main").assets.srcDir("../common/assets")
    }
    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvm.get().toInt())
    }

    kotlin {
        jvmToolchain(libs.versions.jvm.get().toInt())
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":kmp-webrtc"))
    implementation(files("../../libs/android/webrtc.aar"))
    implementation(libs.androidx.lifecycle)
    implementation(libs.kmpXlog)

    implementation(libs.androidx.appcompat)
    implementation(libs.permissionsDispatcher)
    annotationProcessor(libs.permissionsDispatcher.processor)
}
