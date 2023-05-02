//
//  Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import Foundation
import PhenixSdk

public struct PhenixDebugViewModel {
    private let pcast: PhenixPCast
    internal let frameworkInformation: SDKInspector

    public init(pcast: PhenixPCast) {
        self.pcast = pcast
        self.frameworkInformation = SDKInspector()
    }

    public func collectPCastLogs(then handle: @escaping (String?) -> Void) {
        pcast.collectLogMessages { _, status, messages in
            guard let messages = messages, status == .ok else {
                handle(nil)
                return
            }

            handle(messages)
        }
    }
}
