plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.scanner.app"
    compileSdk = 34
    ndkVersion = "27.3.13750724"

    defaultConfig {
        applicationId = "com.scanner.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        freeCompilerArgs += listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true")
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}

val rustBuildTask = tasks.register<Exec>("buildRustLibrary") {
    workingDir = file("${projectDir}/src/main/rust")
    commandLine(
        "cargo", "ndk",
        "--target", "aarch64-linux-android",
        "--", "build", "--release"
    )
    doLast {
        val soFile = file("${workingDir}/target/aarch64-linux-android/release/libproduct_lib.so")
        val destDir = file("${projectDir}/src/main/jniLibs/arm64-v8a")
        destDir.mkdirs()
        soFile.copyTo(file("$destDir/libproduct_lib.so"), overwrite = true)
    }
}

tasks.whenTaskAdded {
    if (name.contains("mergeDebugJniLibFolders") || name.contains("mergeReleaseJniLibFolders")) {
        dependsOn(rustBuildTask)
    }
}