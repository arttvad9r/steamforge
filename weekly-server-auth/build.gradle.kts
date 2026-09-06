plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":weekly-server-core"))
    implementation(libs.java.jwt)

    testImplementation(libs.junit)
}
