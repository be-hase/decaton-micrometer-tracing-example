plugins {
    id("conventions.common")
    id("org.springframework.boot")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.apache.kafka:kafka-clients")
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation(libs.opentelemetry.kafka.clients)
}
