import java.util.Properties
import java.io.FileInputStream
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

if (file("google-services.json").exists()) {
    pluginManager.apply("com.google.gms.google-services")
}

val keystorePropsFile = rootProject.file("release.properties")
val keystoreProps = Properties()

if (keystorePropsFile.exists()) {
    FileInputStream(keystorePropsFile).use {
        keystoreProps.load(it)
    }
}

val hasValidSigningProps = listOf(
    "storeFile",
    "storePassword",
    "keyAlias",
    "keyPassword"
).all { key ->
    keystoreProps.getProperty(key) != null
}

android {
    namespace = "com.numberone.numberone"
    compileSdk = 36

    lint {
        checkReleaseBuilds = false
    }

    signingConfigs {
        if (hasValidSigningProps) {
            create("release") {
                storeFile = rootProject.file(
                    keystoreProps.getProperty("storeFile")
                )
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = "com.numberone.numberone"
        minSdk = 26
        targetSdk = 36
        versionCode = 20
        versionName = "1.3.9"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            if (hasValidSigningProps) {
                signingConfig = signingConfigs.getByName("release")
            }

            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            excludes.add("META-INF/kotlinx_coroutines_core.version")
            excludes.add("DebugProbesKt.bin")
            excludes.add("**/*.kotlin_builtins")

            pickFirsts.add("nonJvmMain/default/linkdata/package_androidx/0_androidx.knm")
            pickFirsts.add("nonJvmMain/default/linkdata/root_package/0_.knm")
            pickFirsts.add("nonJvmMain/default/linkdata/module")

            pickFirsts.add("nativeMain/default/linkdata/root_package/0_.knm")
            pickFirsts.add("nativeMain/default/linkdata/module")

            pickFirsts.add("commonMain/default/linkdata/root_package/0_.knm")
            pickFirsts.add("commonMain/default/linkdata/module")
            pickFirsts.add("commonMain/default/linkdata/package_androidx/0_androidx.knm")

            pickFirsts.add("META-INF/kotlin-project-structure-metadata.json")

            merges.add("commonMain/default/manifest")
            merges.add("nonJvmMain/default/manifest")
            merges.add("nativeMain/default/manifest")
        }
    }

    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:2.3.21")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.3.21")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.3.21")

            force("androidx.annotation:annotation:1.8.1")

            eachDependency {
                if (requested.group == "androidx.lifecycle") {
                    useVersion("2.9.0")
                    because("Keep Lifecycle libraries compatible with compileSdk 36")
                }
            }
        }
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
}

kotlin {
    compilerOptions {
        jvmTarget.set(
            org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("17")
        )
    }
}

dependencies {

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.collection.ktx)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.functions)

    // Ktor
    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    // Firebase Cloud Messaging (google-services.json is supplied per Firebase project)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)

    // Kotlin
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

tasks.matching { it.name.contains("AarMetadata") }.configureEach { enabled = false }
