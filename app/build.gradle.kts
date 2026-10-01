plugins {
    id("com.android.application")
}

android {
    namespace = "com.mose.assistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mosayeb.himose.personal"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.0.0-alpha"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}
