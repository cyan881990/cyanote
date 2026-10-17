plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "vn.ghichunhanh.app"
    compileSdk = 36

    defaultConfig {
        // Đổi applicationId thành tên riêng của bạn trước khi đăng Play Store
        // (nhớ đổi cả targetPackage trong res/xml/shortcuts.xml).
        applicationId = "app.cyanote"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    // Ký bản phát hành bằng keystore của bạn (dùng khi đăng Google Play).
    // Trên GitHub Actions, đặt các secret CYANOTE_KEYSTORE_BASE64, CYANOTE_KEYSTORE_PASSWORD,
    // CYANOTE_KEY_ALIAS, CYANOTE_KEY_PASSWORD; không có thì bản release được build nhưng chưa ký.
    val keystorePath = System.getenv("CYANOTE_KEYSTORE")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("CYANOTE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("CYANOTE_KEY_ALIAS")
                keyPassword = System.getenv("CYANOTE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
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

    // Tự tạo danh sách ngôn ngữ để Android 13+ cho chọn ngôn ngữ riêng cho app trong Cài đặt.
    androidResources {
        generateLocaleConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Quảng cáo Google AdMob
    implementation("com.google.android.gms:play-services-ads:23.6.0")
}
