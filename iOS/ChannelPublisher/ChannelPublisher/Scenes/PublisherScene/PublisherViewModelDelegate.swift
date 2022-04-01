//
//  Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import Foundation
import QuartzCore

protocol PublisherViewModelDelegate: AnyObject {
    typealias ViewModel = PublisherViewController.ViewModel

    func getStreamLayer() -> CALayer?
    func publisherViewModelDidInitializeMedia(_ viewModel: ViewModel)
    func publisherViewModel(_ viewModel: ViewModel, didFailToInitializeMediaWith description: String?)
    func publisherViewModelDidStartPublishingMedia(_ viewModel: ViewModel)
    func publisherViewModel(_ viewModel: ViewModel, didFailToPublishMediaWith description: String?)
}
