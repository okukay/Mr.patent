plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id ("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.ssafy.mr_patent_android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ssafy.mr_patent_android"
        minSdk = 26
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
    kotlinOptions {
        jvmTarget = "11"
    }
    viewBinding {
        enable= true
    }
    dataBinding {
        enable = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.places)
    implementation(libs.firebase.messaging.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.viewbinding)


    // https://github.com/ybq/Android-SpinKit
    implementation("com.github.ybq:Android-SpinKit:1.4.0")

    // https://github.com/square/retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // https://github.com/square/okhttp
    implementation("com.squareup.okhttp3:okhttp:4.9.0")

    // https://github.com/square/retrofit/tree/master/retrofit-converters/gson
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // https://github.com/square/okhttp/tree/master/okhttp-logging-interceptor
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")

    implementation("com.github.hannesa2:AndroidSlidingUpPanel:4.2.1")
    val lifecycle_version = "2.8.7"
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    // LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")
    // Lifecycles only (without ViewModel or LiveData)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version")
    // Saved state module for ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycle_version")

    //Jetpack Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.9")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.9")

    //Glide
    implementation ("com.github.bumptech.glide:glide:4.16.0")

    //STOMP
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")

    //RxJava
    implementation ("io.reactivex.rxjava2:rxjava:2.2.5")
    implementation ("io.reactivex.rxjava2:rxandroid:2.1.0")

    //Firebase
    implementation("com.google.firebase:firebase-bom:33.11.0")

    //sse
    implementation("com.launchdarkly:okhttp-eventsource:4.1.0")

    //EasyFlipView
    implementation ("com.wajahatkarim:EasyFlipView:3.0.3")

    implementation ("io.github.ParkSangGwon:tedimagepicker:1.4.3")

    implementation ("io.github.ParkSangGwon:tedpermission-normal:3.3.0")

    implementation ("com.airbnb.android:lottie:6.3.0")

}
