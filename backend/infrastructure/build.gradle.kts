plugins {
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":domain"))

    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("io.asyncer:r2dbc-mysql:1.3.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.1")

    implementation("org.flywaydb:flyway-core:10.21.0")
    implementation("org.flywaydb:flyway-mysql:10.21.0")
    runtimeOnly("com.mysql:mysql-connector-j:8.4.0")
}
