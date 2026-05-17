pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Flux"
include(":app")
include(":core:domain")
include(":core:data")
include(":core:common")
include(":feature:library")
include(":feature:reader")
include(":feature:export")
