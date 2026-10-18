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
        // Trên GitHub Actions, số build tự tăng mỗi lần chạy → dùng làm versionCode để app biết có bản mới.
        val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
        versionCode = runNumber ?: 1
        versionName = if (runNumber != null) "1.1.$runNumber" else "1.1"
    }

    // Ký bản phát hành bằng keystore của bạn (dùng khi đăng Google Play).
    // Trên GitHub Actions, đặt các secret CYANOTE_KEYSTORE_BASE64, CYANOTE_KEYSTORE_PASSWORD,
    // CYANOTE_KEY_ALIAS, CYANOTE_KEY_PASSWORD; không có thì bản release được build nhưng chưa ký.
    val keystorePath = System.getenv("CYANOTE_KEYSTORE")
    signingConfigs {
        // Khóa ký CỐ ĐỊNH cho bản thử (debug), nằm trong kho code. Nhờ vậy bản build sau
        // cài đè được lên bản trước mà không mất ghi chú. Không dùng khóa này cho Google Play.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
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
        // Quảng cáo: bản debug/qa dùng ID THỬ của Google (an toàn khi tự bấm thử);
        // chỉ bản release (đăng Google Play) dùng ID AdMob thật của cyanote++.
        getByName("debug") {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "AD_BANNER", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "AD_MREC", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "AD_INTERSTITIAL", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("boolean", "SELF_UPDATE", "true")
        }
        release {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-4415627801655056~2996924261"
            buildConfigField("String", "AD_BANNER", "\"ca-app-pub-4415627801655056/5647138339\"")
            buildConfigField("String", "AD_MREC", "\"ca-app-pub-4415627801655056/5647138339\"")
            buildConfigField("String", "AD_INTERSTITIAL", "\"ca-app-pub-4415627801655056/6286441744\"")
            buildConfigField("boolean", "SELF_UPDATE", "false")
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Bản kiểm thử giống hệt bản phát hành (R8 thu gọn, tối ưu) nhưng ký bằng khoá thử để cài được trên máy ảo
        create("qa") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isProfileable = true
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "AD_BANNER", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "AD_MREC", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "AD_INTERSTITIAL", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("boolean", "SELF_UPDATE", "false")
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
        buildConfig = true
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
    // Play Core / AdMob kéo theo Fragment cũ; cần >= 1.3 cho ActivityResult API
    implementation("androidx.fragment:fragment-ktx:1.8.8")
    implementation("androidx.exifinterface:exifinterface:1.4.1")
    // Cài sẵn hồ sơ tối ưu (Baseline Profile) của Compose → mở app và cuộn mượt hơn ngay lần đầu
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Cập nhật ngay trong app khi cài từ Google Play
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // Quảng cáo Google AdMob
    implementation("com.google.android.gms:play-services-ads:23.6.0")
}
