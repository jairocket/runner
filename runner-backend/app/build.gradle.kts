plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
}

application {
    mainClass.set("com.runner.ApplicationKt")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":infra"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.neg)
    implementation(libs.ktor.serialization.json)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.hikari)
    implementation(libs.postgres.driver)
    implementation(libs.dotenv)
    implementation(libs.logback)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(kotlin("test"))
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.test {
    useJUnitPlatform()
}
