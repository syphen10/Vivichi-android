buildscript {
    repositories {
        google()
    }
    dependencies {
        // AGP 8.10's bundled R8 can't read Kotlin 2.3 metadata (needed by the Ads/Billing
        // libraries), and newer AGP 8.x doesn't run on this Gradle version — so pin a newer R8.
        classpath("com.android.tools:r8:8.13.23")
    }
}

plugins {
    id("com.android.application") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}
