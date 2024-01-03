import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

group = "net.vgdragon"
version = "1.0-SNAPSHOT"
val kotlinVersion = "1.5.21"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    // add javax.websocket
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.neovisionaries:nv-websocket-client:2.3")
    implementation("org.java-websocket:Java-WebSocket:1.5.4")
    // add json parser gson
    implementation("com.google.code.gson:gson:2.8.7") // TODO change to kotson or moshi
    // https://mvnrepository.com/artifact/com.github.salomonbrys.kotson/kotson
    //runtimeOnly("com.github.salomonbrys.kotson:kotson:2.5.0")
    //implementation("com.squareup.moshi:moshi:1.14.0")


}


compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "remotecontrol"
            packageVersion = "1.0.0"
        }
    }
}
