buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.10.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.9.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
