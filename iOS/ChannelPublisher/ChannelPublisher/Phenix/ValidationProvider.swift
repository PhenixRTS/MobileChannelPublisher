//
//  Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import Foundation

public enum ValidationProvider {
    public static func validate(authToken: String?, publishToken: String?) throws {
        switch (authToken, publishToken) {
        case (.some, .some):
            // Do nothing, because this is a valid scenario
            break

        default:
            throw Error(reason: "Both `authToken` and `publishToken` must be provided.")
        }
    }
}

extension ValidationProvider {
    public struct Error: Swift.Error, LocalizedError {
        public let reason: String
        public var errorDescription: String? { reason }
    }
}
