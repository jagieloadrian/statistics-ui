@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

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
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
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
            implementation(libs.kotlin.test)
            implementation(libs.koin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.kotlin.logging.jvm)
            implementation(libs.logback.classic)
        }
        jsMain.dependencies {
            implementation(libs.kotlin.logging.js)
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlin.logging.wasmjs)
        }
    }
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
