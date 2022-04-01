//
//  Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import Foundation
import PhenixCore

extension PublisherViewController.ViewModel {
    enum Camera: CaseIterable, CustomStringConvertible {
        case front, rear, off

        public var description: String {
            switch self {
            case .front:
                return "Front camera"
            case .rear:
                return "Rear camera"
            case .off:
                return "No video"
            }
        }
    }

    enum Microphone: CaseIterable, CustomStringConvertible {
        // swiftlint:disable:next identifier_name
        case on, off

        public var description: String {
            switch self {
            case .on:
                return "Microphone"
            case .off:
                return "No audio"
            }
        }
    }
}

extension PhenixCore.MediaOptions.CameraFacingMode: CustomStringConvertible {
    public var description: String {
        switch self {
        case .front:
            return "Front camera"
        case .rear:
            return "Rear camera"
        }
    }
}

extension PhenixCore.MediaOptions.FrameRate: CustomStringConvertible {
    public var description: String {
        "\(rawValue) fps"
    }
}

extension PhenixCore.MediaOptions.AudioEchoCancellation: CustomStringConvertible {
    public var description: String {
        switch self {
        case .automatic:
            return "AEC auto"
        case .on:
            return "AEC on"
        case .off:
            return "AEC off"
        }
    }
}
