plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":weekly-core"))
    implementation(project(":weekly-protocol"))
    testImplementation(libs.junit)
}
