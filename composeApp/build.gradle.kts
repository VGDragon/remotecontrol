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
    google()
}


kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
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

            // Note, if you develop a library, you should use compose.desktop.common.
            // compose.desktop.currentOs should be used in launcher-sourceSet
            // (in a separate module for demo project and in testMain).
            // With compose.desktop.common you will also lose @Preview functionality
           // implementation(compose.desktop.currentOs)
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
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
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
        applicationId = "org.example.project"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    dependencies {
        debugImplementation(libs.compose.ui.tooling)
    }
}

compose.desktop {
    application {
        mainClass = "RemoteConnectionGuiKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "net.vgdragon.remotecontrol"
            packageVersion = "1.0.0"
        }
    }
}
