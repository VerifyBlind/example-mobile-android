import java.util.Properties

plugins {
    id("com.android.application") version "8.2.2"
    id("org.jetbrains.kotlin.android") version "1.9.22"
}

// release-demo.bat tarafindan yonetilir. versionName patch dondurulmus;
// versionCode app/version.properties'te tutulur ve her release'te otomatik artar.
val currentVersionName = "1.0.0"

android {
    namespace = "com.verifyblind.example"
    compileSdk = 36

    // --- Properties dosyalarından oku ---
    val props = Properties()

    // 1. verifyblind.properties (test app'e özel, bu klasörde olmalı)
    val verifyblindFile = rootProject.file("verifyblind.properties")
    if (verifyblindFile.exists()) {
        verifyblindFile.inputStream().use { props.load(it) }
    }

    // 2. Varsa local.properties ile üzerine yaz (SDK path + cihaz bazlı ayarlar)
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { props.load(it) }
    }

    // 3. versionCode'u app/version.properties'ten oku (release-demo.bat artirir)
    val versionPropsFile = file("version.properties")
    val appVersionCode = if (versionPropsFile.exists()) {
        val vp = Properties()
        versionPropsFile.inputStream().use { vp.load(it) }
        (vp.getProperty("versionCode") ?: "1").trim().toInt()
    } else 1

    defaultConfig {
        applicationId = "com.verifyblind.example"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = currentVersionName

        // URL'ye bağlı olmayan sabit alanlar
        val appLinkBase  = props.getProperty("VERIFYBLIND_APP_LINK_BASE") ?: ""
        val certPins     = props.getProperty("verifyblind.certificatePins") ?: ""
        val generateEndp = props.getProperty("VERIFYBLIND_GENERATE_ENDPOINT") ?: "generate"
        val verifyEndp   = props.getProperty("VERIFYBLIND_VERIFY_ENDPOINT") ?: "verify"

        buildConfigField("String", "VERIFYBLIND_APP_LINK_BASE", "\"$appLinkBase\"")
        buildConfigField("String", "VERIFYBLIND_CERTIFICATE_PINS", "\"$certPins\"")
        buildConfigField("String", "VERIFYBLIND_GENERATE_ENDPOINT", "\"$generateEndp\"")
        buildConfigField("String", "VERIFYBLIND_VERIFY_ENDPOINT", "\"$verifyEndp\"")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    buildTypes {
        debug {
            // USE_LOCAL_API=true → lokal Docker (test portal :3001, relay :5102); false → EC2
            val useLocalApi = props.getProperty("USE_LOCAL_API") == "true"
            val partnerUrl = if (useLocalApi)
                props.getProperty("VERIFYBLIND_PARTNER_BACKEND_URL_LOCAL") ?: ""
            else
                props.getProperty("VERIFYBLIND_PARTNER_BACKEND_URL") ?: ""
            val apiUrl = if (useLocalApi)
                props.getProperty("VERIFYBLIND_API_URL_LOCAL") ?: ""
            else
                props.getProperty("VERIFYBLIND_API_URL") ?: "https://api.verifyblind.com"
            buildConfigField("String", "VERIFYBLIND_PARTNER_BACKEND_URL", "\"$partnerUrl\"")
            buildConfigField("String", "VERIFYBLIND_API_URL", "\"$apiUrl\"")
            buildConfigField("Boolean", "USE_LOCAL_API", "$useLocalApi")
        }
        release {
            // Release her zaman production URL kullanır
            val partnerUrl = props.getProperty("VERIFYBLIND_PARTNER_BACKEND_URL") ?: ""
            buildConfigField("String", "VERIFYBLIND_PARTNER_BACKEND_URL", "\"$partnerUrl\"")
            buildConfigField("String", "VERIFYBLIND_API_URL", "\"${props.getProperty("VERIFYBLIND_API_URL") ?: "https://api.verifyblind.com"}\"")
            buildConfigField("Boolean", "USE_LOCAL_API", "false")
            // Play DEX code optimization gerekliligi (Sub 2027). Ana uygulamadan farkli olarak
            // burada obfuscation KAPATILMIYOR: demo'nun kod-seffafligi iddiasi yok.
            isMinifyEnabled = true
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
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":verifyblind"))  // VerifyBlind SDK (local modül)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.browser:browser:1.8.0")

    testImplementation("junit:junit:4.13.2")
}
