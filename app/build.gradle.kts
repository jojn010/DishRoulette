plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.dishroulette"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.dishroulette"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "1.0.1"
        buildConfigField("String", "RECIPE_BASE_URL", "\"https://localhost/\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // adb reverse forwards this port on both USB devices and Android emulators.
            buildConfigField("String", "RECIPE_BASE_URL", "\"http://127.0.0.1:8080/\"")
        }
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":recipe-core"))
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
