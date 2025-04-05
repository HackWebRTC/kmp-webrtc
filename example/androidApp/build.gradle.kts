plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
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

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    implementation(libs.kotlinx.serialization.json)

//    implementation(project(":sdpparser"))
    implementation(project(":proto-client"))
//    implementation(project(":mediasoup-client"))
    implementation("com.squareup.okhttp3:okhttp:4.3.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.3.0")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava2:rxjava:2.2.6")

    implementation(libs.androidx.appcompat)
    implementation(libs.permissionsDispatcher)
    annotationProcessor(libs.permissionsDispatcher.processor)
    implementation(libs.androidx.activity.compose)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.kotlin.test)
}
