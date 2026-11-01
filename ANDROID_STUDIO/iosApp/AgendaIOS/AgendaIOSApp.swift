import SwiftUI
import UIKit
import UserNotifications
import StripePaymentSheet

/** The OS provides this token. The server, not the app, sends APNs notifications. */
final class AgendaPushDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }
    func application(_ application: UIApplication,
                     didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        AgendaKeychain.shared.didReceiveAPNsDeviceToken(deviceToken)
    }
    func application(_ application: UIApplication,
                     didFailToRegisterForRemoteNotificationsWithError error: Error) {
        // No success flag: the preference remains saved, but delivery is not guaranteed.
    }
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .badge, .sound])
    }
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        let data = response.notification.request.content.userInfo
        if let artist = data["artist_name"] as? String, !artist.isEmpty {
            AgendaKeychain.shared.didTapArtistNotification(artist)
        } else if let value = data["event_id"] as? Int, value > 0 {
            AgendaKeychain.shared.didTapPushEvent(Int32(value))
        }
        completionHandler()
    }
}

@main
struct AgendaIOSApp: App {
    @UIApplicationDelegateAdaptor(AgendaPushDelegate.self) var pushDelegate
    @Environment(\.scenePhase) private var scenePhase
    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(.light)
                .onOpenURL { url in
                    // Stripe 3DS / bank-redirect authentication returns to this app.
                    _ = StripeAPI.handleURLCallback(with: url)
                }
                .onChange(of: scenePhase) { phase in
                    switch phase {
                    case .background: AgendaKeychain.shared.notifyFeedBackground()
                    case .active:
                        AgendaKeychain.shared.notifyFeedForeground()
                        AgendaKeychain.shared.ensureRemoteRegistrationIfPermitted()
                    default: break
                    }
                }
                .onAppear {
                let center = UNUserNotificationCenter.current()
                center.getNotificationSettings { settings in
                    // User preference in the server is restored by KMP first.
                    // Never prompt for permission automatically at first launch.
                    if settings.authorizationStatus != .notDetermined {
                        AgendaKeychain.shared.ensureRemoteRegistrationIfPermitted()
                    }
                }
            }
        }
    }
}
