plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    // Capturas de tela das telas em Compose, geradas em testes JVM (sem emulador).
    alias(libs.plugins.paparazzi)
}

// Versão do app.
// - Lançamento: o workflow de release define RELEASE_TAG (ex.: "v1.2.3") a partir da tag do Git;
//   versionName = "1.2.3".
// - Qualquer outro build (CI de cada push, Android Studio): versionName = "<appVersion>-dev[+N]",
//   onde appVersion vem do gradle.properties e N é o número da execução no CI (BUILD_NUMBER).
// versionCode = major * 10000 + minor * 100 + patch (1.2.3 -> 10203): sempre cresce junto com a
// versão, como o Play exige, e não depende de quantas vezes o CI rodou.
val appVersion: String = providers.gradleProperty("appVersion").get()
val releaseVersion: String? = System.getenv("RELEASE_TAG")?.removePrefix("v")?.takeIf { it.isNotBlank() }
val buildNumber: String? = System.getenv("BUILD_NUMBER")?.takeIf { it.isNotBlank() }

fun versionCodeOf(version: String): Int {
    val match = Regex("""^(\d+)\.(\d+)\.(\d+)$""").matchEntire(version)
        ?: throw GradleException("Versão inválida: '$version' (use o formato 1.2.3)")
    val (major, minor, patch) = match.destructured.toList().map { it.toInt() }
    if (minor > 99 || patch > 99) throw GradleException("Versão $version: minor e patch vão de 0 a 99")
    return major * 10_000 + minor * 100 + patch
}

if (releaseVersion != null && releaseVersion != appVersion) {
    throw GradleException(
        "A tag v$releaseVersion não bate com appVersion=$appVersion no gradle.properties. " +
            "Atualize o gradle.properties antes de criar a tag.",
    )
}

android {
    namespace = "com.tomaesseblock"
    // Google Play exige targetSdk 36 (Android 16) para apps novos desde 31/08/2026.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tomaesseblock"
        // CallScreeningService + RoleManager.ROLE_CALL_SCREENING exigem Android 10 (API 29).
        minSdk = 29
        targetSdk = 36
        versionCode = versionCodeOf(appVersion)
        versionName = releaseVersion ?: (appVersion + "-dev" + (buildNumber?.let { "+$it" } ?: ""))
    }

    // Chave de upload do Play: lida de variáveis de ambiente (secrets do GitHub), nunca do repositório.
    // Sem elas, o pacote de release é gerado sem assinatura (útil só para validar o build).
    val releaseKeystore = System.getenv("RELEASE_KEYSTORE_PATH")
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
    buildFeatures {
        compose = true
        // BuildConfig.VERSION_NAME / VERSION_CODE, mostrados em Ajustes → Sobre.
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    // org.json real nos testes JVM (no android.jar ela é só um stub).
    testImplementation(libs.org.json)
    testImplementation(libs.kotlinx.coroutines.test)
}
