import Foundation
import Security
import CryptoKit
import UIKit
import SwiftUI
import CoreImage.CIFilterBuiltins
import UserNotifications
import AgendaShared
import StripePaymentSheet

/** Pas 1: transport natiu per a operacions que requereixen AVFoundation/APNs.
 * Ha d'usar la mateixa base API i la mateixa semàntica HTTP que KMP i Android v336.
 * No es canvia el servidor ni el projecte Android.
 */
private enum AgendaNativeApi {
    static let baseURL = "https://quimerafest.com/app/api/"

    static func post(_ path: String, token: String, body: [String: Any]) -> URLRequest? {
        guard !token.isEmpty,
              !path.isEmpty, !path.hasPrefix("/"), !path.contains(".."), !path.contains("://"),
              let url = URL(string: baseURL + path),
              let data = try? JSONSerialization.data(withJSONObject: body) else { return nil }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = 30
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
        request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.httpBody = data
        return request
    }
}

/// Secure persistent session token: system Keychain, never UserDefaults or logs.
final class AgendaKeychain: NSObject, IosSessionBridge {
    static let shared = AgendaKeychain()
    private let service = "com.agendavalenciana.app.ios.session"
    private let account = "api_bearer_token"
    /** Held while native checkout is visible, never persisted on the device. */
    private var ticketPaymentSheet: PaymentSheet?
    private var ticketPaymentActive = false

    func presentTicketPayment(clientSecret: String, publishableKey: String,
                              merchantName: String, callback: IosTicketPaymentCallback) {
        DispatchQueue.main.async {
            // KMP has already validated the server-issued PaymentIntent response.
            guard !self.ticketPaymentActive else {
                callback.onComplete(result: "FAILED", error: "Ja hi ha una compra oberta.")
                return
            }
            guard clientSecret.hasPrefix("pi_"), clientSecret.contains("_secret_"),
                  publishableKey.hasPrefix("pk_test_") || publishableKey.hasPrefix("pk_live_"),
                  !merchantName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                  let scene = UIApplication.shared.connectedScenes
                    .compactMap({ $0 as? UIWindowScene })
                    .first(where: { $0.activationState == .foregroundActive }),
                  let window = scene.windows.first(where: { $0.isKeyWindow }),
                  let root = window.rootViewController else {
                callback.onComplete(result: "FAILED", error: "No es pot obrir la passarel·la de pagament.")
                return
            }
            var presenter = root
            while let presented = presenter.presentedViewController { presenter = presented }
            StripeAPI.defaultPublishableKey = publishableKey
            var configuration = PaymentSheet.Configuration()
            configuration.merchantDisplayName = merchantName
            configuration.returnURL = "agendavalenciana://stripe-redirect"
            // No delayed methods: only a confirmed server-paid order emits tickets.
            configuration.allowsDelayedPaymentMethods = false
            let sheet = PaymentSheet(paymentIntentClientSecret: clientSecret, configuration: configuration)
            self.ticketPaymentSheet = sheet
            self.ticketPaymentActive = true
            sheet.present(from: presenter) { result in
                self.ticketPaymentActive = false
                self.ticketPaymentSheet = nil
                switch result {
                case .completed:
                    // UI must check payments/status.php. This is NOT a paid receipt.
                    callback.onComplete(result: "COMPLETED", error: "")
                case .canceled:
                    callback.onComplete(result: "CANCELLED", error: "")
                case .failed(let error):
                    callback.onComplete(result: "FAILED", error: error.localizedDescription)
                }
            }
        }
    }

    private var query: [String: Any] {
        [kSecClass as String: kSecClassGenericPassword,
         kSecAttrService as String: service,
         kSecAttrAccount as String: account]
    }

    func loadToken() -> String? {
        var q = query
        q[kSecReturnData as String] = true
        q[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: CFTypeRef?
        guard SecItemCopyMatching(q as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    func saveToken(token: String) {
        guard let data = token.data(using: .utf8) else { return }
        SecItemDelete(query as CFDictionary)
        var q = query
        q[kSecValueData as String] = data
        q[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        _ = SecItemAdd(q as CFDictionary, nil)
        syncDeviceTokenIfAuthenticated()
        // If Keychain rejects the write, the current session still works in memory.
        // The next launch will ask for login again; credentials never fall back to UserDefaults.
    }

    func clearToken() {
        // A token belongs to its server user. Revoke before changing the bearer.
        if let old = loadToken(), !old.isEmpty { unregisterDevice(bearer: old) }
        SecItemDelete(query as CFDictionary)
        syncDeviceTokenIfAuthenticated() // Guest identity can now own the same device.
    }

    /** Pas 2: server-issued guest identity, separate from registered bearer and UserDefaults.
     * The Keychain entry is accessible after first unlock, only on this device.
     */
    private var guestQuery: [String: Any] {
        [kSecClass as String: kSecClassGenericPassword,
         kSecAttrService as String: service,
         kSecAttrAccount as String: "server_guest_identity"]
    }

    func loadGuestIdentity() -> String? {
        var q = guestQuery
        q[kSecReturnData as String] = true
        q[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: CFTypeRef?
        guard SecItemCopyMatching(q as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    func saveGuestIdentity(json: String) -> Bool {
        guard let data = json.data(using: .utf8),
              let parsed = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              (parsed["user_id"] as? Int ?? 0) > 0,
              parsed["guest_id"] is String,
              parsed["token"] is String else { return false }
        var attributes = guestQuery
        attributes[kSecValueData as String] = data
        attributes[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        // Update first if an entry exists; avoid a delete-then-add gap.
        let update = SecItemUpdate(guestQuery as CFDictionary,
                                   [kSecValueData as String: data] as CFDictionary)
        if update == errSecSuccess { return true }
        guard update == errSecItemNotFound else { return false }
        defer { syncDeviceTokenIfAuthenticated() }
        return SecItemAdd(attributes as CFDictionary, nil) == errSecSuccess
    }

    /** Pas 3: clear a claimed guest only after confirmation from the PHP server. */
    func clearGuestIdentity() -> Bool {
        let result = SecItemDelete(guestQuery as CFDictionary)
        return result == errSecSuccess || result == errSecItemNotFound
    }

    // Pas 6: a single encrypted queue file for all users, whose entries carry their
    // own owner_id. AES.GCM key is device-only in Keychain; no bearer in the file.
    // nil means storage/decryption failure (not an empty queue); never overwrite it.
    private var queueKeyQuery: [String: Any] {
        [kSecClass as String: kSecClassGenericPassword,
         kSecAttrService as String: service,
         kSecAttrAccount as String: "pending_offline_aes256_key_v1"]
    }
    private var queueFile: URL? {
        guard let directory = FileManager.default.urls(for: .applicationSupportDirectory,
                                                       in: .userDomainMask).first?
            .appendingPathComponent("AgendaOffline", isDirectory: true) else { return nil }
        do {
            try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
            var values = URLResourceValues()
            values.isExcludedFromBackup = true
            var folder = directory
            try? folder.setResourceValues(values)
        } catch { return nil }
        return directory.appendingPathComponent("pending-v1.aesgcm")
    }
    private func offlineSymmetricKey(createIfMissing: Bool) -> SymmetricKey? {
        var lookup = queueKeyQuery
        lookup[kSecReturnData as String] = true
        lookup[kSecMatchLimit as String] = kSecMatchLimitOne
        var value: CFTypeRef?
        let result = SecItemCopyMatching(lookup as CFDictionary, &value)
        if result == errSecSuccess {
            guard let data = value as? Data, data.count == 32 else { return nil }
            return SymmetricKey(data: data)
        }
        guard result == errSecItemNotFound, createIfMissing else { return nil }
        let generated = SymmetricKey(size: .bits256)
        let bytes = generated.withUnsafeBytes { Data($0) }
        var query = queueKeyQuery
        query[kSecValueData as String] = bytes
        query[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        guard SecItemAdd(query as CFDictionary, nil) == errSecSuccess else { return nil }
        return generated
    }
    func loadEncryptedPendingOperations() -> String? {
        guard let file = queueFile else { return nil }
        guard FileManager.default.fileExists(atPath: file.path) else {
            // Key may be created by the next write; no pending actions exist yet.
            return "{\"version\":1,\"pending\":[]}"
        }
        guard let key = offlineSymmetricKey(createIfMissing: false),
              let bytes = try? Data(contentsOf: file),
              let sealed = try? AES.GCM.SealedBox(combined: bytes),
              let plaintext = try? AES.GCM.open(sealed, using: key),
              let json = String(data: plaintext, encoding: .utf8) else { return nil }
        return json
    }
    func saveEncryptedPendingOperations(json: String) -> Bool {
        guard json.utf8.count <= 1_000_000, let file = queueFile,
              let key = offlineSymmetricKey(createIfMissing: !FileManager.default.fileExists(atPath: file.path)),
              let bytes = json.data(using: .utf8),
              let sealed = try? AES.GCM.seal(bytes, using: key),
              let ciphertext = sealed.combined else { return false }
        do {
            try ciphertext.write(to: file, options: [.atomic, .completeFileProtectionUnlessOpen])
            var values = URLResourceValues()
            values.isExcludedFromBackup = true
            var protectedFile = file
            try? protectedFile.setResourceValues(values)
            return true
        } catch { return false }
    }

    /// Store large catalog JSON as a file, not in UserDefaults (which is for small prefs).
    private var cacheDirectory: URL? {
        guard let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first?
            .appendingPathComponent("AgendaCache", isDirectory: true) else { return nil }
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }
    func readCache(key: String) -> String? {
        if key == "events_json", let file = cacheDirectory?.appendingPathComponent("events.json") {
            return try? String(contentsOf: file, encoding: .utf8)
        }
        return UserDefaults.standard.string(forKey: "agenda_kmp_" + key)
    }
    func writeCache(key: String, value: String) {
        if key == "events_json", let file = cacheDirectory?.appendingPathComponent("events.json") {
            try? value.write(to: file, atomically: true, encoding: .utf8)
        } else {
            UserDefaults.standard.set(value, forKey: "agenda_kmp_" + key)
        }
    }

    /// Apple confirms acceptance asynchronously. Never claim success before the completion handler.
    /// Translate between Android's server-projected `when_<eventId>` epoch millis and iOS local time.
    func reminderEpochMillis(isoDate: String) -> Int64 {
        let fmt = DateFormatter()
        fmt.locale = Locale(identifier: "en_US_POSIX")
        fmt.timeZone = .current
        fmt.dateFormat = "yyyy-MM-dd'T'HH:mm"
        fmt.isLenient = false
        guard let date = fmt.date(from: isoDate) else { return 0 }
        return Int64(date.timeIntervalSince1970 * 1000)
    }
    func formatReminderEpochMillis(epochMillis: Int64) -> String {
        guard epochMillis > 0 else { return "" }
        let fmt = DateFormatter()
        fmt.locale = Locale(identifier: "en_US_POSIX")
        fmt.timeZone = .current
        fmt.dateFormat = "yyyy-MM-dd'T'HH:mm"
        return fmt.string(from: Date(timeIntervalSince1970: TimeInterval(epochMillis) / 1000))
    }

    func scheduleLocalReminderConfirmed(userId: Int32, eventId: Int32, title: String,
                                       isoDate: String, callback: IosReminderSchedulingCallback) {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.timeZone = .current
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm"
        formatter.isLenient = false
        guard userId > 0, eventId > 0,
              let fireDate = formatter.date(from: isoDate), fireDate > Date() else {
            callback.onResult(success: false, message: "Data o compte no vàlids")
            return
        }
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = "Tens una activitat cultural pròximament."
        content.sound = .default
        content.userInfo = ["event_id": Int(eventId), "user_id": Int(userId), "type": "EVENT_REMINDER"]
        let parts = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: fireDate)
        let trigger = UNCalendarNotificationTrigger(dateMatching: parts, repeats: false)
        let request = UNNotificationRequest(identifier: "agenda-event-\(userId)-\(eventId)",
                                             content: content, trigger: trigger)
        let center = UNUserNotificationCenter.current()
        func addRequest() {
            center.add(request) { error in
                // Migrate a reminder scheduled by older v341/v342 builds to account-scoped IDs.
                if error == nil {
                    center.removePendingNotificationRequests(withIdentifiers: ["agenda-event-\(eventId)"])
                }
                callback.onResult(success: error == nil,
                                  message: error?.localizedDescription ?? "")
            }
        }
        center.getNotificationSettings { settings in
            switch settings.authorizationStatus {
            case .authorized, .provisional, .ephemeral:
                addRequest()
            case .notDetermined:
                center.requestAuthorization(options: [.alert, .badge, .sound]) { allowed, error in
                    if allowed && error == nil { addRequest() }
                    else { callback.onResult(success: false, message: "Notificacions no autoritzades") }
                }
            default:
                callback.onResult(success: false, message: "Permís de notificacions desactivat")
            }
        }
    }
    /** An APNs token is not a login credential. It is attached only to the current authenticated account. */
    private var feedLifecycleListener: IosFeedLifecycleCallback?
    func setFeedLifecycleListener(listener: IosFeedLifecycleCallback) {
        feedLifecycleListener = listener
    }
    func notifyFeedBackground() { feedLifecycleListener?.onBackground() }
    func notifyFeedForeground() { feedLifecycleListener?.onForeground() }

    // Pas 30: the APNs token is a Keychain secret, never an ordinary UserDefaults value.
    private var apnsTokenQuery: [String: Any] {
        [kSecClass as String: kSecClassGenericPassword,
         kSecAttrService as String: service,
         kSecAttrAccount as String: "apns_hex_device_token_v2"]
    }
    private var pushOpenListener: IosPushOpenCallback?
    private var pendingPushArtist: String?
    private var pendingPushEvent: Int32?
    private var pushEnabledInCurrentSession = false // In RAM only, restored from PHP state by KMP.

    func setPushOpenListener(listener: IosPushOpenCallback) {
        pushOpenListener = listener
        if let pending = pendingPushArtist {
            pendingPushArtist = nil
            DispatchQueue.main.async { listener.onOpenArtist(artist: pending) }
        }
        if let pending = pendingPushEvent {
            pendingPushEvent = nil
            DispatchQueue.main.async { listener.onOpenEvent(eventId: pending) }
        }
    }
    func didTapArtistNotification(_ artist: String) {
        guard !artist.isEmpty else { return }
        if let listener = pushOpenListener {
            DispatchQueue.main.async { listener.onOpenArtist(artist: artist) }
        } else { pendingPushArtist = artist }
    }
    func didTapPushEvent(_ eventId: Int32) {
        guard eventId > 0 else { return }
        if let listener = pushOpenListener {
            DispatchQueue.main.async { listener.onOpenEvent(eventId: eventId) }
        } else { pendingPushEvent = eventId }
    }

    // This is invoked only after the user's preference has been read from the server
    // or accepted into the encrypted offline queue. No automatic opt-in at app launch.
    func setPushDeliveryEnabled(enabled: Bool) {
        pushEnabledInCurrentSession = enabled
        if enabled { ensureRemoteRegistrationIfPermitted() }
        else if let bearer = currentPushBearer() { unregisterDevice(bearer: bearer) }
    }
    func requestRemoteNotifications(callback: IosReminderSchedulingCallback) {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound, .badge]) { allowed, error in
            if allowed && error == nil {
                DispatchQueue.main.async { UIApplication.shared.registerForRemoteNotifications() }
            }
            callback.onResult(success: allowed && error == nil,
                              message: allowed && error == nil ? "Permís concedit; registrant dispositiu" : "Permís no concedit")
        }
    }
    func ensureRemoteRegistrationIfPermitted() {
        guard pushEnabledInCurrentSession else { return }
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            if [.authorized, .provisional, .ephemeral].contains(settings.authorizationStatus) {
                DispatchQueue.main.async { UIApplication.shared.registerForRemoteNotifications() }
                self.syncDeviceTokenIfAuthenticated()
            }
        }
    }
    private func currentPushBearer() -> String? {
        if let token = loadToken(), !token.isEmpty { return token }
        guard let json = loadGuestIdentity(),
              let bytes = json.data(using: .utf8),
              let parsed = try? JSONSerialization.jsonObject(with: bytes) as? [String: Any],
              (parsed["user_id"] as? Int ?? 0) > 0,
              let bearer = parsed["token"] as? String, !bearer.isEmpty else { return nil }
        return bearer
    }
    private func apnsHexToken() -> String? {
        var q = apnsTokenQuery
        q[kSecReturnData as String] = true
        q[kSecMatchLimit as String] = kSecMatchLimitOne
        var ref: CFTypeRef?
        guard SecItemCopyMatching(q as CFDictionary, &ref) == errSecSuccess,
              let bytes = ref as? Data, let value = String(data: bytes, encoding: .utf8),
              value.count == 64 || value.count == 128,
              value.allSatisfy({ $0.isHexDigit }) else { return nil }
        return value
    }
    func didReceiveAPNsDeviceToken(_ data: Data) {
        let token = data.map { String(format: "%02x", $0) }.joined()
        guard (token.count == 64 || token.count == 128),
              let bytes = token.data(using: .utf8) else { return }
        let updates = SecItemUpdate(apnsTokenQuery as CFDictionary,
                                    [kSecValueData as String: bytes] as CFDictionary)
        if updates == errSecItemNotFound {
            var q = apnsTokenQuery
            q[kSecValueData as String] = bytes
            q[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            guard SecItemAdd(q as CFDictionary, nil) == errSecSuccess else { return }
        } else if updates != errSecSuccess { return }
        syncDeviceTokenIfAuthenticated()
    }
    private func performAPNsRequest(bearer: String, action: String) {
        guard let deviceToken = apnsHexToken(),
              let request = AgendaNativeApi.post("notifications/apns-device.php", token: bearer, body: [
                "action": action, "device_token": deviceToken,
                "environment": Self.apnsEnvironment
              ]) else { return }
        URLSession.shared.dataTask(with: request) { data, response, error in
            guard error == nil, let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode),
                  let data = data,
                  let obj = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
                  obj["ok"] as? Bool == true,
                  obj["registered"] as? Bool == (action == "register") else {
                // Never claim success. On next foreground or session restore retry the POST.
                return
            }
        }.resume()
    }
    private static var apnsEnvironment: String {
        // Entitlement from the signed build, not simply DEBUG (TestFlight is production).
        if let task = SecTaskCreateFromSelf(nil),
           let value = SecTaskCopyValueForEntitlement(task, "aps-environment" as CFString, nil) as? String,
           value == "development" || value == "production" { return value }
        #if DEBUG
        return "development"
        #else
        return "production"
        #endif
    }
    private func unregisterDevice(bearer: String) {
        performAPNsRequest(bearer: bearer, action: "unregister")
    }
    func syncDeviceTokenIfAuthenticated() {
        guard pushEnabledInCurrentSession, let bearer = currentPushBearer() else { return }
        performAPNsRequest(bearer: bearer, action: "register")
    }

    func shareText(text: String) {
        guard !text.isEmpty else { return }
        DispatchQueue.main.async {
            guard let scene = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene }).first(where: { $0.activationState == .foregroundActive }),
                let window = scene.windows.first(where: { $0.isKeyWindow }),
                let controller = window.rootViewController else { return }
            var presenter = controller
            while let presented = presenter.presentedViewController { presenter = presented }
            let sheet = UIActivityViewController(activityItems: [text], applicationActivities: nil)
            if let pop = sheet.popoverPresentationController {
                pop.sourceView = presenter.view
                pop.sourceRect = CGRect(x: presenter.view.bounds.midX,
                                        y: presenter.view.bounds.midY, width: 1, height: 1)
            }
            presenter.present(sheet, animated: true)
        }
    }

    // Pas 39: only the issued tickets are held locally. Full state remains in MariaDB.
    // Distinct AES-GCM document from the pending-actions queue; shared device-only Keychain key.
    private var offlineTicketIndexQuery: [String: Any] {
        [kSecClass as String: kSecClassGenericPassword,
         kSecAttrService as String: service,
         kSecAttrAccount as String: "offline_ticket_owner_v1"]
    }

    private func offlineTicketFile(ownerId: Int) -> URL? {
        guard ownerId > 0, let directory = queueFile?.deletingLastPathComponent() else { return nil }
        return directory.appendingPathComponent("tickets-\(ownerId)-v1.aesgcm")
    }

    private func currentTicketCredential() -> (token: String, guestUserId: Int?)? {
        if let bearer = loadToken(), !bearer.isEmpty { return (bearer, nil) }
        guard let json = loadGuestIdentity(),
              let data = json.data(using: .utf8),
              let root = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
              let bearer = root["token"] as? String, !bearer.isEmpty,
              let userId = root["user_id"] as? Int, userId > 0 else { return nil }
        return (bearer, userId)
    }

    private func ticketBearerFingerprint(_ bearer: String) -> String {
        SHA256.hash(data: Data(bearer.utf8)).map { String(format: "%02x", $0) }.joined()
    }

    func saveOfflineTickets(ownerId: Int32, json: String) -> Bool {
        let uid = Int(ownerId)
        guard uid > 0, json.utf8.count <= 2_000_000,
              let credential = currentTicketCredential(),
              credential.guestUserId == nil || credential.guestUserId == uid,
              let file = offlineTicketFile(ownerId: uid),
              let data = json.data(using: .utf8),
              let root = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
              root["version"] as? Int == 1,
              root["owner_id"] as? Int == uid,
              root["tickets"] is [[String: Any]],
              let key = offlineSymmetricKey(createIfMissing: true),
              let sealed = try? AES.GCM.seal(data, using: key),
              let bytes = sealed.combined else { return false }
        do {
            try bytes.write(to: file, options: [.atomic, .completeFileProtectionUnlessOpen])
            var values = URLResourceValues()
            values.isExcludedFromBackup = true
            var protectedFile = file
            try? protectedFile.setResourceValues(values)
        } catch { return false }
        // The index never contains a bearer, only a one-way digest; a new session
        // cannot unlock another user's tickets by guessing the previous user ID.
        guard currentTicketCredential()?.token == credential.token else { return false }
        let index = "\(uid)|\(ticketBearerFingerprint(credential.token))"
        let updated = SecItemUpdate(offlineTicketIndexQuery as CFDictionary,
                                    [kSecValueData as String: Data(index.utf8)] as CFDictionary)
        if updated == errSecSuccess { return true }
        guard updated == errSecItemNotFound else { return false }
        var insert = offlineTicketIndexQuery
        insert[kSecValueData as String] = Data(index.utf8)
        insert[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        return SecItemAdd(insert as CFDictionary, nil) == errSecSuccess
    }

    func loadOfflineTicketsForCurrentSession() -> String? {
        guard let credential = currentTicketCredential() else { return nil }
        var request = offlineTicketIndexQuery
        request[kSecReturnData as String] = true
        request[kSecMatchLimit as String] = kSecMatchLimitOne
        var found: CFTypeRef?
        guard SecItemCopyMatching(request as CFDictionary, &found) == errSecSuccess,
              let data = found as? Data,
              let index = String(data: data, encoding: .utf8) else { return nil }
        let components = index.split(separator: "|", omittingEmptySubsequences: false)
        guard components.count == 2, let uid = Int(components[0]), uid > 0,
              credential.guestUserId == nil || credential.guestUserId == uid,
              String(components[1]) == ticketBearerFingerprint(credential.token),
              let file = offlineTicketFile(ownerId: uid),
              let key = offlineSymmetricKey(createIfMissing: false),
              let encrypted = try? Data(contentsOf: file),
              let sealed = try? AES.GCM.SealedBox(combined: encrypted),
              let plaintext = try? AES.GCM.open(sealed, using: key),
              let root = (try? JSONSerialization.jsonObject(with: plaintext)) as? [String: Any],
              root["version"] as? Int == 1, root["owner_id"] as? Int == uid,
              root["tickets"] is [[String: Any]],
              currentTicketCredential()?.token == credential.token else { return nil }
        return String(data: plaintext, encoding: .utf8)
    }

    // Called only on explicit logout / confirmed account deletion for that owner.
    func clearOfflineTickets(ownerId: Int32) {
        let uid = Int(ownerId)
        guard uid > 0 else { return }
        if let file = offlineTicketFile(ownerId: uid) {
            try? FileManager.default.removeItem(at: file)
        }
        var request = offlineTicketIndexQuery
        request[kSecReturnData as String] = true
        request[kSecMatchLimit as String] = kSecMatchLimitOne
        var found: CFTypeRef?
        if SecItemCopyMatching(request as CFDictionary, &found) == errSecSuccess,
           let bytes = found as? Data,
           let value = String(data: bytes, encoding: .utf8),
           value.hasPrefix("\(uid)|") {
            SecItemDelete(offlineTicketIndexQuery as CFDictionary)
        }
    }

    // QR de serveis especials: es mostra, però la validesa i el bescanvi són exclusivament del servidor.
    func showSpecialServiceQr(code: String, title: String) {
        showTicketQr(code: code, title: title)
    }

    func scanSpecialServiceQr(callback: IosSpecialServiceQrCallback) {
        DispatchQueue.main.async {
            guard let scene = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene }).first(where: { $0.activationState == .foregroundActive }),
                let window = scene.windows.first(where: { $0.isKeyWindow }),
                let root = window.rootViewController else {
                    callback.onScanned(code: "", error: "La càmera no està disponible.")
                    return
                }
            var presenter = root
            while let presented = presenter.presentedViewController { presenter = presented }
            let host = UIHostingController(rootView: AgendaSpecialServiceCaptureView(callback: callback))
            host.modalPresentationStyle = .pageSheet
            presenter.present(host, animated: true)
        }
    }

    func showTicketQr(code: String, title: String) {
        guard !code.isEmpty else { return }
        DispatchQueue.main.async {
            guard let scene = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene }).first(where: { $0.activationState == .foregroundActive }),
                let window = scene.windows.first(where: { $0.isKeyWindow }),
                let controller = window.rootViewController else { return }
            var presenter = controller
            while let presented = presenter.presentedViewController { presenter = presented }
            let screen = IssuedTicketQrView(code: code, title: title)
            let host = UIHostingController(rootView: screen)
            host.modalPresentationStyle = .pageSheet
            presenter.present(host, animated: true)
        }
    }

    func cancelLocalReminder(userId: Int32, eventId: Int32) {
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: [
                "agenda-event-\(userId)-\(eventId)", "agenda-event-\(eventId)"
            ])
    }
}

/// The QR encodes the exact ticket code issued by the server.
/// Ticket validity is always decided by the backend at scanning time.
private struct IssuedTicketQrView: View {
    @Environment(\.dismiss) private var dismiss
    let code: String
    let title: String
    private var qrImage: UIImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(code.utf8)
        filter.correctionLevel = "M"
        guard let output = filter.outputImage,
              let cg = CIContext().createCGImage(output.transformed(by: CGAffineTransform(scaleX: 9, y: 9)),
                                                from: output.extent.applying(CGAffineTransform(scaleX: 9, y: 9)))
        else { return nil }
        return UIImage(cgImage: cg)
    }
    var body: some View {
        VStack(spacing: 18) {
            Text(title).font(.headline).multilineTextAlignment(.center)
            if let qr = qrImage {
                Image(uiImage: qr).interpolation(.none).resizable().scaledToFit()
                    .frame(width: 255, height: 255).accessibilityLabel("Codi QR de l'entrada")
            } else { Text("No s'ha pogut generar el QR").foregroundColor(.red) }
            Text(code).font(.system(.caption, design: .monospaced)).textSelection(.enabled)
            Text("Entrada personal. No compartisques el codi.").font(.caption).foregroundStyle(.secondary)
            Button("Tancar") { dismiss() }.buttonStyle(.borderedProminent)
        }.padding(24)
    }
}

// MARK: - iOS device-only camera verification. Never validates a ticket locally.
// QR scanning is deliberately opt-in and waits for a manual confirmation.
import AVFoundation

extension AgendaKeychain {
    func showTicketScanner(eventId: Int32) {
        guard eventId > 0 else { return }
        DispatchQueue.main.async {
            guard let scene = UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene }).first(where: { $0.activationState == .foregroundActive }),
                  let window = scene.windows.first(where: { $0.isKeyWindow }),
                  let root = window.rootViewController else { return }
            var presenter = root
            while let next = presenter.presentedViewController { presenter = next }
            let host = UIHostingController(rootView: AgendaScannerView(eventId: Int(eventId), token: self.loadToken() ?? ""))
            host.modalPresentationStyle = .pageSheet
            presenter.present(host, animated: true)
        }
    }

    func showNotificationSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        DispatchQueue.main.async { UIApplication.shared.open(url) }
    }
}

private final class AgendaPreviewView: UIView {
    let preview = AVCaptureVideoPreviewLayer()
    override init(frame: CGRect) {
        super.init(frame: frame)
        preview.videoGravity = .resizeAspectFill
        layer.addSublayer(preview)
    }
    required init?(coder: NSCoder) { fatalError("Not supported") }
    override func layoutSubviews() {
        super.layoutSubviews()
        preview.frame = bounds
    }
}

private struct AgendaCameraScanner: UIViewRepresentable {
    let onCode: (String) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(onCode: onCode) }
    func makeUIView(context: Context) -> AgendaPreviewView {
        let view = AgendaPreviewView(frame: .zero)
        context.coordinator.prepare(view: view)
        return view
    }
    func updateUIView(_ uiView: AgendaPreviewView, context: Context) {}
    static func dismantleUIView(_ uiView: AgendaPreviewView, coordinator: Coordinator) {
        coordinator.stop()
    }

    final class Coordinator: NSObject, AVCaptureMetadataOutputObjectsDelegate {
        private let session = AVCaptureSession()
        private let scanQueue = DispatchQueue(label: "agenda-ios-qr-capture")
        private let onCode: (String) -> Void
        private var wasDelivered = false

        init(onCode: @escaping (String) -> Void) { self.onCode = onCode }

        func prepare(view: AgendaPreviewView) {
            guard let camera = AVCaptureDevice.default(for: .video),
                  let input = try? AVCaptureDeviceInput(device: camera),
                  session.canAddInput(input) else { return }
            session.beginConfiguration()
            session.addInput(input)
            let output = AVCaptureMetadataOutput()
            guard session.canAddOutput(output) else { session.commitConfiguration(); return }
            session.addOutput(output)
            output.setMetadataObjectsDelegate(self, queue: .main)
            output.metadataObjectTypes = [.qr]
            session.commitConfiguration()
            view.preview.session = session
            scanQueue.async { [weak self] in
                guard let self = self, !self.session.isRunning else { return }
                self.session.startRunning()
            }
        }
        func metadataOutput(_ output: AVCaptureMetadataOutput,
                            didOutput metadataObjects: [AVMetadataObject],
                            from connection: AVCaptureConnection) {
            guard !wasDelivered, let code = (metadataObjects.first as? AVMetadataMachineReadableCodeObject)?.stringValue,
                  !code.isEmpty else { return }
            wasDelivered = true
            onCode(code)
            stop()
        }
        func stop() {
            scanQueue.async { [weak self] in
                guard let self = self, self.session.isRunning else { return }
                self.session.stopRunning()
            }
        }
    }
}

private struct AgendaScannerView: View {
    @Environment(\.dismiss) private var dismiss
    let eventId: Int
    let token: String
    @State private var permission = false
    @State private var permissionDenied = false
    @State private var scannedCode = ""
    @State private var busy = false
    @State private var result = ""

    var body: some View {
        NavigationStack {
            VStack(spacing: 14) {
                Text("Esdeveniment #\(eventId)").font(.subheadline).foregroundStyle(.secondary)
                if permission && scannedCode.isEmpty {
                    AgendaCameraScanner(onCode: { scannedCode = $0 })
                        .frame(height: 320).clipShape(RoundedRectangle(cornerRadius: 18))
                        .accessibilityLabel("Lector de codis QR")
                    Text("Enquadra el QR de l'entrada").font(.caption)
                } else if permissionDenied {
                    Text("Cal autoritzar la càmera des de Configuració d'iPhone.")
                        .multilineTextAlignment(.center)
                    Button("Obrir Configuració") { AgendaKeychain.shared.showNotificationSettings() }
                }
                if !scannedCode.isEmpty {
                    Text("Codi llegit:").font(.caption)
                    Text(scannedCode).font(.system(.callout, design: .monospaced)).textSelection(.enabled)
                    Button(busy ? "Validant…" : "Confirmar validació") {
                        Task { await validate() }
                    }
                    .disabled(busy || token.isEmpty)
                    .buttonStyle(.borderedProminent)
                    Button("Escanejar una altra entrada") {
                        scannedCode = ""; result = ""
                    }.disabled(busy)
                }
                if !result.isEmpty {
                    Text(result).multilineTextAlignment(.center)
                }
                Spacer()
            }
            .padding()
            .navigationTitle("Validar entrada")
            .toolbar { ToolbarItem(placement: .topBarTrailing) { Button("Tancar") { dismiss() } } }
            .onAppear { requestCameraPermission() }
        }
    }

    private func requestCameraPermission() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized: permission = true
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async { permission = granted; permissionDenied = !granted }
            }
        default: permissionDenied = true
        }
    }

    @MainActor private func validate() async {
        guard !token.isEmpty, AgendaKeychain.shared.loadToken() == token else {
            result = "Sessió canviada o caducada. Torna a iniciar sessió per validar."; return
        }
        busy = true
        defer { busy = false }
        guard let request = AgendaNativeApi.post("tickets/validate.php", token: token, body: [
            "event_id": eventId, "code": scannedCode
        ]) else { result = "No s'ha pogut preparar la validació"; return }
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let obj = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                result = "Resposta del servidor invàlida"; return
            }
            let ok = obj["ok"] as? Bool ?? false
            let outcome = (obj["outcome"] as? String ?? "").uppercased()
            let msg = obj["message"] as? String ?? ""
            let status = (response as? HTTPURLResponse)?.statusCode ?? 0
            // The API returns ok=true for rejected scans as well; only VALIDATED is success.
            if ok && (200...299).contains(status) && outcome == "VALIDATED" {
                result = "Entrada validada correctament. \(msg)"
            } else {
                result = "Entrada NO validada (\(outcome.isEmpty ? "ERROR" : outcome)). \(msg)"
            }
        } catch {
            result = "No s'ha pogut verificar el codi amb el servidor. Comprova la connexió."
        }
    }
}


/// Lector específic de serveis: només llig el token. El bescanvi necessita
/// una inspecció i una segona acció autoritzada al PHP (no es valida localment).
private struct AgendaSpecialServiceCaptureView: View {
    @Environment(\.dismiss) private var dismiss
    let callback: IosSpecialServiceQrCallback
    @State private var permission = false
    @State private var denied = false
    @State private var scanned = ""

    var body: some View {
        NavigationStack {
            VStack(spacing: 14) {
                if permission && scanned.isEmpty {
                    AgendaCameraScanner(onCode: { code in
                        guard !code.isEmpty else { return }
                        scanned = code
                    })
                    .frame(height: 340)
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                    Text("Enquadra el QR del servei").font(.caption)
                } else if denied {
                    Text("Cal autoritzar la càmera des dels ajustos d'iPhone.")
                    Button("Tornar") { dismiss() }
                }
                if !scanned.isEmpty {
                    Text("Codi detectat. No s'ha validat encara.").font(.callout)
                    Button("Utilitzar este codi") {
                        callback.onScanned(code: scanned, error: "")
                        dismiss()
                    }.buttonStyle(.borderedProminent)
                    Button("Tornar a escanejar") { scanned = "" }
                }
                Spacer()
            }
            .padding()
            .navigationTitle("Escanejar servei")
            .toolbar { ToolbarItem(placement: .topBarTrailing) { Button("Tancar") { dismiss() } } }
            .onAppear {
                switch AVCaptureDevice.authorizationStatus(for: .video) {
                case .authorized: permission = true
                case .notDetermined:
                    AVCaptureDevice.requestAccess(for: .video) { granted in
                        DispatchQueue.main.async { permission = granted; denied = !granted }
                    }
                default: denied = true
                }
            }
        }
    }
}
