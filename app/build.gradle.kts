import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.android.build.api.variant.impl.VariantOutputImpl

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
//    alias(libs.plugins.hilt.android)
//    kotlin("kapt")
}

android {
    namespace = "com.pcrclicker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pcrclicker"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidComponents {
        onVariants(selector().all()) { variant ->
            variant.outputs.forEach { output ->
                val fileName = "pcrclicker-${variant.buildType}-v${defaultConfig.versionName}.apk"
                if (output is VariantOutputImpl) {
                    output.outputFileName.set(fileName)
                }
            }
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("pcrclicker.jks")
            storePassword = "pcrclicker"
            keyAlias = "pcrclicker"
            keyPassword = "pcrclicker"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("11")
    }
}

//kapt {
//    correctErrorTypes = true
//}

dependencies {
//    implementation(libs.hilt.android)
//    kapt(libs.hilt.android.compiler)
//    implementation(libs.hilt.navigation.compose)
//    implementation(libs.hilt.work)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.window)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.viewbinding)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

//    testImplementation(libs.hilt.testing)
//    kaptTest(libs.hilt.android.compiler)
//    androidTestImplementation(libs.hilt.testing)
//    kaptAndroidTest(libs.hilt.android.compiler)
}
