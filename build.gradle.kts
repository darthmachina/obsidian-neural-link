plugins {
    kotlin("js") version "1.6.0"
    kotlin("plugin.serialization") version "1.6.0"
}

group = "io.github.darthmachina"
version = "0.6.2-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
}

dependencies {
    val kotlinVersion = "1.6.0"
    val kotlinxHtmlVersion = "0.7.3"
    val kvisionVersion = "5.8.2"

    implementation(npm("obsidian", "0.12.17"))

    implementation("org.jetbrains.kotlinx:kotlinx-html-js:$kotlinxHtmlVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html:$kotlinxHtmlVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.reduxkotlin:redux-kotlin:0.5.5")
    implementation("org.reduxkotlin:redux-kotlin-reselect:0.2.10")
    implementation("app.softwork:kotlinx-uuid-core:0.0.12")

    implementation("io.kvision:kvision:$kvisionVersion")

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
                mode = org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode.DEVELOPMENT
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
