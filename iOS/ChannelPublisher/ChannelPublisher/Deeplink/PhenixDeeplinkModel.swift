//
//  Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import PhenixDeeplink

struct PhenixDeeplinkModel: PhenixDeeplinkModelProvider {
    var alias: String?
    var uri: URL?
    var backend: URL?
    var authToken: String?
    var publishToken: String?

    init?(components: URLComponents) {
        self.alias = components.fragment

        if let string = components.queryItems?.first(where: { $0.name == "uri" })?.value {
            self.uri = URL(string: string)
        }

        if let string = components.queryItems?.first(where: { $0.name == "backend" })?.value {
            self.backend = URL(string: string)
        }

        if let string = components.queryItems?.first(where: { $0.name == "authToken" })?.value {
            self.authToken = string
        } else if let string = components.queryItems?.first(where: { $0.name == "edgeToken" })?.value {
            // "edgeToken" is deprecated token key name. It will be removed later on.
            self.authToken = string
        }

        if let string = components.queryItems?.first(where: { $0.name == "publishToken" })?.value {
            self.publishToken = string
        }
    }
}
