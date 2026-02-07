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
                implementation(projects.feature.starred.domain)

                implementation(libs.bundles.landscapist)

                implementation(libs.kotlinx.collections.immutable)

                implementation(compose.components.uiToolingPreview)
                implementation(compose.components.resources)
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