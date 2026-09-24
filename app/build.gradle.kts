plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.tibi"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.tibi"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "0.1.0"
    }

    signingConfigs {
        create("release") {
            val yol = System.getenv("TIBI_KEYSTORE_YOLU")
            if (yol != null) {
                storeFile = file(yol)
                storePassword = System.getenv("TIBI_KEYSTORE_SIFRE")
                keyAlias = System.getenv("TIBI_ANAHTAR_ADI")
                keyPassword = System.getenv("TIBI_ANAHTAR_SIFRE")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (System.getenv("TIBI_KEYSTORE_YOLU") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
}
