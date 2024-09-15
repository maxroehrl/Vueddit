buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.5.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.8.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
