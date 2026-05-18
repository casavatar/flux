plugins {
    id("flux.kotlin.library")
}

dependencies {
    implementation(libs.javax.inject)
    implementation(libs.coroutines.core)

    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.turbine)
}
