# mapbox-geography-quiz-demo

This demo shows how gaming can be combined with the Mapbox Maps SDK for Android. Using a list of cities around the world, a one or two player game geography guessing game can be built on top of a Mapbox map.

## Instructions to open this project on your Android device

1. Clone this repo and open the project in [Android Studio **3.0**](https://developer.android.com/studio/preview/index.html). 

2. Retrieve your Mapbox account's _default public token_, which can be found at [mapbox.com/account/access-tokens](https://www.mapbox.com/account/access-tokens/).

3. Find [this project's `strings.xml` file](https://github.com/mapbox/mapbox-voice-runtime-demo/blob/master/app/src/main/res/values/strings.xml) in Android Studio and paste your Mapbox default public token in [the string resource that's already in the `strings.xml` file](https://github.com/mapbox/mapbox-geography-quiz-demo/blob/master/TapperGeoChallenge/app/src/main/res/values/strings.xml#L3):
```<string name="mapbox_access_token">PASTE_YOUR_MAPBOX_TOKEN_HERE</string>```

4. You're all set, so run the app from Android Studio to install it on your Android device!


**One-player game:**
<br>
![](https://user-images.githubusercontent.com/4394910/29583155-24504c7c-874d-11e7-9569-b84aac1eeaa9.gif)

**Two-player game:**
<br>
![](https://user-images.githubusercontent.com/4394910/29583156-2450986c-874d-11e7-8546-cd62ba9ca2d6.gif)
