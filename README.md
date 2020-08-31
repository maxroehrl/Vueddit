# Vueddit

A Reddit client for Android built with NativeScript-Vue.

## Prerequisites

* [NodeJS with npm](https://nodejs.org/en/download/current/)
* [Android SDK](https://developer.android.com/studio)
* [Java SE Development Kit 8](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html)
* [Nativescript CLI](https://github.com/NativeScript/nativescript-cli) `npm install -g nativescript`

## Usage

``` bash
# Install dependencies
npm install

# Prepare android build environment
tns platform add android

# Build, watch for changes and debug the application
tns debug android

# Update app icon
tns resources generate icons icon.png

# Update splash image
tns resources generate splashes splash.png --background #53ba82

# Build a release bundle
# tns build android --release --key-store-path "XXX.jks" --key-store-password XXX --key-store-alias XXX --key-store-alias-password XXX --aab
```

## Libraries used

* [android-customtabs](https://github.com/saschpe/android-customtabs)
* [Markwon](https://github.com/noties/Markwon)
