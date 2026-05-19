plugins {
    id("flux.android.library")
    id("flux.hilt")
}

android {
    namespace = "com.example.flux.data"

    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "${rootDir}/room/schemas"
                arguments["room.incremental"] = "true"
            }
        }
    }

    sourceSets {
        getByName("androidTest") {
            assets.srcDir("${rootDir}/room/schemas")
        }
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)

    implementation(libs.readium.shared)
    implementation(libs.readium.streamer)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)

    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.coroutines.test)
}
