import CoreLocation
import Foundation

@MainActor
final class LocationManager: NSObject, ObservableObject {
    @Published var status: Status = .idle

    enum Status {
        case idle, locating, resolved(String), failed(String)
    }

    private let clManager = CLLocationManager()
    private let geocoder = CLGeocoder()
    private var continuation: CheckedContinuation<String, Error>?

    override init() {
        super.init()
        // Delegate is set lazily in fetchCity() to avoid iOS triggering
        // locationManagerDidChangeAuthorization (and a spurious requestLocation)
        // on every ProfileEditView presentation.
        clManager.desiredAccuracy = kCLLocationAccuracyKilometer
    }

    func fetchCity() async -> Result<String, Error> {
        // Fix 7: prevent concurrent calls from overwriting continuation.
        if case .locating = status { return .failure(LocationError.busy) }

        status = .locating
        // Fix 3: attach delegate only now, so the auth-change callback only
        // fires when the user has actually requested a location lookup.
        clManager.delegate = self

        do {
            let city = try await withCheckedThrowingContinuation { cont in
                self.continuation = cont
                let authStatus = clManager.authorizationStatus
                if authStatus == .notDetermined {
                    clManager.requestWhenInUseAuthorization()
                } else if authStatus == .authorizedWhenInUse || authStatus == .authorizedAlways {
                    clManager.requestLocation()
                } else {
                    cont.resume(throwing: LocationError.denied)
                    self.continuation = nil
                }
            }
            status = .resolved(city)
            return .success(city)
        } catch {
            let msg = (error as? LocationError)?.localizedDescription ?? error.localizedDescription
            status = .failed(msg)
            return .failure(error)
        }
    }
}

extension LocationManager: CLLocationManagerDelegate {
    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let auth = manager.authorizationStatus
        if auth == .authorizedWhenInUse || auth == .authorizedAlways {
            manager.requestLocation()
        } else if auth == .denied || auth == .restricted {
            Task { @MainActor in
                self.continuation?.resume(throwing: LocationError.denied)
                self.continuation = nil
            }
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.first else { return }
        Task { @MainActor in
            do {
                let placemarks = try await self.geocoder.reverseGeocodeLocation(location)
                let city = placemarks.first?.locality
                    ?? placemarks.first?.administrativeArea
                    ?? "未知城市"
                self.continuation?.resume(returning: city)
            } catch {
                self.continuation?.resume(throwing: error)
            }
            self.continuation = nil
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        Task { @MainActor in
            self.continuation?.resume(throwing: error)
            self.continuation = nil
        }
    }
}

enum LocationError: LocalizedError {
    case denied
    case busy
    var errorDescription: String? {
        switch self {
        case .denied: return "定位权限未开启，请在系统设置中允许"
        case .busy:   return "正在定位中，请稍候"
        }
    }
}
