plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.v2rayng"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.v2rayng"
        minSdk = 24
        targetSdk = 37
        versionCode = providers.gradleProperty("aetherVersionCode").orElse("4").get().toInt()
        versionName = providers.gradleProperty("aetherVersionName").orElse("0.1").get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("aetherRelease") {
            storeFile = rootProject.file(
                providers.environmentVariable("AETHER_KEYSTORE_PATH")
                    .orElse(".signing/aether-release.jks").get()
            )
            val passwordFile = rootProject.file(".signing/password")
            val password = providers.environmentVariable("AETHER_KEYSTORE_PASSWORD").orNull
                ?: passwordFile.takeIf { it.exists() }?.readText()?.trim()
            storePassword = password
            keyAlias = "aether"
            keyPassword = password
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "Aether Debug")
        }
        release {
            signingConfig = signingConfigs.getByName("aetherRelease")
            optimization {
                // Partial library optimization caused IllegalAccessError in kotlin.text at startup.
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

// Firebase is currently unused. Its registered application is release-only;
// do not reuse the release Firebase identity for the separate debug package.
tasks.matching { it.name == "processDebugGoogleServices" }.configureEach {
    enabled = false
}

dependencies {
    implementation("com.github.2dust:libv2ray:v26.11.9@aar")
    testImplementation("org.json:json:20240303")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.firebase.ai)
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}