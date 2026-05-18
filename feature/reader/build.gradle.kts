plugins {
    id("flux.android.feature")
    alias(libs.plugins.kotlin.compose)
    id("flux.hilt")
}

android {
    namespace = "com.example.flux.feature.reader"

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.coroutines.android)

    debugImplementation(libs.compose.ui.tooling)
}
