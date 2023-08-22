//
//  Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import PhenixSdk

public enum PublishOption {
    public enum Camera: CaseIterable, CustomStringConvertible {
        case front, rear, off

        public var description: String {
            switch self {
            case .front: return "Front camera"
            case .rear:  return "Rear camera"
            case .off:   return "No video"
            }
        }

        public var value: PhenixFacingMode {
            switch self {
            case .front: return .user
            case .rear:  return .environment
            case .off:   return .undefined
            }
        }
    }

    public enum Microphone: CaseIterable, CustomStringConvertible {
        case on, off

        public var description: String {
            switch self {
            case .on:  return "Microphone"
            case .off: return "No audio"
            }
        }

        public var value: Bool {
            switch self {
            case .on:  return true
            case .off: return false
            }
        }
    }

    public enum FrameRate: Double, CaseIterable, CustomStringConvertible {
        case fps15 = 15
        case fps30 = 30

        public var description: String {
            switch self {
            case .fps15: return "15 fps"
            case .fps30: return "30 fps"
            }
        }
    }

    public enum AudioEchoCancellation: CaseIterable, CustomStringConvertible {
        case automatic, on, off

        public var description: String {
            switch self {
            case .automatic: return "AEC auto"
            case .on:        return "AEC on"
            case .off:       return "AEC off"
            }
        }

        public var value: PhenixAudioEchoCancellationMode {
            switch self {
            case .automatic: return .automatic
            case .on:        return .on
            case .off:       return .off
            }
        }
    }
}
