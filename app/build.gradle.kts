import com.android.build.api.dsl.ApplicationExtension

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization") version "2.1.20"
}

android {
    namespace = "moe.ore.txhook"
    compileSdk = 35

    defaultConfig {
        applicationId = "moe.ore.txhook"
        minSdk = 23
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 29
        versionCode = 25040918
        versionName = "3.0.2"
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
                "bsh/**",
                "WEB-INF/**",
                "okhttp3/**",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json"
            )
        }
    }

    sourceSets {
        named("main") {
            jniLibs.setSrcDirs(listOf("libs"))
        }
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
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")

    compileOnly("de.robv.android.xposed:api:82")

    // 数据处理的库
    implementation("com.google.code.gson:gson:2.13.0")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation("org.jetbrains.kotlinx:kotlinx-io-jvm:0.1.16")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.8.1")
    implementation(files("libs/protobuf-java-3.5.1.jar"))

    // UI交互的库
    implementation("com.rengwuxian.materialedittext:library:2.1.4")
    implementation("com.github.yogkin:SettingView:1.0.8")

    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("org.apache-extras.beanshell:bsh:2.0b6")
}