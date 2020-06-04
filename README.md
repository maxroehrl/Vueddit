# Vueddit

A Reddit client for Android built with NativeScript-Vue

## Prerequisites

* NodeJS with npm
* Android SDK 28

## Usage

``` bash
# Install dependencies
npm install

# Prepare android build environment
tns platform add android

# Build, watch for changes and debug the application
tns debug <platform>

# Update app icon
tns resources generate icons icon.png

# Update splash image
tns resources generate splashes splash.png --background #53ba82
```

## Libraries used

* [android-customtabs](https://github.com/saschpe/android-customtabs)
* [Markwon](https://github.com/noties/Markwon)
