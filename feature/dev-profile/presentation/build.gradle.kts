plugins {
    alias(libs.plugins.convention.cmp.feature)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)

                implementation(projects.core.domain)
                implementation(projects.core.presentation)
                implementation(projects.feature.devProfile.domain)

                implementation(compose.components.uiToolingPreview)
                implementation(compose.components.resources)

                implementation(libs.bundles.landscapist)
                implementation(libs.kotlinx.collections.immutable)
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