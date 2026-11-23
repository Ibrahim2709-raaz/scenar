plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.scenar"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.scenar"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SCENAR_AI_ENDPOINT", "\"\"")
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "SCENAR_AI_ENDPOINT",
                "\"http://10.0.2.2:3000/api/incident\""
            )
        }
        release {
            buildConfigField("String", "SCENAR_AI_ENDPOINT", "\"\"")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.intents)
    androidTestImplementation(libs.ui.automator)
    androidTestImplementation(libs.ext.junit)
    implementation(libs.gson)
}
