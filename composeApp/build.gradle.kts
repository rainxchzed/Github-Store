import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
}

// Load local.properties for secrets like GITHUB_CLIENT_ID
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { this.load(it) }
}
val localGithubClientId = (localProps.getProperty("GITHUB_CLIENT_ID") ?: "").trim()

// Generate BuildConfig for JVM (Configuration Cache Compatible)
val generateJvmBuildConfig = tasks.register("generateJvmBuildConfig") {
    val outputDir = layout.buildDirectory.dir("generated/buildconfig/jvm")
    val clientId = localGithubClientId

    outputs.dir(outputDir)

    doLast {
        val file = outputDir.get().asFile.resolve("zed/rainxch/githubstore/BuildConfig.kt")
        file.parentFile.mkdirs()
        file.writeText("""
            package zed.rainxch.githubstore
            
            object BuildConfig {
                const val GITHUB_CLIENT_ID = "$clientId"
            }
        """.trimIndent())
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_20)
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            // Koin DI
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            // Ktor client for Android
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            // Secure storage (direct coordinate to avoid catalog accessor mismatch)
            implementation(libs.androidx.security.crypto)
            implementation(libs.core.splashscreen)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Koin core
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            // Shared HTTP and serialization
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)

            // Navigation
            implementation(libs.navigation.compose)

            // Logging
            implementation(libs.kermit)

            // Coil
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)

            implementation(libs.kotlinx.datetime)

            implementation(libs.multiplatform.markdown.renderer)
            implementation(libs.multiplatform.markdown.renderer.coil3)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/buildconfig/jvm"))

            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutinesSwing)
                // Koin core
                implementation(libs.koin.core)
                // Ktor client for JVM Desktop
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.java)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}

tasks.named("compileKotlinJvm") {
    dependsOn(generateJvmBuildConfig)
}

android {
    namespace = "zed.rainxch.githubstore"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "zed.rainxch.githubstore"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 2
        versionName = "1.0.1"
        // Expose GitHub client id to Android BuildConfig (do NOT commit secrets; read from local.properties)
        buildConfigField("String", "GITHUB_CLIENT_ID", "\"${localGithubClientId}\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_20
        targetCompatibility = JavaVersion.VERSION_20
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "zed.rainxch.githubstore.MainKt"
        nativeDistributions {
            packageName = "github-store"
            packageVersion = "1.0.1"
            vendor = "rainxchzed"
            includeAllModules = true
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Pkg,
                TargetFormat.Exe,
                TargetFormat.Msi,
                TargetFormat.Deb,
                TargetFormat.Rpm,
            )
            windows {
                iconFile.set(project.file("logo/app_icon.ico"))
                menuGroup = "Github Store"
                shortcut = true
                perUserInstall = true
            }
            macOS {
                iconFile.set(project.file("logo/app_icon.icns"))
                bundleID = "zed.rainxch.githubstore"
            }
            linux {
                iconFile.set(project.file("logo/app_icon.png"))
                appRelease = "2"
                debPackageVersion = "1.0.1"
                menuGroup = "Development"
                appCategory = "Development"
            }
        }
    }
}