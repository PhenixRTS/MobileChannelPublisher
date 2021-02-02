//
//  Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import PhenixSdk

public protocol PhenixChannelPublisherDelegate: AnyObject {
    func channelPublisher(_ channelPublisher: PhenixChannelPublisher, didPublishWith publisher: PhenixExpressPublisher, roomService: PhenixRoomService)
    func channelPublisher(_ channelPublisher: PhenixChannelPublisher, didFailToPublishWith error: PhenixChannelPublisher.Error)
}
