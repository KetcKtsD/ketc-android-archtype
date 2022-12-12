import com.android.build.api.dsl.*
import org.jetbrains.kotlin.gradle.tasks.*
import com.android.build.gradle.*
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.internal.dsl.*

buildscript {
    val kotlinVersion by extra { "1.7.20" }
    val dokkaVersion by extra { "1.7.20" }
    val androidJUnit5Version by extra { "1.7.1.1" }

    apply(from = "app-settings.gradle.kts")
    apply(from = "common-dependency.gradle.kts")
    apply(from = "module-settings-map.gradle.kts")

    repositories {
        mavenCentral()
        google()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.0.0-alpha09")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:${dokkaVersion}")
        classpath("de.mannodermaus.gradle.plugins:android-junit5:$androidJUnit5Version")
    }
}

buildDir = File("$rootDir/.build")

subprojects {
    val moduleSettingsMap: Map<String, Map<String, Any>> by extra

    @Suppress("UNCHECKED_CAST")
    fun <T> getSetting(
        name: String
    ): T = requireNotNull(moduleSettingsMap[this.name])[name] as T

    val isAndroid: Boolean = (getSetting("isAndroid") ?: false) as? Boolean ?: false
    val isAndroidLib: Boolean = (getSetting("isAndroidLib") ?: false) as? Boolean ?: false
    val isCompose: Boolean = (getSetting("isCompose") ?: false) as? Boolean ?: false
    val isAndroidProject = isAndroid || isAndroidLib

    repositories {
        mavenCentral()
        google()
        maven { setUrl("https://androidx.dev/storage/compose-compiler/repository/") }
    }

    //apply plugins
    plugins.apply("org.jetbrains.dokka")
    if (isAndroidProject) {
        plugins.apply("kotlin-android")
        if (isAndroidLib) plugins.apply("com.android.library")
        else plugins.apply("com.android.application")
        plugins.apply("de.mannodermaus.android-junit5")
    } else {
        plugins.apply("kotlin")
    }

    if (!isAndroidProject) tasks.withType<Test> {
        useJUnitPlatform {
            includeEngines = setOf("spek", "spek2")
        }
    }

    @Suppress("UnstableApiUsage") if (isAndroidProject) extensions.configure<TestedExtension>("android") {
        val rootScope = this

        val minAndroidSdkVersion: Int by extra
        val targetAndroidSdkVersion: Int by extra
        val androidBuildToolVersion: String by extra
        // val androidNdkVersion: String by extra

        val appVersionName: String by extra
        val appVersionCode: Int by extra
        val appId: String by extra

        when (this) {
            is LibraryExtension -> compileSdk = targetAndroidSdkVersion
            is BaseAppModuleExtension -> compileSdk = targetAndroidSdkVersion
        }
        buildToolsVersion = androidBuildToolVersion
        // ndkVersion = androidNdkVersion


        fun ExtensionAware.generateNamespace(): String {
            val namespace: String by extra
            return "$namespace.${project.name.split("-").joinToString(".")}"
        }
        namespace = generateNamespace()


        defaultConfig {
            minSdk = minAndroidSdkVersion
            if (rootScope is BaseAppModuleExtension) with(rootScope) {
                applicationId = appId
                targetSdk = targetAndroidSdkVersion
                versionName = appVersionName
                versionCode = appVersionCode
            }

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            testInstrumentationRunnerArguments.apply {
                this["runnerBuilder"] = "de.mannodermaus.junit5.AndroidJUnit5Builder"
            }

            consumerProguardFiles("consumer-rules.pro")
            /*
            ndk {
                abiFilters.addAll(setOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"))
            }
            */
        }

        if (rootScope is BaseAppModuleExtension) rootScope.buildFeatures {
            compose = isCompose
            buildConfig = true
        }

        if (rootScope is LibraryExtension) rootScope.buildFeatures {
            compose = isCompose
            buildConfig = false
        }

        if (rootScope is BaseAppModuleExtension) rootScope.signingConfigs {
            named("debug") {
                storeFile = file("$rootDir/debug.jks")
                storePassword = "android"
                keyAlias = "AndroidDebugKey"
                keyPassword = "android"
            }
        }

        buildTypes {
            getByName("release") {
                isMinifyEnabled = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    file("proguard-rules.pro")
                )
            }

            getByName("debug") {
                applicationIdSuffix = ".debug"
                isMinifyEnabled = false
                isDebuggable = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    file("proguard-rules.pro")
                )
                if (rootScope is BaseAppModuleExtension) signingConfig = signingConfigs["debug"]
            }
        }

        sourceSets {
            getByName("main") {
                java.setSrcDirs(setOf("src/main/kotlin"))
            }
            getByName("test") {
                java.srcDir("src/test/kotlin")
            }
            getByName("androidTest") {
                java.srcDir("src/androidTest/kotlin")
            }
        }

        testOptions {
            unitTests.isReturnDefaultValues = true
        }

        (this as ExtensionAware).extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions>(
            "kotlinOptions"
        ) {
            jvmTarget = "11"
            if (isCompose) freeCompilerArgs = freeCompilerArgs + arrayOf(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true"
            )
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }

        if (isCompose) composeOptions {
            kotlinCompilerExtensionVersion = "1.3.2"
        }
    }

    tasks {
        withType<KotlinCompile> {
            @Suppress("SpellCheckingInspection")
            kotlinOptions {
                jvmTarget = "11"
                freeCompilerArgs = listOf(
                    "-Xallow-result-return-type",
                    "-opt-in=kotlin.RequiresOptIn",
                    "-Xcontext-receivers",
                )
                if (project.name.startsWith("bonx-sdk")) {
                    freeCompilerArgs = freeCompilerArgs + listOf("-Xexplicit-api=strict")
                }
            }
        }

        val coroutines: (String) -> String by extra
        configurations.all {
            resolutionStrategy {
                failOnVersionConflict()
                preferProjectModules()
                force(coroutines("core"), coroutines("android"))
            }
        }
    }

    //default dependencies
    val composeBomVersion: String by extra

    val mockk: String by extra
    val spek: (String) -> String by extra
    val junitJupiter: (String) -> String by extra

    val androidJunit5TestRunner: String by extra
    val xJunit: String by extra
    val xTestRunner: String by extra
    val xEspresso: String by extra


    dependencies {
        fun testImplementation(notation: Any) = add("testImplementation", notation)
        fun testRuntimeOnly(notation: Any) = add("testImplementation", notation)

        testImplementation(kotlin("test"))
        testImplementation(kotlin("reflect"))
        testImplementation(mockk)
        testImplementation(spek("dsl-jvm"))
        testImplementation(spek("runner-junit5"))
        testImplementation(junitJupiter("api"))
        testRuntimeOnly(junitJupiter("engine"))

        if (!isAndroidProject) return@dependencies

        fun androidTestImplementation(notation: Any) = add("androidTestImplementation", notation)
        fun androidTestRuntimeOnly(notation: Any) = add("androidTestRuntimeOnly", notation)

        androidTestImplementation(xTestRunner)
        androidTestImplementation(xJunit)
        androidTestImplementation(xEspresso)
        androidTestRuntimeOnly(androidJunit5TestRunner)

        if (!isCompose) return@dependencies
        fun implementation(notation: Any) = add("implementation", notation)

        val composeBom = platform("androidx.compose:compose-bom:$composeBomVersion")
        implementation(composeBom)
        androidTestImplementation(composeBom)
    }
}

tasks {
    wrapper { gradleVersion = "7.6" }
}
