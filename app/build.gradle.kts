import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.google.gms.google-services")
}

/**
 * Lecture locale de la clé Gemini depuis local.properties.
 * On garde ce mécanisme simple pour éviter de remettre la clé en dur dans le code source.
 */
val geminiApiKeyFromLocal = run {
    val props = Properties()
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { props.load(it) }
        props.getProperty("GEMINI_API_KEY")
    } else {
        null
    }
}

/**
 * Priorité :
 * 1. propriété Gradle GEMINI_API_KEY
 * 2. local.properties
 * 3. chaîne vide si rien n’est défini
 */
val geminiApiKey = providers.gradleProperty("GEMINI_API_KEY").orNull
    ?: geminiApiKeyFromLocal
    ?: ""

/**
 * Échappement minimal défensif pour injection sûre dans BuildConfig.
 */
val escapedGeminiApiKey = geminiApiKey
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.revizeus.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.revizeus.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        /**
         * La clé Gemini n’est plus codée en dur dans GeminiManager.
         * Elle est injectée via BuildConfig.
         */
        buildConfigField("String", "GEMINI_API_KEY", "\"$escapedGeminiApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        /**
         * ViewBinding obligatoire dans RéviZeus.
         */
        viewBinding = true

        /**
         * Indispensable ici car on utilise un buildConfigField personnalisé
         * pour injecter GEMINI_API_KEY.
         */
        buildConfig = true
    }
}

dependencies {
    // --- FIREBASE AUTH ---
    implementation(platform("com.google.firebase:firebase-bom:34.10.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // --- IA & GEMINI ---
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("com.google.guava:guava:33.5.0-android")
    implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")

    // --- GOOGLE SIGN-IN ---
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // --- ANDROID CORE ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity)

    // --- COIL / WEBP ANIMÉ ---
    implementation(libs.coil)
    implementation(libs.coil.gif)

    // --- MEDIA3 / EXOPLAYER ---
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // --- LIFECYCLE / COROUTINES UI ---
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // --- ML KIT & CAMERA X ---
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    val cameraxVersion = "1.5.3"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // --- ROOM DATABASE (IAristote) ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // --- LOTTIE ---
    implementation("com.airbnb.android:lottie:6.4.0")

    // --- FIREBASE FUNCTIONS ---
    implementation("com.google.firebase:firebase-functions")}