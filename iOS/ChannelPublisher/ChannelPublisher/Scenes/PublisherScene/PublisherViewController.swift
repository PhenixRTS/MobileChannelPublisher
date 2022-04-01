//
//  Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import Combine
import UIKit

class PublisherViewController: UIViewController, Storyboarded {
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

    private var cancellable: AnyCancellable?

    var viewModel: ViewModel!

    override func viewDidLoad() {
        super.viewDidLoad()

        assert(viewModel != nil, "ViewModel should exist!")

        // Setup user interface
        cameraButton.setTitle(viewModel.camera.description, for: .normal)
        frameRateButton.setTitle(viewModel.frameRate.description, for: .normal)
        microphoneButton.setTitle(viewModel.microphone.description, for: .normal)
        audioEchoCancellationButton.setTitle(viewModel.audioEchoCancellation.description, for: .normal)

        startBroadcastButton.isEnabled = false
        broadcastConfigurationButtons.forEach { $0.isEnabled = false }

        viewModel.delegate = self
        viewModel.subscribeForEvents()
        viewModel.initializeMedia()
    }

    // MARK: - User interface actions

    @IBAction func cameraButtonTapped(_ sender: UIButton) {
        showActionSheet(title: "Camera", values: ViewModel.Camera.allCases) { [weak self] value in
            sender.setTitle(value.description, for: .normal)
            self?.viewModel.setMedia(camera: value)
        }
    }

    @IBAction func microphoneButtonTapped(_ sender: UIButton) {
        showActionSheet(title: "Microphone", values: ViewModel.Microphone.allCases) { [weak self] value in
            sender.setTitle(value.description, for: .normal)
            self?.viewModel.setMedia(microphone: value)
        }
    }

    @IBAction func frameRateButtonTapped(_ sender: UIButton) {
        let values: [ViewModel.MediaOptions.FrameRate] = [.fps15, .fps30]
        showActionSheet(title: "Frame rate", values: values) { [weak self] value in
            sender.setTitle(value.description, for: .normal)
            self?.viewModel.setMedia(frameRate: value)
        }
    }

    @IBAction func audioEchoCancellationButtonTapped(_ sender: UIButton) {
        showActionSheet(
            title: "Audio Echo Cancellation",
            values: ViewModel.MediaOptions.AudioEchoCancellation.allCases
        ) { [weak self] value in
            sender.setTitle(value.description, for: .normal)
            self?.viewModel.setMedia(audioEchoCancellation: value)
        }
    }

    @IBAction func broadcastButtonTapped(_ sender: UIButton) {
        activityIndicatorView.isHidden = false
        startBroadcastButton.isHidden = true
        broadcastConfigurationButtons.forEach { $0.isHidden = true }

        viewModel.publishToChannel()
    }

    @IBAction func stopBroadcastButtonTapped(_ sender: UIButton) {
        viewModel.stopPublishing()

        stopBroadcastButton.isHidden = true
        startBroadcastButton.isHidden = false
        broadcastConfigurationButtons.forEach { $0.isHidden = false }
    }

    // MARK: - Private methods

    private func showActionSheet<T: CustomStringConvertible>(
        title: String,
        values: [T],
        completion: @escaping (T) -> Void
    ) {
        let alertController = UIAlertController(title: title, message: nil, preferredStyle: .actionSheet)
        for value in values {
            let action = UIAlertAction(title: value.description, style: .default) { _ in completion(value) }
            alertController.addAction(action)
        }
        alertController.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        present(alertController, animated: true)
    }
}

extension PublisherViewController: PublisherViewModelDelegate {
    func getStreamLayer() -> CALayer? {
        surfaceView.layer
    }

    func publisherViewModelDidInitializeMedia(_ viewModel: ViewModel) {
        activityIndicatorView.isHidden = true
        startBroadcastButton.isEnabled = true
        broadcastConfigurationButtons.forEach { $0.isEnabled = true }
    }

    func publisherViewModel(_ viewModel: ViewModel, didFailToInitializeMediaWith description: String?) {
        AppDelegate.terminate(
            afterDisplayingAlertWithTitle: "Problem occurred when starting media",
            message: description ?? "n/a"
        )
    }

    func publisherViewModelDidStartPublishingMedia(_ viewModel: ViewModel) {
        activityIndicatorView.isHidden = true
        stopBroadcastButton.isHidden = false
        broadcastConfigurationButtons.forEach { $0.isEnabled = true }
    }

    func publisherViewModel(_ viewModel: ViewModel, didFailToPublishMediaWith description: String?) {
        AppDelegate.terminate(
            afterDisplayingAlertWithTitle: "Something went wrong",
            message: "Application failed to publish to the channel (\(description ?? "n/a"))."
        )
    }
}
