//
//  Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import PhenixDebug
import PhenixSdk
import UIKit

class ViewController: UIViewController {
    @IBOutlet private var surfaceView: UIView!
    @IBOutlet private var activityIndicatorView: UIActivityIndicatorView!
    @IBOutlet private var broadcastConfigurationButtons: [UIButton]!
    @IBOutlet private var startBroadcastButton: UIButton!
    @IBOutlet private var stopBroadcastButton: UIButton!

    // MARK: - Configuration elements
    @IBOutlet private var cameraButton: UIButton!
    @IBOutlet private var frameRateButton: UIButton!
    @IBOutlet private var microphoneButton: UIButton!
    @IBOutlet private var audioEchoCancellationButton: UIButton!

    private lazy var channelPublisher: PhenixChannelPublisher = {
        let publisher = PhenixChannelPublisher(channelExpress: AppDelegate.channelExpress)
        publisher.delegate = self
        return publisher
    }()

    private var currentRoomService: PhenixRoomService?
    private var currentExpressPublisher: PhenixExpressPublisher?
    private var userMediaStreamController: UserMediaStreamController?

    // MARK: Configurable parameters
    private var userMediaConfiguration = UserMediaConfiguration.default

    override func viewDidLoad() {
        super.viewDidLoad()

        // Validate configuration
        do {
            try ValidationProvider.validate(
                authToken: PhenixConfiguration.authToken,
                publishToken: PhenixConfiguration.publishToken
            )
        } catch {
            DispatchQueue.main.async {
                AppDelegate.terminate(
                    afterDisplayingAlertWithTitle: "Incorrect configuration",
                    message: error.localizedDescription
                )
            }
            return
        }

        // Configure tap gesture to open debug menu, when user taps 5 times on the video surface view.
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(surfaceViewTappedMultipleTimes))
        tapGesture.numberOfTapsRequired = 5
        surfaceView.addGestureRecognizer(tapGesture)

        // Setup user interface
        cameraButton.setTitle(userMediaConfiguration.camera.description, for: .normal)
        frameRateButton.setTitle(userMediaConfiguration.frameRate?.description, for: .normal)
        microphoneButton.setTitle(userMediaConfiguration.microphone.description, for: .normal)
        audioEchoCancellationButton.setTitle(userMediaConfiguration.audioEchoCancelation.description, for: .normal)

        startBroadcastButton.isEnabled = false
        broadcastConfigurationButtons.forEach { $0.isEnabled = false }

        // Get user media stream and then create user media stream controller out of it.
        let streamProvider = UserMediaStreamProvider(pcastExpress: AppDelegate.channelExpress.pcastExpress)
        streamProvider.createUserMediaStream(with: userMediaConfiguration) { [weak self] result in
            DispatchQueue.main.async {
                switch result {
                case .success(let userMediaStream):
                    self?.createUserMediaController(with: userMediaStream)

                    self?.activityIndicatorView.isHidden = true
                    self?.startBroadcastButton.isEnabled = true
                    self?.broadcastConfigurationButtons.forEach { $0.isEnabled = true }

                case .failure(let error):
                    AppDelegate.terminate(
                        afterDisplayingAlertWithTitle: "Problem with media",
                        message: error.localizedDescription
                    )
                }
            }
        }
    }

    // MARK: - User interface actions

    @IBAction func cameraButtonTapped(_ sender: UIButton) {
        showActionSheet(title: "Camera", values: PublishOption.Camera.allCases) {
            [weak self] selectedValue, description in

            sender.setTitle(description, for: .normal)
            self?.updateCamera(selectedValue)
        }
    }

    @IBAction func microphoneButtonTapped(_ sender: UIButton) {
        showActionSheet(title: "Microphone", values: PublishOption.Microphone.allCases) {
            [weak self] selectedValue, description in

            sender.setTitle(description, for: .normal)
            self?.updateMicrophone(selectedValue)
        }
    }

    @IBAction func frameRateButtonTapped(_ sender: UIButton) {
        showActionSheet(title: "Frame rate", values: PublishOption.FrameRate.allCases) {
            [weak self] selectedValue, description in

            sender.setTitle(description, for: .normal)
            self?.updateFrameRate(selectedValue)
        }
    }

    @IBAction func audioEchoCancellationButtonTapped(_ sender: UIButton) {
        showActionSheet(title: "Audio Echo Cancellation", values: PublishOption.AudioEchoCancellation.allCases) {
            [weak self] selectedValue, description in

            sender.setTitle(description, for: .normal)
            self?.updateAudioEchoCancellation(selectedValue)
        }
    }

    @IBAction func broadcastButtonTapped(_ sender: UIButton) {
        activityIndicatorView.isHidden = false
        startBroadcastButton.isHidden = true
        broadcastConfigurationButtons.forEach { $0.isHidden = true }

        publish()
    }

    @IBAction func stopBroadcastButtonTapped(_ sender: UIButton) {
        stopPublishing()

        stopBroadcastButton.isHidden = true
        startBroadcastButton.isHidden = false
        broadcastConfigurationButtons.forEach { $0.isHidden = false }
    }

    @objc func surfaceViewTappedMultipleTimes() {
        let pcast: PhenixPCast = AppDelegate.channelExpress.roomExpress.pcastExpress.pcast
        let viewModel = PhenixDebugViewModel(pcast: pcast)
        let vc = PhenixDebugViewController(viewModel: viewModel)
        present(vc, animated: true)
    }

    // MARK: - Private methods

    private func createUserMediaController(with userMediaStream: PhenixUserMediaStream) {
        userMediaStreamController = UserMediaStreamController(userMediaStream)
        userMediaStreamController?.setCamera(surfaceView.layer)
    }

    private func publish() {
        guard let userMediaStream = userMediaStreamController?.userMediaStream else {
            fatalError("Fatal error. User stream must be provided.")
        }

        channelPublisher.publish(userMediaStream: userMediaStream)
    }

    private func stopPublishing() {
        channelPublisher.stopPublishing(roomService: currentRoomService, publisher: currentExpressPublisher)

        currentExpressPublisher = nil
        currentRoomService = nil
    }

    private func showActionSheet<T: CustomStringConvertible>(title: String, values: [T], completion: @escaping (T, String) -> Void) {
        let ac = UIAlertController(title: title, message: nil, preferredStyle: .actionSheet)
        for value in values {
            let action = UIAlertAction(title: value.description, style: .default) { _ in completion(value, value.description) }
            ac.addAction(action)
        }
        ac.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        present(ac, animated: true)
    }

    // MARK: - Configuration handlers

    private func updateCamera(_ selectedValue: PublishOption.Camera) {
        if selectedValue == .off {
            userMediaStreamController?.setVideo(enabled: false)
        } else {
            userMediaStreamController?.setVideo(enabled: true)
            userMediaConfiguration.camera = selectedValue

            do {
                try self.userMediaStreamController?.update(configuration: userMediaConfiguration)
            } catch {
                AppDelegate.terminate(
                    afterDisplayingAlertWithTitle: "User media configuration error",
                    message: "Could not set camera configuration."
                )
            }
        }
    }

    private func updateMicrophone(_ selectedValue: PublishOption.Microphone) {
        userMediaStreamController?.setAudio(enabled: selectedValue.value)
    }

    private func updateFrameRate(_ selectedValue: PublishOption.FrameRate) {
        userMediaConfiguration.frameRate = selectedValue

        do {
            try userMediaStreamController?.update(configuration: userMediaConfiguration)
        } catch {
            AppDelegate.terminate(
                afterDisplayingAlertWithTitle: "User media configuration error",
                message: "Could not set frame rate configuration."
            )
        }
    }

    private func updateAudioEchoCancellation(_ selectedValue: PublishOption.AudioEchoCancellation) {
        userMediaConfiguration.audioEchoCancelation = selectedValue

        do {
            try userMediaStreamController?.update(configuration: userMediaConfiguration)
        } catch {
            AppDelegate.terminate(
                afterDisplayingAlertWithTitle: "User media configuration error",
                message: "Could not set audio echo cancellation configuration."
            )
        }
    }
}

extension ViewController: PhenixChannelPublisherDelegate {
    func channelPublisher(_ channelPublisher: PhenixChannelPublisher, didPublishWith publisher: PhenixExpressPublisher, roomService: PhenixRoomService) {
        DispatchQueue.main.async { [weak self] in
            self?.currentExpressPublisher = publisher
            self?.currentRoomService = roomService

            self?.activityIndicatorView.isHidden = true
            self?.stopBroadcastButton.isHidden = false
        }
    }

    func channelPublisher(_ channelPublisher: PhenixChannelPublisher, didFailToPublishWith error: PhenixChannelPublisher.Error) {
        DispatchQueue.main.async {
            AppDelegate.terminate(
                afterDisplayingAlertWithTitle: "Something went wrong",
                message: "Application failed to publish to the channel (\(error.reason))."
            )
        }
    }
}
