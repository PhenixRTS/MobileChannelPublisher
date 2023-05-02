# Phenix Deeplink

Support framework providing necessary functionality to parse deep links.

## Requirements
* iOS 13.0+
* Xcode 12.5.1+
* Swift 5.4+

## Installation

### CocoaPods (using Development Pods)

[CocoaPods](https://cocoapods.org) is a dependency manager for Swift and Objective-C Cocoa projects.
For usage and installation instructions, visit their website.

To integrate `PhenixDeeplink` into your Xcode project using CocoaPods:

1. Move `PhenixDeeplink` directory inside your iOS project root directory.

2. Modify your `Podfile`:

```ruby
source 'https://cdn.cocoapods.org/'
source 'git@github.com:PhenixRTS/CocoaPodsSpecs.git' # Phenix private repository

target 'your app name'
  use_frameworks!
  pod 'PhenixDeeplink', :path => './PhenixDeeplink'
end
```

3. Install `Podfile` dependencies:

```shell
foo@bar Demo % pod install
```

## Usage

1. Choose which deep link model to use.

1.1. Use default deep link model `PhenixDeeplinkModel`,
which contains all the possible deep link properties, like, `authToken`, `publishToken`, etc.

1.2. Create a new custom deep link model, which will know how to parse the provided URL parameters.
This model must conform to the `PhenixDeeplinkUrlModelRepresentable` protocol.

Here is an example of custom deep link model for URL like  `https://{host}?token=DIGEST:eyJhcHBsaWNhdGlvb#test`:

```swift
import PhenixDeeplink

struct ExampleDeeplinkModel: PhenixDeeplinkModelProvider {
    let alias: String?
    let uri: URL?
    let backend: URL?

    init?(components: URLComponents) {
        self.alias = components.fragment

        if let string = components.queryItems?.first(where: { $0.name == "uri" })?.value {
            self.uri = URL(string: string)
        }

        if let string = components.queryItems?.first(where: { $0.name == "backend" })?.value {
            self.backend = URL(string: string)
        }
    }
}
```

2. In the *AppDelegate.swift*, import the `PhenixDeeplink` framework

```swift
import PhenixDeeplink
```

3. In the *AppDelegate.swift* inside the method `func application(_:didFinishLaunchingWithOptions:) -> Bool` make a deep-link instance:

```swift
func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
    ...

    // Setup deeplink
    if let deeplink = PhenixDeeplinkService<PhenixDeeplinkModel>.makeDeeplink(launchOptions) {
        ...
    }

    return true
}
```

4. In the *AppDelegate.swift* inside the method `func application(_:continue:restorationHandler:) -> Bool` generate a deep-link instance:

```swift
func application(_ application: UIApplication, continue userActivity: NSUserActivity, restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {
    guard let deeplink = PhenixDeeplinkService<PhenixDeeplinkModel>.makeDeeplink(userActivity) else {
        return false
    }

    ...
}
```

After these steps, each time the application will be launched from Xcode, it will use the injected deep link URL.
If the application will be launched manually by tapping on the app icon in the device/simulator,
it will not use the Xcode environments and will open normally.
