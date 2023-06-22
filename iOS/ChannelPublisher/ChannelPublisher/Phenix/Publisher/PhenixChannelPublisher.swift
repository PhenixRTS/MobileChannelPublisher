//
//  Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import PhenixSdk
import UIKit

public final class PhenixChannelPublisher {
    private let channelExpress: PhenixChannelExpress

    public weak var delegate: PhenixChannelPublisherDelegate?

    public init(channelExpress: PhenixChannelExpress) {
        self.channelExpress = channelExpress
    }

    public func publish(userMediaStream: PhenixUserMediaStream) {
        let options = PhenixConfiguration.makePublishChannelOptions(
            userMediaStream: userMediaStream
        )

        channelExpress.publish(toChannel: options) { [weak self] status, roomService, publisher in
            guard let self = self else { return }
            switch status {
            case .ok:
                guard let roomService = roomService else {
                    let error = Error(reason: "Missing PhenixRoomService")
                    self.delegate?.channelPublisher(self, didFailToPublishWith: error)
                    return
                }

                guard let publisher = publisher else {
                    let error = Error(reason: "Missing PhenixExpressPublisher")
                    self.delegate?.channelPublisher(self, didFailToPublishWith: error)
                    return
                }

                self.delegate?.channelPublisher(self, didPublishWith: publisher, roomService: roomService)

            default:
                let error = Error(reason: status.description)
                self.delegate?.channelPublisher(self, didFailToPublishWith: error)
            }
        }
    }

    public func stopPublishing(roomService: PhenixRoomService?, publisher: PhenixExpressPublisher?) {
        publisher?.stop()
        roomService?.leaveRoom { roomService, status in }
    }
}

// MARK: - PhenixChannelViewer.Error
extension PhenixChannelPublisher {
    public struct Error: Swift.Error {
        let reason: String
    }
}
