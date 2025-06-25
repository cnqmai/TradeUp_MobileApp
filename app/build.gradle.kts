plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    id ("androidx.navigation.safeargs")
}

android {
    namespace = "com.example.tradeup"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tradeup"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.legacy.support.v4)
    implementation(libs.recyclerview)
    implementation(libs.gridlayout)

    // Firebase
    implementation(libs.firebase.database)
    implementation("com.google.firebase:firebase-auth:23.0.0")
    implementation("com.google.firebase:firebase-storage:21.0.0")
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))

    // GeoFire
    implementation("com.firebase:geofire-android:3.2.0")

    // Google Sign-In
    implementation ("com.google.android.gms:play-services-auth:21.2.0")

    // Google Play Services Location (Bắt buộc cho FusedLocationProviderClient)
    implementation ("com.google.android.gms:play-services-location:21.0.1")

    // Cloudinary Storage
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")

    // Apache Commons IO
    implementation ("commons-io:commons-io:2.13.0")

    // SONObject để parse JSON (nếu chưa có)
    implementation ("org.json:json:20231013")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Picasso
    implementation("com.squareup.picasso:picasso:2.71828")

    // ThreeTenABP
    implementation ("com.jakewharton.threetenabp:threetenabp:1.4.6")

    // Circle ImageView
    implementation ("de.hdodenhof:circleimageview:3.1.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}