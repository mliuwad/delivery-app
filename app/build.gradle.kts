import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use {
        localProperties.load(it)
    }
}

val imgbbApiKey = localProperties.getProperty("IMGBB_API_KEY") ?: ""

android {
    namespace = "com.example.udgh"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.udgh"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "IMGBB_API_KEY",
            "\"$imgbbApiKey\""
        )
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    // ✅ THÊM PACKAGING OPTIONS ĐẦY ĐỦ
    packagingOptions {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "com/j256/ormlite/core/README.txt"
            excludes += "**/README.txt"
            excludes += "**/LICENSE.txt"
            excludes += "**/NOTICE.txt"
            excludes += "**/builddef.lst"
        }
        // ✅ Thêm pickFirsts cho các file conflict
        pickFirsts += "**/module-info.class"
        pickFirsts += "META-INF/versions/9/module-info.class"
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // ✅ Material Design
    implementation("com.google.android.material:material:1.12.0")

    // ✅ Firebase
    implementation("com.google.firebase:firebase-auth:24.0.1")

    // ✅ Glide - Load ảnh
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

    // ✅ SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // ✅ OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ✅ OpenStreetMap (osmdroid)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    //implementation("org.osmdroid:osmdroid-mapsforge:6.1.18")
    //implementation("org.osmdroid:osmdroid-geopackage:6.1.18")

    // ✅ OSMBonusPack
    implementation("com.github.MKergall:osmbonuspack:6.9.0")


    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.7")

}