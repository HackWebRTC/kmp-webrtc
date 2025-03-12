plugins {
    alias(libs.plugins.kmp)
}

kotlin {
    js(IR) {
        browser {
        }
        binaries.executable()
    }

    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":kmp-webrtc"))
            }
        }
    }
}
