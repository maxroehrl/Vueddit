# Vueddit

A Reddit client for Android built with NativeScript-Vue.

## Prerequisites

* [NodeJS with npm](https://nodejs.org/en/download/current/)
* [Android SDK 28](https://developer.android.com/studio)
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
```

## Libraries used

* [android-customtabs](https://github.com/saschpe/android-customtabs)
* [Markwon](https://github.com/noties/Markwon)
