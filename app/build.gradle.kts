import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val appVerCode: Int by lazy {
    getGitCommitCount()
}
val appVerName: String = "3.2.2" + ".r${getGitCommitCount()}." + getVersionName()

android {
    namespace = "moe.ore.txhook"
    compileSdk = 36

    defaultConfig {
        applicationId = "moe.ore.txhook"
        minSdk = 24
        targetSdk = 36
        versionCode = appVerCode
        versionName = appVerName
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

    android.applicationVariants.all {
        outputs.map { it as BaseVariantOutputImpl }
            .forEach {
                it.outputFileName = "TXHook-v${versionName}.apk"
            }
    }

    flavorDimensions.add("mode")

    productFlavors {
        create("app") {
            dimension = "mode"
        }
    }

    configureAppSigningConfigsForRelease()
}

fun configureAppSigningConfigsForRelease() {
    val keystorePath: String? = System.getenv("KEYSTORE_PATH")
    project.configure<ApplicationExtension> {
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
        buildTypes {
            var signatureDigest: String? = getSignatureKeyDigest(signingConfigs.findByName("release"))
            if (signatureDigest != null) {
                println("Signature Digest: $signatureDigest")
            } else {
                println("No Signature Digest Configured")
            }
            release {
                isMinifyEnabled = true
                isShrinkResources = true
                if (!keystorePath.isNullOrBlank()) {
                    signingConfig = signingConfigs.findByName("release")
                }
                proguardFiles("proguard-rules.pro")
            }
        }
    }
}

fun getLocalProperty(propertyName: String): String? {
    val rootProject = project.rootProject
    val localProp = File(rootProject.projectDir, "local.properties")
    if (!localProp.exists()) {
        return null
    }
    val localProperties = Properties()
    localProp.inputStream().use {
        localProperties.load(it)
    }
    return localProperties.getProperty(propertyName, null)
}

fun getGitCommitCount(): Int {
    val out = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        standardOutput = out
    }
    return out.toString().trim().toInt()
}

fun getGitCommitHash(): String {
    val out = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        standardOutput = out
    }
    return out.toString().trim()
}

fun getVersionName(): String {
    return getGitCommitHash()
}

fun getSignatureKeyDigest(signConfig: ApkSigningConfig?): String? {
    val key1: String? = if (signConfig?.storeFile != null) {
        // extract certificate digest
        val key = signConfig.storeFile
        val keyStore = KeyStore.getInstance(signConfig.storeType ?: KeyStore.getDefaultType())
        FileInputStream(key!!).use {
            keyStore.load(it, signConfig.storePassword!!.toCharArray())
        }
        val cert = keyStore.getCertificate(signConfig.keyAlias!!)
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(cert.encoded)
        digest.joinToString("") { "%02X".format(it) }
    } else null
    val key2: String? = getLocalProperty("ffekit.signature.md5digest")
        ?.uppercase(Locale.ROOT)?.ifEmpty { null }
    // check if key1 and key2 are the same
    if (key1 != null && key2 != null && key1 != key2) {
        error(
            "The signature key digest in the signing config and local.properties are different, " +
                    "got $key1 and $key2, please make sure they are the same."
        )
    }
    return (key1 ?: key2)?.also {
        check(it.matches(Regex("[0-9A-F]{32}"))) {
            "Invalid signature key digest: $it"
        }
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
