plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.chaquo.python") version "16.0.0"
}

android {
    namespace = "com.ali.systemIn"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ali.systemIn"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "0.3"


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86")
            abiFilters.add("x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
    }
    ndkVersion = "28.0.13004108"
    buildToolsVersion = "35.0.0"


}

dependencies {
    // Firebase Libraries
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))  // Firebase BOM (Bill of Materials)
    implementation("com.google.firebase:firebase-analytics-ktx")          // Firebase Analytics
    implementation("com.google.firebase:firebase-database:21.0.0")
    implementation("com.google.firebase:firebase-storage:21.0.1")
    implementation("com.google.firebase:firebase-auth:23.2.0")
    implementation("com.google.firebase:firebase-firestore:25.1.3")

    // Google Play Services and Ads
    implementation("com.google.android.gms:play-services-ads-lite:24.0.0")

    // Picasso for image loading
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // Other Libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Testing Libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Interactive media and credentials
    implementation(libs.interactivemedia)
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Code Libraries
    implementation("org.codehaus.janino:janino:3.1.6")

    // Chat bot
    implementation("com.google.code.gson:gson:2.8.9")

//    terminal
    testImplementation ("junit:junit:4.13.2")

}
