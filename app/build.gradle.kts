import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

abstract class GitCommitCount : ValueSource<Int, ValueSourceParameters.None> {
    @get:Inject abstract val execOperations: ExecOperations

    override fun obtain(): Int {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            standardOutput = output
        }
        return output.toString().trim().toInt()
    }
}

abstract class GitShortHash : ValueSource<String, ValueSourceParameters.None> {
    @get:Inject abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            commandLine("git", "rev-parse", "--short=7", "HEAD")
            standardOutput = output
        }
        return output.toString().trim()
    }
}

val gitCommitCount = providers.of(GitCommitCount::class.java) {}
val gitShortHash = providers.of(GitShortHash::class.java) {}

android {
    namespace = "moe.ore.txhook"
    compileSdk = 36

    defaultConfig {
        applicationId = "moe.ore.txhook"
        minSdk = 24
        targetSdk = 36
        versionCode = providers.provider { getBuildVersionCode(rootProject) }.get()
        versionName = "3.2.3"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        debug {
            val gitSuffix = providers.provider { getGitHeadRefsSuffix(rootProject, "debug") }.get()
            versionNameSuffix = ".${gitSuffix}"
        }
        release {
            val keystorePath: String? = System.getenv("KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                signingConfigs {
                    create("release") {
                        storeFile = file(keystorePath)
                        storePassword = System.getenv("KEYSTORE_PASSWORD")
                        keyAlias = System.getenv("KEY_ALIAS")
                        keyPassword = System.getenv("KEY_PASSWORD")
                        enableV2Signing = true
                    }
                }
            }

            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val gitSuffix = providers.provider { getGitHeadRefsSuffix(rootProject, "release") }.get()
            versionNameSuffix = ".${gitSuffix}"

            if (!keystorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.findByName("release")
            }
        }
    }

    packaging {
        resources.excludes.addAll(
            arrayOf(
                "META-INF/**",
                "kotlin/**",
                "google/**",
                "org/**",
                "WEB-INF/**",
                "okhttp3/**",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json"
            )
        )
    }

    lint {
        abortOnError = false
    }

    applicationVariants.all {
        outputs.all {
            val output = this as BaseVariantOutputImpl
            output.outputFileName?.let { fileName ->
                if (fileName.endsWith(".apk")) {
                    val projectName = rootProject.name
                    val currentVersionName = versionName
                    output.outputFileName = "${projectName}-v${currentVersionName}.APK"
                }
            }
        }
    }
}

fun getGitHeadRefsSuffix(project: Project, buildType: String): String {
    val rootProject = project.rootProject
    val projectDir = rootProject.projectDir
    val headFile = File(projectDir, ".git" + File.separator + "HEAD")
    return if (headFile.exists()) {
        try {
            val commitCount = gitCommitCount.get()
            val hash = gitShortHash.get()
            val prefix = if (buildType == "debug") "d" else "r"
            "$prefix$commitCount.$hash"
        } catch (e: Exception) {
            println("Failed to get git info: ${e.message}")
            ".standalone"
        }
    } else {
        println("Git HEAD file not found")
        ".standalone"
    }
}

fun getBuildVersionCode(project: Project): Int {
    val rootProject = project.rootProject
    val projectDir = rootProject.projectDir
    val headFile = File(projectDir, ".git" + File.separator + "HEAD")
    return if (headFile.exists()) {
        try {
            gitCommitCount.get()
        } catch (e: Exception) {
            println("Failed to get git commit count: ${e.message}")
            1
        }
    } else {
        println("Git HEAD file not found")
        1
    }
}

dependencies {
    compileOnly(libs.xposed.api)
    implementation(libs.android.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.ezxhelper.android.utils)
    implementation(libs.ezxhelper.core)
    implementation(libs.ezxhelper.xposed.api)
    implementation(libs.google.gson)
    implementation(libs.google.protobuf)
    implementation(libs.kotlinx.io.jvm)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.materialedittext.library)
    implementation(libs.okhttp3.okhttp)
}
