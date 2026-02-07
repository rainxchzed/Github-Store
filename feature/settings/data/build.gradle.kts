plugins {
    alias(libs.plugins.convention.kmp.library)
    alias(libs.plugins.convention.buildkonfig)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)

                implementation(projects.core.domain)
                implementation(projects.core.data)
                implementation(projects.feature.settings.domain)
            }
        }

        androidMain {
            dependencies {

            }
        }

        jvmMain {
            dependencies {

            }
        }
    }

}