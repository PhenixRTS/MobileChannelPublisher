//
//  Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import Combine
import Foundation
import os.log
import PhenixCore

extension PublisherViewController {
    class ViewModel {
        // swiftlint:disable:next nesting
        typealias MediaOptions = PhenixCore.MediaOptions

        private static let logger = OSLog(identifier: "PublisherViewController.ViewModel")

        private let core: PhenixCore
        private let session: AppSession

        private var coreEventCancellable: AnyCancellable?
        private var configuration: PhenixCore.MediaConfiguration = .default

        var camera: Camera {
            guard configuration.isVideoEnabled else {
                return .off
            }

            switch configuration.cameraFacingMode {
            case .rear:
                return .rear
            case .front:
                return .front
            }
        }

        var microphone: Microphone {
            configuration.isAudioEnabled ? .on : .off
        }

        var audioEchoCancellation: MediaOptions.AudioEchoCancellation {
            configuration.audioEchoCancelation
        }

        var frameRate: MediaOptions.FrameRate {
            configuration.frameRate
        }

        weak var delegate: PublisherViewModelDelegate?

        init(core: PhenixCore, session: AppSession) {
            self.core = core
            self.session = session
        }

        func initializeMedia() {
            core.setLocalMedia(enabled: true)

            if let layer = delegate?.getStreamLayer() {
                core.previewVideo(layer: layer)
            }
        }

        func subscribeForEvents() {
            subscribeForCoreEvents()
        }

        func publishToChannel() {
            let configuration = PhenixCore.Channel.Configuration(
                alias: session.alias,
                publishToken: session.publishToken
            )
            core.publishToChannel(configuration: configuration)
        }

        func stopPublishing() {
            core.stopPublishingToChannel()
        }

        // MARK: Media configuration

        func setMedia(camera value: Camera) {
            if value == .off {
                core.setSelfVideoEnabled(enabled: false)
                return
            } else {
                core.setSelfVideoEnabled(enabled: true)
            }

            switch value {
            case .front:
                configuration.cameraFacingMode = .front
            case .rear:
                configuration.cameraFacingMode = .rear
            default:
                break
            }

            core.updateLocalMedia(configuration)
        }

        func setMedia(microphone value: Microphone) {
            core.setSelfAudioEnabled(enabled: value == .on)
        }

        func setMedia(frameRate value: MediaOptions.FrameRate) {
            configuration.frameRate = value
            core.updateLocalMedia(configuration)
        }

        func setMedia(audioEchoCancellation value: MediaOptions.AudioEchoCancellation) {
            configuration.audioEchoCancelation = value
            core.updateLocalMedia(configuration)
        }

        // MARK: - Private methods

        private func subscribeForCoreEvents() {
            coreEventCancellable = core.eventPublisher
                .sink { [weak self] completion in
                    self?.processEventCompletion(completion)
                } receiveValue: { [weak self] event in
                    self?.processEvent(event)
                }
        }

        private func processEventCompletion(_ completion: Subscribers.Completion<PhenixCore.Error>) {
            // do nothing
        }

        private func processEvent(_ event: PhenixCore.Event) {
            switch event {
            case .media(.mediaInitialized):
                delegate?.publisherViewModelDidInitializeMedia(self)

            case .media(.mediaNotInitialized):
                delegate?.publisherViewModel(
                    self,
                    didFailToInitializeMediaWith: "Could not access local media. Please try again."
                )

            case .channel(.channelPublished):
                delegate?.publisherViewModelDidStartPublishingMedia(self)

            case .channel(.channelPublishingFailed(_, let error)):
                delegate?.publisherViewModel(self, didFailToPublishMediaWith: error.localizedDescription)

            default:
                // do nothing, no other events needs to be cached here.
                break
            }
        }
    }
}
