import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.convention.cmp.application)
    alias(libs.plugins.compose.hot.reload)
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation(libs.core.splashscreen)

            implementation(libs.koin.android)
        }
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.domain)
            implementation(projects.core.presentation)

            implementation(projects.feature.apps.domain)
            implementation(projects.feature.apps.data)
            implementation(projects.feature.apps.presentation)

            implementation(projects.feature.auth.domain)
            implementation(projects.feature.auth.data)
            implementation(projects.feature.auth.presentation)

            implementation(projects.feature.details.domain)
            implementation(projects.feature.details.data)
            implementation(projects.feature.details.presentation)

            implementation(projects.feature.devProfile.domain)
            implementation(projects.feature.devProfile.data)
            implementation(projects.feature.devProfile.presentation)

            implementation(projects.feature.favourites.domain)
            implementation(projects.feature.favourites.data)
            implementation(projects.feature.favourites.presentation)

            implementation(projects.feature.home.domain)
            implementation(projects.feature.home.data)
            implementation(projects.feature.home.presentation)

            implementation(projects.feature.search.domain)
            implementation(projects.feature.search.data)
            implementation(projects.feature.search.presentation)

            implementation(projects.feature.settings.domain)
            implementation(projects.feature.settings.data)
            implementation(projects.feature.settings.presentation)

            implementation(projects.feature.starred.domain)
            implementation(projects.feature.starred.data)
            implementation(projects.feature.starred.presentation)

            implementation(libs.jetbrains.compose.navigation)
            implementation(libs.bundles.koin.common)
            implementation(libs.liquid)
            implementation(libs.jetbrains.compose.material.icons.extended)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.jetbrains.compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.jetbrains.compose.viewmodel)
            implementation(libs.jetbrains.lifecycle.compose)
        }

        jvmMain {
            dependencies {
                implementation(projects.core.presentation)
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.kotlin.stdlib)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.koin.compose.viewmodel)

                implementation(compose.desktop.linux_x64)
                implementation(compose.desktop.linux_arm64)
                implementation(compose.desktop.macos_x64)
                implementation(compose.desktop.macos_arm64)
                implementation(compose.desktop.windows_x64)
                implementation(compose.desktop.windows_arm64)
            }
        }
    }
}


compose.desktop {
    application {
        mainClass = "zed.rainxch.githubstore.MainKt"
        nativeDistributions {
            packageName = "GitHub-Store"
            packageVersion = libs.versions.projectVersionName.get().toString()
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
                appRelease = libs.versions.projectVersionName.get().toString()
                debPackageVersion = libs.versions.projectVersionName.get().toString()
                menuGroup = "Development"
                appCategory = "Development"
            }
        }
    }
}