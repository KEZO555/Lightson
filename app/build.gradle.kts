plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "app.lightson"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.lightson"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "1.3.0"
    }

    signingConfigs {
        create("release") {
            // Personal sideload key, committed on purpose so CI releases and
            // Obtainium updates carry a consistent signature. Do not reuse
            // this key for anything that ships to an app store.
            storeFile = rootProject.file("signing/lightson.jks")
            storePassword = "lightson"
            keyAlias = "lightson"
            keyPassword = "lightson"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
