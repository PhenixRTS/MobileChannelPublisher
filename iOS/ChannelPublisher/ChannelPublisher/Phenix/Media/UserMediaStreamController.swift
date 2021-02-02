//
//  Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import PhenixSdk

public struct UserMediaStreamController {
    private let renderer: PhenixRenderer

    public private(set) var userMediaStream: PhenixUserMediaStream

    public init(_ userMediaStream: PhenixUserMediaStream) {
        self.userMediaStream = userMediaStream
        self.renderer = userMediaStream.mediaStream.createRenderer()
    }

    public func setCamera(_ layer: CALayer) {
        renderer.start(layer)
    }

    public func setAudio(enabled: Bool) {
        userMediaStream.mediaStream.getAudioTracks()?.forEach { $0.setEnabled(enabled) }
    }

    public func setVideo(enabled: Bool) {
        userMediaStream.mediaStream.getVideoTracks()?.forEach { $0.setEnabled(enabled) }
    }

    public func update(configuration: UserMediaConfiguration) throws {
        let options = configuration.makeOptions()
        let result = userMediaStream.apply(options)

        if result != .ok {
            throw Error.optionsNotApplied
        }
    }
}

extension UserMediaStreamController {
    public enum Error: Swift.Error {
        case optionsNotApplied
    }
}
