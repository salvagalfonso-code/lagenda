plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.agendavalenciana"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.agendavalenciana.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 340
        versionName = "0.20.8.340"

        val configuredCsvUrl = providers.gradleProperty("AGENDA_EVENTS_CSV_URL").orElse("").get().trim()
        val remoteEventsCsvUrl = (configuredCsvUrl.ifBlank { "https://quimerafest.com/app/data/events.csv" })
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "REMOTE_EVENTS_CSV_URL", "\"$remoteEventsCsvUrl\"")

        val configuredBaseUrl = providers.gradleProperty("AGENDA_REMOTE_BASE_URL").orElse("").get().trim()
        val remoteAgendaBaseUrl = (configuredBaseUrl.ifBlank { "https://quimerafest.com/app/" })
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "REMOTE_AGENDA_BASE_URL", "\"$remoteAgendaBaseUrl\"")

        val configuredApiUrl = providers.gradleProperty("AGENDA_API_BASE_URL").orElse("").get().trim()
        val remoteApiBaseUrl = (configuredApiUrl.ifBlank { "https://quimerafest.com/app/api/" })
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "REMOTE_API_BASE_URL", "\"$remoteApiBaseUrl\"")

        // OAuth Web client ID de Google. És un identificador públic, no un secret.
        val configuredGoogleWebClientId = providers.gradleProperty("AGENDA_GOOGLE_WEB_CLIENT_ID").orElse("").get().trim()
        val googleWebClientId = configuredGoogleWebClientId
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")

        buildConfigField("boolean", "IS_PRO_APP", "false")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}


dependencies {
    implementation(project(":shared"))
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.1")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("com.google.zxing:core:3.5.4")
    implementation("com.stripe:stripe-android:23.20.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
