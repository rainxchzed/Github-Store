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
                implementation(projects.feature.details.domain)

                implementation(libs.markdown.renderer)
                implementation(libs.markdown.renderer.coil3)

                implementation(compose.components.resources)
                implementation(libs.liquid)
                implementation(libs.kotlinx.datetime)

                implementation(compose.components.uiToolingPreview)
                implementation(libs.bundles.landscapist)
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