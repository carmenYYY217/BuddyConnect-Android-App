plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "edu.cuhk.csci3310.buddyconnect"
    compileSdk = 35

    defaultConfig {
        applicationId = "edu.cuhk.csci3310.buddyconnect"
        minSdk = 34
        targetSdk = 35
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
    //for Bottom Navigation bar
    buildFeatures {
        dataBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    implementation ("com.google.firebase:firebase-storage:20.3.0") // Check for the latest version
    implementation ("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.12.0")


    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // Cloud Storage SDK
    // implementation("com.google.firebase:firebase-storage")

    // Volley for networking
    implementation("com.android.volley:volley:1.2.1")

    // Add the dependencies for any other desired Firebase products
    // https://firebase.google.com/docs/android/setup#available-libraries


    // ChatBot
    // Volley for networking (GitHub: https://google.github.io/volley/)
    implementation("com.android.volley:volley:1.2.1")

    // Material Design (https://mvnrepository.com/artifact/com.google.android.material/material)
    implementation("com.google.android.material:material:1.13.0-alpha11")

    //Bottom Navigation Bar (Github: https://github.com/Foysalofficial/NafisBottomNav)
    implementation ("com.github.Foysalofficial:NafisBottomNav:5.0")

    //ViewPager 2
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    //Bookmark
    implementation ("com.google.android.material:material:1.12.0")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation ("androidx.cardview:cardview:1.0.0")

    //Scheduler
    // RecyclerView for task lists
    implementation ("androidx.recyclerview:recyclerview:1.2.1")

    // MaterialCalendarView for weekly scheduler
    implementation ("com.github.prolificinteractive:material-calendarview:2.0.1")

    // Room for task storage
    implementation ("androidx.room:room-runtime:2.6.1")
    annotationProcessor ("androidx.room:room-compiler:2.6.1")
    implementation ("com.jakewharton.threetenabp:threetenabp:1.3.1")

    //Map
    implementation ("org.osmdroid:osmdroid-android:6.1.10")
    implementation ("com.google.android.gms:play-services-location:21.0.1")
}