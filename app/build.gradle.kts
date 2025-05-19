import com.android.build.api.dsl.ApplicationExtension

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization") version "2.1.21"
}

android {
    namespace = "moe.ore.txhook"
    compileSdk = 35

    defaultConfig {
        applicationId = "moe.ore.txhook"
        minSdk = 23
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 29
        versionCode = 25051202
        versionName = "3.0.5"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/**",
                "kotlin/**",
                "google/**",
                "org/**",
                "WEB-INF/**",
                "okhttp3/**",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json"
            )
        }
    }

    // Disable lint errors for this build
    lint {
        abortOnError = false
    }

    configureAppSigningConfigsForRelease(project)
}

fun configureAppSigningConfigsForRelease(project: Project) {
    val keystorePath: String? = System.getenv("KEYSTORE_PATH")
    project.configure<ApplicationExtension> {
        if (!keystorePath.isNullOrBlank()) {
            signingConfigs {
                create("release") {
                    storeFile = file(keystorePath)
                    storePassword = System.getenv("KEYSTORE_PASSWORD")
                    keyAlias = System.getenv("KEY_ALIAS")
                    keyPassword = System.getenv("KEY_PASSWORD")
                    enableV1Signing = true
                    enableV2Signing = true
                    enableV3Signing = true
                }
            }
        }
        buildTypes {
            release {
                if (!keystorePath.isNullOrBlank()) {
                    signingConfig = signingConfigs.findByName("release")
                }

                // 代码压缩
                isMinifyEnabled = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

                // 移除无用的 resource 文件
                isShrinkResources = true

                // 禁用调试
                isDebuggable = false
            }
            debug {
                if (!keystorePath.isNullOrBlank()) {
                    signingConfig = signingConfigs.findByName("release")
                }
            }
        }
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")

    implementation("com.google.code.gson:gson:2.13.1")
    implementation("com.google.protobuf:protobuf-java:4.31.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-io-jvm:0.1.16")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.8.1")

    implementation("com.rengwuxian.materialedittext:library:2.1.4")

    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
}
