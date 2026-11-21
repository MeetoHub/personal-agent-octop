plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.octop.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.octop.mobile"
        minSdk = 28
        targetSdk = 35
        versionCode = 5
        versionName = "0.3.1"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/LICENSE.md",
                "META-INF/NOTICE.md"
            )
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // WebSocket（对话流 / 通知流）
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // 手机能力桥的嵌入式 HTTP server（给 NAS 上的 agent 调）
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    // token 加密落盘（Keystore 支持的 EncryptedSharedPreferences）
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // ── 接 droid-mcp（可选，默认注释，见 docs/接入-droid-mcp.md）─────────────
    // implementation("com.github.stixez.droid-mcp:droid-mcp-core:0.15.0")
    // implementation("com.github.stixez.droid-mcp:droid-mcp-shizuku:0.15.0")
    // implementation("com.github.stixez.droid-mcp:droid-mcp-screenshot:0.15.0")
    // implementation("com.github.stixez.droid-mcp:droid-mcp-accessibility:0.15.0")
    // implementation("com.github.stixez.droid-mcp:droid-mcp-contacts:0.15.0")
    // implementation("com.github.stixez.droid-mcp:droid-mcp-sms:0.15.0")
    // implementation("com.github.stixez.droid-mcp:droid-mcp-alarms:0.15.0")
}
