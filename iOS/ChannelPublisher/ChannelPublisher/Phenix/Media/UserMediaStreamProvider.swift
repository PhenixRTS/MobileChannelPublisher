//
//  Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
//

import PhenixSdk

public struct UserMediaStreamProvider {
    private let pcastExpress: PhenixPCastExpress

    public init(pcastExpress: PhenixPCastExpress) {
        self.pcastExpress = pcastExpress
    }

    public func createUserMediaStream(with configuration: UserMediaConfiguration = .default,
                                      completion: @escaping (Result<PhenixUserMediaStream, UserMediaStreamProvider.Error>) -> Void) {
        let options = configuration.makeOptions()
        pcastExpress.getUserMedia(options) { status, userMediaStream in
            switch status {
            case .ok:
                guard let userMediaStream = userMediaStream else {
                    completion(.failure(Error(reason: "User media stream not provided.")))
                    return
                }
                completion(.success(userMediaStream))

            default:
                completion(.failure(Error(reason: "User media stream not provided (\(status.description).")))
            }
        }
    }
}

extension UserMediaStreamProvider {
    public struct Error: Swift.Error, LocalizedError {
        public let reason: String
        public var errorDescription: String? { reason }
    }
}
