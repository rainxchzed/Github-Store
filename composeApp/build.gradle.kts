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
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

val appVersionName = "1.4.2"
val appVersionCode = 8

// Load local.properties for secrets like GITHUB_CLIENT_ID
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { this.load(it) }
}
val localGithubClientId =
    (localProps.getProperty("GITHUB_CLIENT_ID") ?: "Ov23linTY28VFpFjFiI9").trim()

// Generate BuildConfig for JVM (Configuration Cache Compatible)
val generateJvmBuildConfig = tasks.register("generateJvmBuildConfig") {
    val outputDir = layout.buildDirectory.dir("generated/buildconfig/jvm")
    val clientId = localGithubClientId
    val versionName = appVersionName

    outputs.dir(outputDir)

    doLast {
        val file = outputDir.get().asFile.resolve("zed/rainxch/githubstore/BuildConfig.kt")
        file.parentFile.mkdirs()
        file.writeText(
            """
            package zed.rainxch.githubstore
            
            object BuildConfig {
                const val GITHUB_CLIENT_ID = "$clientId"
                const val VERSION_NAME = "$versionName"
            }
        """.trimIndent()
        )
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
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
            implementation(libs.material3)
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

            // Date-time
            implementation(libs.kotlinx.datetime)

            // Navigation 3
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)

            // Markdown
            implementation(libs.multiplatform.markdown.renderer)
            implementation(libs.multiplatform.markdown.renderer.coil3)

            // Liquid
            implementation(libs.liquid)

            // Data store
            implementation(libs.androidx.datastore)
            implementation(libs.androidx.datastore.preferences)

            // Room
            implementation(libs.androidx.room.runtime)
            implementation(libs.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
        }
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
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

afterEvaluate {
    tasks.matching { it.name.contains("kspKotlinJvm") || it.name == "compileKotlinJvm" }.configureEach {
        dependsOn(generateJvmBuildConfig)
    }
}

tasks.named("compileKotlinJvm") {
    dependsOn(generateJvmBuildConfig)
}

android {
    namespace = "zed.rainxch.githubstore"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    defaultConfig {
        applicationId = "zed.rainxch.githubstore"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName

        buildConfigField("String", "GITHUB_CLIENT_ID", "\"${localGithubClientId}\"")
        buildConfigField("String", "VERSION_NAME", "\"${appVersionName}\"")
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        buildConfig = true
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

dependencies {
    debugImplementation(compose.uiTooling)

    ksp(libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

compose.desktop {
    application {
        mainClass = "zed.rainxch.githubstore.MainKt"
        nativeDistributions {
            packageName = "github-store"
            packageVersion = appVersionName
            vendor = "rainxchzed"
            includeAllModules = true
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Pkg,
                TargetFormat.Exe,
                TargetFormat.Msi,
                TargetFormat.Deb,
                TargetFormat.Rpm,
                TargetFormat.AppImage
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
                appRelease = appVersionCode.toString()
                debPackageVersion = appVersionName
                menuGroup = "Development"
                appCategory = "Development"
            }
        }
    }
}
