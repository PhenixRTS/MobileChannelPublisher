//
//  Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
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

    public enum Quality: String, CaseIterable, CustomStringConvertible {
        case vvld, vld, ld, sd, hd, fhd

        public var description: String {
            switch self {
            case .vvld: return "144p"
            case .vld:  return "240p"
            case .ld:   return "360p"
            case .sd:   return "480p"
            case .hd:   return "720p"
            case .fhd:  return "1080p"
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

        public var value: PhenixAudioEchoCancelationMode {
            switch self {
            case .automatic: return .automatic
            case .on:        return .on
            case .off:       return .off
            }
        }
    }

    public enum BitrateMode: CaseIterable, CustomStringConvertible {
        case sbr, sbr_vp8, sbr_h268, mbr, mbr_vp8, mbr_h264

        public var description: String {
            switch self {
            case .sbr:      return "SBR auto"
            case .sbr_vp8:  return "SBR/vp8"
            case .sbr_h268: return "SBR/h264"
            case .mbr:      return "MBR auto"
            case .mbr_vp8:  return "MBR/VP8"
            case .mbr_h264: return "MBR/H264"
            }
        }

        public var value: [String] {
            switch self {
            case .sbr:      return []
            case .sbr_vp8:  return ["prefer-vp8"]
            case .sbr_h268: return ["prefer-h264"]
            case .mbr:      return ["multi-bitrate"]
            case .mbr_vp8:  return ["multi-bitrate", "multi-bitrate-codec=vp8"]
            case .mbr_h264: return ["multi-bitrate", "multi-bitrate-codec=h264"]
            }
        }
    }
}
