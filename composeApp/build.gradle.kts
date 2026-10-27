@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.koinCompiler)
    alias(libs.plugins.buildKonfig)
}

kotlin {
    jvm()

    js {
        browser {
            commonWebpackConfig {
                // UI dev server on 8081, backend keeps 8080 (CORS_HOSTS default)
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).copy(port = 8081)
            }
        }
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                // UI dev server on 8081, backend keeps 8080 (CORS_HOSTS default)
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).copy(port = 8081)
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.charty)
            //compose
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.compose.navigation)
            implementation(libs.compose.material.icons.rounded)
            implementation(libs.compose.material.symbols.rounded)
            //lifecycle
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            //ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            implementation(libs.kotlin.logging)

            //kotlin
            implementation(libs.kotlinx.date.time)
            //koin
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.annotations)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            // ponytail: no io.kotest/KSP plugins, so js/wasm tests compile but are not discovered; add both to run them there
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
            implementation(libs.koin.test)
            // no test uses it: pins koin-test's kotlin-test to the compiler version, js/wasm reject the older klib
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.kotlin.logging.jvm)
            implementation(libs.logback.classic)
        }
        webMain.dependencies {
            // localStorage for the session token (TokenStore.web.kt)
            implementation(libs.kotlinx.browser)
        }
        jsMain.dependencies {
            implementation(libs.kotlin.logging.js)
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlin.logging.wasmjs)
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

buildkonfig {
    packageName = "com.anjo.statisticsui"
    defaultConfigs {
        buildConfigField(STRING, "backendurl", "http://127.0.0.1:8080")
    }
}

compose.desktop {
    application {
        mainClass = "com.anjo.statisticsui.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.anjo.statisticsui"
            packageVersion = "1.0.0"
        }
    }
}
