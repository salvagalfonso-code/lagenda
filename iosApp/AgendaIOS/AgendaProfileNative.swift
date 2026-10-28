import Foundation
import UIKit
import PhotosUI
import AuthenticationServices
import CryptoKit
import Security
import AgendaShared

/// A single, system-owned sign-in session. Tokens are not logged or saved by this class.
final class AgendaSocialLoginCoordinator: NSObject, ASAuthorizationControllerDelegate,
                                           ASAuthorizationControllerPresentationContextProviding,
                                           ASWebAuthenticationPresentationContextProviding {
    static let shared = AgendaSocialLoginCoordinator()
    private var callback: IosSocialAuthCallback?
    private var nonce = ""
    private var webSession: ASWebAuthenticationSession?

    private func randomToken() -> String? {
        var bytes = [UInt8](repeating: 0, count: 32)
        guard SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes) == errSecSuccess else { return nil }
        return Data(bytes).base64EncodedString().replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_").replacingOccurrences(of: "=", with: "")
    }
    private func finish(_ idToken: String = "", error: String = "") {
        let previous = callback
        callback = nil
        webSession = nil
        let usedNonce = nonce
        nonce = ""
        DispatchQueue.main.async { previous?.onComplete(idToken: idToken, nonce: usedNonce, error: error) }
    }
    func begin(provider: String, callback: IosSocialAuthCallback) {
        DispatchQueue.main.async {
            guard self.callback == nil else {
                callback.onComplete(idToken: "", nonce: "", error: "Ja hi ha un inici de sessió en curs")
                return
            }
            guard let nonce = self.randomToken() else {
                callback.onComplete(idToken: "", nonce: "", error: "No s'ha pogut crear el codi segur")
                return
            }
            self.callback = callback
            self.nonce = nonce
            if provider.uppercased() == "APPLE" { self.beginApple() }
            else if provider.uppercased() == "GOOGLE" { self.beginGoogle() }
            else { self.finish(error: "Proveïdor desconegut") }
        }
    }
    private func beginApple() {
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.email, .fullName]
        request.nonce = SHA256.hash(data: Data(nonce.utf8)).map { String(format: "%02x", $0) }.joined()
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor { anchor() }
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor { anchor() }
    private func anchor() -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
            .first(where: { $0.activationState == .foregroundActive })?.windows.first(where: { $0.isKeyWindow }) ?? UIWindow()
    }
    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let data = credential.identityToken,
              let token = String(data: data, encoding: .utf8), !token.isEmpty else {
            finish(error: "Apple no ha retornat una identitat verificable")
            return
        }
        finish(token)
    }
    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        finish(error: error.localizedDescription)
    }
    private func beginGoogle() {
        // Google Cloud: OAuth 2.0 iOS client (bundle ID must match this signed app).
        let clientID = (Bundle.main.object(forInfoDictionaryKey: "AgendaGoogleIOSClientID") as? String ?? "").trimmingCharacters(in: .whitespaces)
        let reversed = (Bundle.main.object(forInfoDictionaryKey: "AgendaGoogleReversedClientID") as? String ?? "").trimmingCharacters(in: .whitespaces)
        guard clientID.hasSuffix(".apps.googleusercontent.com"), reversed.hasPrefix("com.googleusercontent.apps.") else {
            finish(error: "Falta configurar el client OAuth d'iOS de Google i l'esquema de retorn")
            return
        }
        guard let verifier = randomToken(), let state = randomToken() else {
            finish(error: "No s'ha pogut iniciar l'autenticació segura")
            return
        }
        let redirect = "\(reversed):/oauth2redirect"
        let challenge = Data(SHA256.hash(data: Data(verifier.utf8))).base64EncodedString()
            .replacingOccurrences(of: "+", with: "-").replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
        var components = URLComponents(string: "https://accounts.google.com/o/oauth2/v2/auth")!
        components.queryItems = [URLQueryItem(name: "client_id", value: clientID),
            URLQueryItem(name: "redirect_uri", value: redirect), URLQueryItem(name: "response_type", value: "code"),
            URLQueryItem(name: "scope", value: "openid email profile"), URLQueryItem(name: "nonce", value: nonce),
            URLQueryItem(name: "state", value: state), URLQueryItem(name: "code_challenge", value: challenge),
            URLQueryItem(name: "code_challenge_method", value: "S256")]
        guard let authURL = components.url else { finish(error: "URL d'autenticació invàlida"); return }
        let web = ASWebAuthenticationSession(url: authURL, callbackURLScheme: reversed) { [weak self] callbackURL, error in
            guard let self = self else { return }
            guard error == nil, let url = callbackURL,
                  let returned = URLComponents(url: url, resolvingAgainstBaseURL: false),
                  returned.queryItems?.first(where: { $0.name == "state" })?.value == state,
                  let code = returned.queryItems?.first(where: { $0.name == "code" })?.value,
                  url.scheme == reversed else {
                self.finish(error: error?.localizedDescription ?? "Resposta de Google no vàlida")
                return
            }
            var req = URLRequest(url: URL(string: "https://oauth2.googleapis.com/token")!)
            req.httpMethod = "POST"
            req.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
            let pairs = ["client_id": clientID, "code": code, "redirect_uri": redirect,
                         "grant_type": "authorization_code", "code_verifier": verifier]
            var form = URLComponents()
            form.queryItems = pairs.map { URLQueryItem(name: $0.key, value: $0.value) }
            req.httpBody = form.percentEncodedQuery?.data(using: .utf8)
            URLSession.shared.dataTask(with: req) { data, response, error in
                guard let http = response as? HTTPURLResponse, http.statusCode == 200,
                      let data = data,
                      let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                      let token = json["id_token"] as? String, !token.isEmpty else {
                    self.finish(error: error?.localizedDescription ?? "Google no ha validat l'accés")
                    return
                }
                self.finish(token)
            }.resume()
        }
        web.presentationContextProvider = self
        web.prefersEphemeralWebBrowserSession = true
        webSession = web
        if !web.start() { finish(error: "No s'ha pogut obrir l'accés de Google") }
    }
}

final class AgendaProfilePhotoPicker: NSObject, PHPickerViewControllerDelegate {
    static let shared = AgendaProfilePhotoPicker()
    private var callback: IosPickedPhotoCallback?
    func show(callback: IosPickedPhotoCallback) {
        DispatchQueue.main.async {
            guard self.callback == nil else {
                callback.onComplete(base64Jpeg: "", error: "Ja hi ha un selector de fotografies obert")
                return
            }
            var settings = PHPickerConfiguration(photoLibrary: .shared())
            settings.filter = .images
            settings.selectionLimit = 1
            let picker = PHPickerViewController(configuration: settings)
            picker.delegate = self
            guard let root = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene })
                .first(where: { $0.activationState == .foregroundActive })?.windows
                .first(where: { $0.isKeyWindow })?.rootViewController else {
                callback.onComplete(base64Jpeg: "", error: "No hi ha una finestra activa")
                return
            }
            var presenter = root
            while let next = presenter.presentedViewController { presenter = next }
            self.callback = callback
            presenter.present(picker, animated: true)
        }
    }
    func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        picker.dismiss(animated: true)
        guard let provider = results.first?.itemProvider else {
            let previous = callback; callback = nil
            previous?.onComplete(base64Jpeg: "", error: "Selecció cancel·lada")
            return
        }
        guard provider.canLoadObject(ofClass: UIImage.self) else {
            let previous = callback; callback = nil
            previous?.onComplete(base64Jpeg: "", error: "La imatge no és compatible")
            return
        }
        provider.loadObject(ofClass: UIImage.self) { object, error in
            guard let image = object as? UIImage else {
                self.complete("", error: error?.localizedDescription ?? "No s'ha pogut llegir la foto")
                return
            }
            let longest = max(image.size.width, image.size.height)
            let scale = min(1.0, 1280.0 / max(longest, 1))
            let size = CGSize(width: max(1, image.size.width * scale), height: max(1, image.size.height * scale))
            let renderer = UIGraphicsImageRenderer(size: size)
            let normalized = renderer.image { _ in image.draw(in: CGRect(origin: .zero, size: size)) }
            guard let jpeg = normalized.jpegData(compressionQuality: 0.75), jpeg.count <= 5 * 1024 * 1024 else {
                self.complete("", error: "La fotografia supera la mida permesa")
                return
            }
            self.complete(jpeg.base64EncodedString())
        }
    }
    private func complete(_ jpeg: String, error: String = "") {
        DispatchQueue.main.async {
            let old = self.callback; self.callback = nil
            old?.onComplete(base64Jpeg: jpeg, error: error)
        }
    }
}

extension AgendaKeychain {
    func beginSocialSignIn(provider: String, callback: IosSocialAuthCallback) {
        AgendaSocialLoginCoordinator.shared.begin(provider: provider, callback: callback)
    }
    func pickProfilePhoto(callback: IosPickedPhotoCallback) {
        AgendaProfilePhotoPicker.shared.show(callback: callback)
    }
    func eraseLocalAccountData(userId: Int32) {
        guard userId > 0 else { return }
        let suffix = "_\(userId)"
        // Only delete this account's settings; retain unrelated users and shared event catalog.
        for key in UserDefaults.standard.dictionaryRepresentation().keys where key.hasPrefix("agenda_kmp_") {
            let raw = String(key.dropFirst("agenda_kmp_".count))
            if raw.hasSuffix(suffix) || raw.hasPrefix("reminder_\(userId)_") || raw.hasPrefix("festival_slot_\(userId)_") {
                UserDefaults.standard.removeObject(forKey: key)
            }
        }
        UserDefaults.standard.removeObject(forKey: "agenda_kmp_push_open_artist")
        UNUserNotificationCenter.current().getPendingNotificationRequests { requests in
            let prefix = "agenda-event-\(userId)-"
            let ids = requests.map(\.identifier).filter { $0.hasPrefix(prefix) }
            UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ids)
        }
    }
}
