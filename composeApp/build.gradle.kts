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
            implementation(libs.androidx.compose.navigation)
            implementation(libs.compose.material.symbols.rounded)
            //ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            //kotlin
            implementation(libs.kotlinx.date.time)
            //koin
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.annotations)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }
        commonTest.dependencies {
            // ponytail: no io.kotest/KSP plugins, so js/wasm tests compile but are not discovered; add both to run them there
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
        }
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.cio)
            implementation(libs.logback.classic)
        }
        webMain.dependencies {
            // localStorage for the session token (TokenStore.web.kt)
            implementation(libs.kotlinx.browser)
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

buildkonfig {
    packageName = "com.anjo.statisticsui"
    defaultConfigs {
        // -PbackendUrl= (empty): the web build calls the origin it was served from (container image)
        buildConfigField(STRING, "backendurl", providers.gradleProperty("backendUrl").getOrElse("http://127.0.0.1:8080"))
    }
}

compose.desktop {
    application {
        mainClass = "com.anjo.statisticsui.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.anjo.statisticsui"
            packageVersion = providers.gradleProperty("appVersion").getOrElse("1.0.0")
            // loopback receiver for provider sign-in (ProviderSignIn.jvm.kt)
            modules("jdk.httpserver")
        }
    }
}
