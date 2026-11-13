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
        versionCode = 31
        versionName = "1.7.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Opt-in private owner build; ordinary builds never include bootstrap credentials.
    val privateSetupAssets = providers.gradleProperty("privateSetupAssetsDir")
    if (privateSetupAssets.isPresent) {
        sourceSets.getByName("debug").assets.srcDir(privateSetupAssets.get())
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    bundle { language { enableSplit = false } }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(project(":recipe-core"))
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
