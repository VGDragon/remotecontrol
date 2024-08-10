import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    google()
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "21"
            }
        }
    }
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        google()
    }
    jvm("desktop")


    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)
            implementation(libs.ktor.client.core)

            // add javax.websocket
            implementation("com.neovisionaries:nv-websocket-client:2.3")
            implementation("org.java-websocket:Java-WebSocket:1.5.4")

            // Ktor dependencies websocket
            implementation("io.ktor:ktor-websockets:2.3.8")
            implementation("com.squareup.okhttp3:okhttp:4.12.0")
            // Ktor dependencies
            //implementation("io.ktor:ktor-server-core:2.3.8")
            //implementation("io.ktor:ktor-serialization:2.3.8")

            // add json parser gson
            implementation("com.google.code.gson:gson:2.8.7") // TODO change to kotson or moshi
            implementation("me.sujanpoudel.mputils:paths:0.1.1")

            // websockets server
            implementation("io.ktor:ktor-server-core-jvm:2.3.10")
            implementation("io.ktor:ktor-server-websockets-jvm:2.3.10")
            implementation("io.ktor:ktor-server-netty-jvm:2.3.10")
            implementation("io.ktor:ktor-server-core:2.3.10")
            implementation("io.ktor:ktor-server-netty:2.3.10")
            implementation("io.ktor:ktor-server-websockets:2.3.10")

            // websockets client
            implementation("io.ktor:ktor-client-core:2.3.10")
            implementation("io.ktor:ktor-client-cio:2.3.10")
            implementation("io.ktor:ktor-client-websockets:2.3.10")
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation("io.ktor:ktor-server-netty-jvm:2.3.8")
        }

    }
}

android {
    namespace = "net.vgdragon.remotecontrol"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "net.vgdragon.remotecontrol"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    dependencies {
        debugImplementation(libs.compose.ui.tooling)
    }
    packagingOptions {
        exclude("META-INF/INDEX.LIST")
        exclude("META-INF/io.netty.versions.properties")
    }

}
dependencies {
    implementation(libs.play.services.location)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "net.vgdragon.remotecontrol"
            packageVersion = "1.0.1"
        }
    }
}
