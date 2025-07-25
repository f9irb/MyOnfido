plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.onfidoloader"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.onfidoloader"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    signingConfigs {
        create("release") {
            storeFile = file("../my-release-key.jks")    // путь к keystore
            storePassword = "172339"                  // ← твой пароль
            keyAlias = "release_alias"                // alias
            keyPassword = "172339"                    // пароль ключа
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false      // можно сделать true, если нужен R8
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // ✅ УСТАНОВКА СОВМЕСТИМОСТИ С JAVA 1.8
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition-common:19.0.0")
}

