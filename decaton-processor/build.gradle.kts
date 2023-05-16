plugins {
    id("conventions.common")
    id("org.springframework.boot")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation(libs.decaton.processor)
    implementation(libs.decaton.brave)
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
}
