pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

rootProject.name = "decaton-micrometer-tracing-example"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":brave-producer")
include(":decaton-processor")
