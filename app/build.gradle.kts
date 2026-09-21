import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    // Kotlin 2.x ships the Compose compiler as a plugin (replaces composeOptions).
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.vivichi.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vivichi.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 14
        versionName = "1.4.4"
        vectorDrawables.useSupportLibrary = true

        // AdMob IDs live in gradle.properties so real ones can be swapped in without touching
        // code. Defaults are Google's public test IDs, which always serve test ads.
        val admobAppId = (project.findProperty("vivichi.admobAppId") as String?) ?: "ca-app-pub-3940256099942544~3347511713"
        val rewardedUnit = (project.findProperty("vivichi.rewardedUnitId") as String?) ?: "ca-app-pub-3940256099942544/5224354917"
        manifestPlaceholders["admobAppId"] = admobAppId
        val bannerUnit = (project.findProperty("vivichi.bannerUnitId") as String?) ?: "ca-app-pub-3940256099942544/9214589741"
        buildConfigField("String", "REWARDED_UNIT_ID", "\"$rewardedUnit\"")
        buildConfigField("String", "BANNER_UNIT_ID", "\"$bannerUnit\"")
        buildConfigField("String", "PREMIUM_PRODUCT_ID", "\"vivichi_premium\"")
    }

    signingConfigs {
        create("release") {
            if (keystoreProps.containsKey("storeFile")) {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Real release keystore (own identity cert) — required for Amazon Appstore / Play
            // Store submission, which reject an app signed with the debug certificate.
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            // Debug builds always use test ads, even once real IDs are configured — clicking
            // your own live ads can get the AdMob account suspended.
            buildConfigField("String", "REWARDED_UNIT_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "BANNER_UNIT_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Rewarded ads (+ Google's consent form for EEA/UK users) and the one-time Premium purchase.
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    implementation("com.android.billingclient:billing:9.1.0")


    // Renders bundled Twemoji SVG assets (res/raw) for a consistent look across all devices,
    // instead of relying on whatever emoji font each OEM/Android version happens to ship.
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-svg:2.6.0")
    // Synchronous SVG->Canvas rendering for the streak-share bitmap (drawn off the Compose
    // tree onto a raw android.graphics.Canvas, where Coil's async pipeline doesn't fit).
    implementation("com.caverock:androidsvg-aar:1.4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
