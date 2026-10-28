import SwiftUI
import UIKit
import UserNotifications

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
        }
        completionHandler()
    }
}

@main
struct AgendaIOSApp: App {
    @UIApplicationDelegateAdaptor(AgendaPushDelegate.self) var pushDelegate
    var body: some Scene {
        WindowGroup {
            ContentView().onAppear {
                let center = UNUserNotificationCenter.current()
                center.getNotificationSettings { settings in
                    if settings.authorizationStatus == .notDetermined {
                        center.requestAuthorization(options: [.alert, .badge, .sound]) { allowed, _ in
                            if allowed { AgendaKeychain.shared.ensureRemoteRegistrationIfPermitted() }
                        }
                    } else {
                        AgendaKeychain.shared.ensureRemoteRegistrationIfPermitted()
                    }
                }
            }
        }
    }
}
