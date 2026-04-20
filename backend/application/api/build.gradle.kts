plugins {
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":infrastructure"))

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.1")

    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.6.0")

    // Flyway runs JDBC — keep driver on app classpath
    implementation("org.flywaydb:flyway-core:10.21.0")
    implementation("org.flywaydb:flyway-mysql:10.21.0")
    runtimeOnly("com.mysql:mysql-connector-j:8.4.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.mockito")
    }
    testImplementation("io.projectreactor:reactor-test")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("agentfactory-api.jar")
    mainClass.set("io.agentfactory.application.api.ApplicationKt")
}
