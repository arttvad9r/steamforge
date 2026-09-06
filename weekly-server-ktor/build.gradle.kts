plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":weekly-protocol"))
    implementation(project(":weekly-server-core"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.body.limit)

    testImplementation(libs.junit)
    testImplementation(libs.ktor.server.test.host)
}
