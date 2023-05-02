//
//  Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import PhenixSdk
import UIKit

public enum PhenixConfiguration {
    public static var pcastUri: URL?
    public static var backendUri: URL? = URL(string: "https://demo.phenixrts.com/pcast")
    public static var authToken: String?
    public static var publishToken: String?
    public static var channelAlias: String?

    public static func makeChannelExpress() -> PhenixChannelExpress {
        precondition((backendUri != nil) != (authToken != nil), "You must provide the AuthToken or the backend url. At least one must be provided but not both simultaneously.")

        var pcastExpressOptionsBuilder = PhenixPCastExpressFactory.createPCastExpressOptionsBuilder()

        if let authToken = authToken {
            pcastExpressOptionsBuilder = pcastExpressOptionsBuilder?.withAuthenticationToken(authToken)
        } else if let backendUri = backendUri {
            pcastExpressOptionsBuilder = pcastExpressOptionsBuilder?.withBackendUri(backendUri.absoluteString)
        }

        if let pcastUri = pcastUri {
            pcastExpressOptionsBuilder = pcastExpressOptionsBuilder?.withPCastUri(pcastUri.absoluteString)
        }

        let pcastExpressOptions = pcastExpressOptionsBuilder?
            .withMinimumConsoleLogLevel("Info")
            .withUnrecoverableErrorCallback { status, description in
                DispatchQueue.main.async {
                    AppDelegate.terminate(
                        afterDisplayingAlertWithTitle: "Something went wrong!",
                        message: "Application entered in unrecoverable state and will be terminated."
                    )
                }
            }
            .buildPCastExpressOptions()

        let roomExpressOptions = PhenixRoomExpressFactory.createRoomExpressOptionsBuilder()
            .withPCastExpressOptions(pcastExpressOptions)
            .buildRoomExpressOptions()

        let channelExpressOptions = PhenixChannelExpressFactory.createChannelExpressOptionsBuilder()
            .withRoomExpressOptions(roomExpressOptions)
            .buildChannelExpressOptions()

        return PhenixChannelExpressFactory.createChannelExpress(channelExpressOptions)
    }

    public static func makePublishChannelOptions(channelAlias: String?, userMediaStream: PhenixUserMediaStream) -> PhenixPublishToChannelOptions! {

        var channelOptions: PhenixChannelOptions? = nil

        if let channelAlias = channelAlias {
            channelOptions = PhenixRoomServiceFactory.createChannelOptionsBuilder()
                .withName(channelAlias)
                .withAlias(channelAlias)
                .buildChannelOptions()
        }

        var publishOptionsBuilder = PhenixPCastExpressFactory.createPublishOptionsBuilder()

        if let publishToken = publishToken {
            publishOptionsBuilder = publishOptionsBuilder?
                .withStreamToken(publishToken)
                .withSkipRetryOnUnauthorized()
        }

        let publishOptions = publishOptionsBuilder?
            .withUserMedia(userMediaStream)
            .buildPublishOptions()

        var publishToRoomOptionsBuilder = PhenixChannelExpressFactory.createPublishToChannelOptionsBuilder()!

        if let channelOptions = channelOptions {
            publishToRoomOptionsBuilder = publishToRoomOptionsBuilder
                .withChannelOptions(channelOptions)
        }

        let publishToRoomOptions = publishToRoomOptionsBuilder
            .withPublishOptions(publishOptions)
            .buildPublishToChannelOptions()

        return publishToRoomOptions
    }
}
