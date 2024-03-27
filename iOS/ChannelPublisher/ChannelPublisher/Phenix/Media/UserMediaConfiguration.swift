//
//  Copyright 2024 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import PhenixSdk

public struct UserMediaConfiguration {
    public var camera: PublishOption.Camera
    public var frameRate: PublishOption.FrameRate?
    public var frameHeight: Double = 360
    public var microphone: PublishOption.Microphone
    public var audioEchoCancellation: PublishOption.AudioEchoCancellation

    public func makeOptions() -> PhenixUserMediaOptions {
        let options = PhenixUserMediaOptions()

        options.video.enabled = true
        options.video.capabilityConstraints[PhenixDeviceCapability.facingMode.rawValue] = [PhenixDeviceConstraint.initWith(camera.value)]
        options.video.capabilityConstraints[PhenixDeviceCapability.height.rawValue] = [PhenixDeviceConstraint.initWith(frameHeight)]

        if let fps = frameRate {
            options.video.capabilityConstraints[PhenixDeviceCapability.frameRate.rawValue] = [PhenixDeviceConstraint.initWith(fps.rawValue)]
        }

        options.audio.enabled = microphone.value
        options.audio.capabilityConstraints[PhenixDeviceCapability.audioEchoCancellationMode.rawValue] = [PhenixDeviceConstraint.initWith(audioEchoCancellation.value)]

        return options
    }
}

extension UserMediaConfiguration {
    public static let `default` = UserMediaConfiguration(camera: .front, frameRate: .fps30, microphone: .on, audioEchoCancellation: .on)
}
