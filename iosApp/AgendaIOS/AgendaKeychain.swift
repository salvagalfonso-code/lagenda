import Foundation
import Security
import UIKit
import SwiftUI
import CoreImage.CIFilterBuiltins
import UserNotifications
import AgendaShared

/// Secure persistent session token: system Keychain, never UserDefaults or logs.
final class AgendaKeychain: NSObject, IosSessionBridge {
    static let shared = AgendaKeychain()
    private let service = "com.agendavalenciana.app.ios.session"
    private let account = "api_bearer_token"

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
        // If Keychain rejects the write, the current session still works in memory.
        // The next launch will ask for login again; credentials never fall back to UserDefaults.
    }

    func clearToken() { SecItemDelete(query as CFDictionary) }

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
        guard !token.isEmpty else { result = "Inicia sessió per validar entrades."; return }
        busy = true
        defer { busy = false }
        guard let url = URL(string: "https://quimerafest.com/app/api/tickets/validate.php") else { return }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        request.timeoutInterval = 15
        request.httpBody = try? JSONSerialization.data(withJSONObject: ["event_id": eventId, "code": scannedCode])
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
