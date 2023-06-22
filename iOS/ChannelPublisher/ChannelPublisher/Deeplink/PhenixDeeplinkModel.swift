//
//  Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import PhenixDeeplink

struct PhenixDeeplinkModel: PhenixDeeplinkModelProvider {
    var authToken: String?
    var publishToken: String?

    init?(components: URLComponents) {
        if let string = components.queryItems?.first(where: { $0.name == "authToken" })?.value {
            self.authToken = string
        }

        if let string = components.queryItems?.first(where: { $0.name == "publishToken" })?.value {
            self.publishToken = string
        }
    }
}
