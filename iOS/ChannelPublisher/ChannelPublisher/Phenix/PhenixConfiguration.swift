//
//  Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import PhenixSdk
import UIKit

public enum PhenixConfiguration {
    public static var authToken: String?
    public static var publishToken: String?

    public static func makeChannelExpress() -> PhenixChannelExpress {
        guard let authToken = authToken else { fatalError("You must provide the AuthToken.") }

        let pcastExpressOptions = PhenixPCastExpressFactory.createPCastExpressOptionsBuilder(
            unrecoverableErrorCallback: { status, description in
                DispatchQueue.main.async {
                    AppDelegate.terminate(
                        afterDisplayingAlertWithTitle: "Something went wrong!",
                        message: "Application entered in unrecoverable state and will be terminated."
                    )
                }
            })
            .withMinimumConsoleLogLevel("Info")
            .withAuthenticationToken(authToken)
            .buildPCastExpressOptions()

        let roomExpressOptions = PhenixRoomExpressFactory.createRoomExpressOptionsBuilder()
            .withPCastExpressOptions(pcastExpressOptions)
            .buildRoomExpressOptions()

        let channelExpressOptions = PhenixChannelExpressFactory.createChannelExpressOptionsBuilder()
            .withRoomExpressOptions(roomExpressOptions)
            .buildChannelExpressOptions()

        return PhenixChannelExpressFactory.createChannelExpress(channelExpressOptions)
    }

    public static func makePublishChannelOptions(userMediaStream: PhenixUserMediaStream) -> PhenixPublishToChannelOptions! {
        var publishOptionsBuilder = PhenixPCastExpressFactory.createPublishOptionsBuilder()!

        if let publishToken = publishToken {
            publishOptionsBuilder = publishOptionsBuilder
                .withStreamToken(publishToken)
        }

        let publishOptions = publishOptionsBuilder
            .withUserMedia(userMediaStream)
            .buildPublishOptions()

        let publishToRoomOptions = PhenixChannelExpressFactory.createPublishToChannelOptionsBuilder()!
            .withPublishOptions(publishOptions)
            .buildPublishToChannelOptions()

        return publishToRoomOptions
    }
}
