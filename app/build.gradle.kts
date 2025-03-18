plugins {

    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.ali.javaquizbyali"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ali.javaquizbyali"
        minSdk = 24
        targetSdk = 34
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
    implementation(libs.play.services.ads)
    implementation(libs.google.material)
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.interactivemedia)
    implementation(libs.firebase.database)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
//    code
    implementation ("org.codehaus.janino:janino:3.1.6")


}

