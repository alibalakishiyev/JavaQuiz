plugins {

    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)


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

    dependencies {

        implementation(libs.appcompat)
        implementation(libs.material)
        implementation(libs.activity)
        implementation(libs.constraintlayout)
        implementation(libs.navigation.fragment)
        implementation(libs.navigation.ui)
        testImplementation(libs.junit)
        androidTestImplementation(libs.ext.junit)
        androidTestImplementation(libs.espresso.core)
        implementation(libs.play.services.ads.lite)



    }
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
    implementation("com.google.android.gms:play-services-ads-lite:23.5.0")
    implementation("com.google.android.gms:play-services-ads:23.5.0")

    // Picasso for image loading
    implementation("com.squareup.picasso:picasso:2.8")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")

    // Other Libraries
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
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

    //chat bot
    implementation ("com.google.code.gson:gson:2.8.9")
}


