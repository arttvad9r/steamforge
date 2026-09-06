plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":weekly-server-core"))
    implementation(project(":weekly-protocol"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    runtimeOnly("org.postgresql:postgresql:42.7.13")

    testImplementation(libs.junit)
    testImplementation("com.h2database:h2:2.5.250")
    testImplementation(libs.kotlinx.coroutines.test)
}
