plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.gradle.plugin.spring.boot)

    // Workaround for referencing org.gradle.accessors.dm.LibrariesForLibs in pre-compiled script
    // https://github.com/gradle/gradle/issues/15383
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
