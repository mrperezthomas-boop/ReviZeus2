// [2026-04-22 00:00][ASSAINISSEMENT_GLOBAL] Harmonisation versions AGP/Kotlin/KSP avec la stack officielle pour stabiliser la sync Gradle.
plugins {
    id("com.android.application") version "9.0.1" apply false
    id("com.android.library") version "9.0.1" apply false
    id("org.jetbrains.kotlin.android") version "2.3.10" apply false
    id("com.google.devtools.ksp") version "2.3.10-2.0.2" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
}
