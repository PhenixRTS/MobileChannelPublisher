//
//  Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import Foundation

public enum ValidationProvider {
    public static func validate(backend: URL?, authToken: String?, publishToken: String?, channelAlias: String?) throws {
        switch (backend, authToken, publishToken, channelAlias) {
        case (.some(_), .some(_), .some(_), _),
             (.some(_), .some(_), .none, _),
             (.some(_), .none, .some(_), _):
            throw Error(
                reason: "Provide only `backend` or `authToken` with `publishToken`, backend url cannot be provided together with tokens."
            )

        case (.none, .some(_), .none, _),
             (.none, .none, .some(_), _):
            throw Error(reason: "Both `authToken` and `publishToken` must be provided.")

        case (.some(_), _, _, .none):
            throw Error(reason: "Channel Alias must be provided.")

        default:
            // Do nothing, because this is a valid scenario
            break
        }
    }
}

extension ValidationProvider {
    public struct Error: Swift.Error, LocalizedError {
        public let reason: String
        public var errorDescription: String? { reason }
    }
}
