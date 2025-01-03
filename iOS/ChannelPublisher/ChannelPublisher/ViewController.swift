//
//  Copyright 2024 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
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
    @IBOutlet private var focusPointImage: UIImageView!

    private lazy var channelPublisher: PhenixChannelPublisher = {
        let publisher = PhenixChannelPublisher(channelExpress: AppDelegate.channelExpress)
        publisher.delegate = self
        return publisher
    }()

    private var currentRoomService: PhenixRoomService?
    private var currentExpressPublisher: PhenixExpressPublisher?
    private var userMediaStreamController: UserMediaStreamController?

    private var focusImageHideDispatch : DispatchWorkItem?

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

        // Configure tap gesture to focus (one tap)
        let focusTapGesture = UITapGestureRecognizer(target: self, action: #selector(surfaceViewTappedOnce))
        focusTapGesture.numberOfTapsRequired = 1
        surfaceView.addGestureRecognizer(focusTapGesture)

        // Configure tap gesture to open debug menu, when user taps 5 times on the video surface view.
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(surfaceViewTappedMultipleTimes))
        tapGesture.numberOfTapsRequired = 5
        surfaceView.addGestureRecognizer(tapGesture)

        // Setup user interface
        cameraButton.setTitle(userMediaConfiguration.camera.description, for: .normal)
        frameRateButton.setTitle(userMediaConfiguration.frameRate?.description, for: .normal)
        microphoneButton.setTitle(userMediaConfiguration.microphone.description, for: .normal)
        audioEchoCancellationButton.setTitle(userMediaConfiguration.audioEchoCancellation.description, for: .normal)

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

    @objc func surfaceViewTappedOnce(tapGesture: UITapGestureRecognizer) {
        let locationInView = tapGesture.location(in: surfaceView)

        let focusRelativePoint = userMediaStreamController?.renderer.convertRenderPreviewCoordinatesToSourceCoordinates(locationInView)

        if setFocus(focusRelativePoint!) {
            focusImageHideDispatch?.cancel()

            focusPointImage.isHidden = false
            focusPointImage.center = locationInView

            // Animate the icon with a zoom in.
            focusPointImage.transform = CGAffineTransformScale(CGAffineTransformIdentity, 2.0, 2.0);
            UIView.animate(withDuration: 0.5) {
                self.focusPointImage.transform = CGAffineTransformScale(CGAffineTransformIdentity, 1.0, 1.0);
            }

            // Auto-hide the icon after a delay.
            // If focusing somewhere else meanwhile, the counter will be reinitialized.
            focusImageHideDispatch = DispatchWorkItem(block: { [weak self] in
                self?.focusPointImage.isHidden = true
            })
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5, execute: focusImageHideDispatch!)
        }
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
        let alertController = UIAlertController(title: title, message: nil, preferredStyle: .actionSheet)
        for value in values {
            let action = UIAlertAction(title: value.description, style: .default) { _ in completion(value, value.description) }
            alertController.addAction(action)
        }
        alertController.addAction(UIAlertAction(title: "Cancel", style: .cancel))

        alertController.popoverPresentationController?.sourceView = self.view
        alertController.popoverPresentationController?.sourceRect = CGRect(x: self.view.bounds.width / 2, y: 0, width: 1, height: 1)

        present(alertController, animated: true)
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
        userMediaConfiguration.audioEchoCancellation = selectedValue

        do {
            try userMediaStreamController?.update(configuration: userMediaConfiguration)
        } catch {
            AppDelegate.terminate(
                afterDisplayingAlertWithTitle: "User media configuration error",
                message: "Could not set audio echo cancellation configuration."
            )
        }
    }

    // Returns true if the focus point could be set. There are many reasons to fail,the most common would be
    // that the camera does not support it (the front camera of an iPhone does not support setting a focus point).
    private func setFocus(_ focusPoint: CGPoint) -> Bool {
        if userMediaConfiguration.camera == .off  {
            return false
        }

        userMediaConfiguration.focusPoint = focusPoint

        defer {
            // Reset the saved focus point to avoid setting a new focus point if any other parameter is changed.
            userMediaConfiguration.focusPoint = nil
        }

        do {
            try userMediaStreamController?.update(configuration: userMediaConfiguration)
        } catch {
            print("Cannot set focus to position [\(focusPoint)] on current video device")
            return false
        }

        return true
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
