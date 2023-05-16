package conventions

plugins {
    java
    `java-library`
    `project-report`
}

group = "example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

dependencies {
    implementation(platform(libs.brave.bom))
    implementation(platform(libs.opentelemetry.bom))
    implementation(platform(libs.spring.boot.bom))
}
