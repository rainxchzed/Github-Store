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
                implementation(projects.feature.home.domain)

                implementation(libs.liquid)

                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
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