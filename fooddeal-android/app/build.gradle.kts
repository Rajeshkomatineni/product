plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.fooddeal.companion"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.fooddeal.companion"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
    }
    signingConfigs {
        create("release") {
            val keystorePath = project.findProperty("RELEASE_STORE_FILE")?.toString()
            val storePassword = project.findProperty("RELEASE_STORE_PASSWORD")?.toString()
            val keyAlias = project.findProperty("RELEASE_KEY_ALIAS")?.toString()
            val keyPassword = project.findProperty("RELEASE_KEY_PASSWORD")?.toString()
            if (!keystorePath.isNullOrBlank() && !storePassword.isNullOrBlank() &&
                !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()) {
                storeFile = file(keystorePath)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
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
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
