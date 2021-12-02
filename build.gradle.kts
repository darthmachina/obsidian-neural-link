plugins {
    kotlin("js") version "1.6.0"
}

group = "io.github.darthmachina"
version = "0.2.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
}

dependencies {
    val kotlinVersion = "1.6.0"
    val kotlinxHtmlVersion = "0.7.3"

    implementation(npm("obsidian", "0.12.17"))

    implementation("org.jetbrains.kotlinx:kotlinx-html-js:$kotlinxHtmlVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinxHtmlVersion")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-RC")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.6.0-RC")

    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
}

kotlin {
    js(IR) {
        binaries.executable()
        browser {
            useCommonJs()
            commonWebpackConfig {
                cssSupport.enabled = true
                // Set DEVELOPMENT mode for webpack to get better messaging when testing
//                mode = org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode.DEVELOPMENT
            }
            webpackTask {
                output.libraryTarget = "commonjs"
                output.library = null
                outputFileName = "main.js"
            }
        }
    }
}

// OptIn to JsExport annotation
tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

project.gradle.startParameter.excludedTaskNames.add("browserTest")
