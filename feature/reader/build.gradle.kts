plugins {
    id("flux.android.feature")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.flux.feature.reader"

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
}
