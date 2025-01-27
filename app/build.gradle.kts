plugins {
    id("com.android.application")
    id("kotlin-android")
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-parcelize")
}

android {
    signingConfigs {
        create("Play") {
            storeFile = file("key.jks")
            storePassword = "1234567890"
            keyAlias = "debugKey"
            keyPassword = "1234567890"
        }
        create("Debug") {
            storeFile = file("key.jks")
            storePassword = "1234567890"
            keyAlias = "debugKey"
            keyPassword = "1234567890"
        }
    }
    compileSdk = 35
    buildToolsVersion = "35.0.1"

    defaultConfig {
        applicationId = "de.max.roehrl.vueddit2"
        minSdk = 21
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {
            applicationIdSuffix = ".play"
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("Play")
            manifestPlaceholders["appName"] = "Vueddit 2"
        }
        getByName("debug") {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appName"] = "Vueddit 2"
            signingConfig = signingConfigs.getByName("Debug")
        }
    }
    namespace = "de.max.roehrl.vueddit2"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.7")
    implementation("androidx.media3:media3-exoplayer-dash:1.5.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.1")
    implementation("androidx.media3:media3-exoplayer-smoothstreaming:1.5.1")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("de.peilicke.sascha:android-customtabs:3.1.0")
    implementation("com.android.volley:volley:1.2.1")
    implementation("io.coil-kt:coil:2.7.0")
    implementation("androidx.browser:browser:1.8.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
