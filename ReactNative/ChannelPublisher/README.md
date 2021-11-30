# MobileChannelPublisher

Simple ReactNative Phenix channel publisher, based on Phenix WebSDK

# Channel Publisher Example Application
This application shows how to subscribe to a channel using the `Phenix Channel Express API`.

This example application demonstrates how to:
1. Enter AuthToken and PublishToken
2. Join channel
3. Publish stream from front or rear mobile camera

For more details and additional features, please refer to our `Channel Express API` documentation.

## How to Install
Required:
* node: 12.18.2
* npm: 6.14.5

1. cd ReactNative/ChannelPublisher
2. `npm install`

## iOS:
1. cd ReactNative/ChannelPublisher/ios
2. `pod install`
3. Open ChannelPublisher.xcworkspace with Xcode
4. Navigate to Project Settings - Signing & Capabilities
5. Select your development team
### This project was started on Xcode 12.0, COCOAPODS 1.10.2.

## How to Run iOS
1. cd ReactNative/ChannelPublisher:
2. `npm run start`
3. `npm run ios` or `react-native run-ios`

## Android
In Android Manifest, if it doesn't exist, add:

<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.CAMERA"/>
<uses-permission android:name="android.permission.FLASHLIGHT"/>
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS"/>
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.RECORD_VIDEO"/>
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
<uses-permission android:name="android.permission.WAKE_LOCK"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>

Reference: https://github.com/react-native-webrtc/react-native-webrtc/blob/master/Documentation/AndroidInstallation.md

In some cases in gradle.properties add:
`android.enableDexingArtifactTransform.desugaring=false`

1. Start Android Studio
2. Select ChannelPublisher project
3. Check android/local.properties
4. Check /android/app/debug.keystore, if it doesn't exist add or generate

## How to Run Android
1. cd ReactNative/ChannelPublisher
2. `npm run start`
3. `npm run android` or `react-native run-android`

## See Also
### Related Examples
* [Mobile Channel Viewer](https://github.com/PhenixRTS/MobileChannelViewer)
* [Web Examples](https://github.com/PhenixRTS/WebExamples)

### Documentation
* [Channel Publisher Tutorial](https://phenixrts.com/docs/web/react-native/#web-sdk-react-native-example)
* [Phenix Channel Express API](https://phenixrts.com/docs/web/#channel-express)
* [React Native Support](https://phenixrts.com/docs/web/#react-native-support)
* [Phenix Platform Documentation](http://phenixrts.com/docs/)
