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
        } else if let eventId = (data["event_id"] as? NSNumber)?.intValue, eventId > 0 {
            AgendaKeychain.shared.didTapEventNotification(eventId)
        } else if let eventString = data["event_id"] as? String, let eventId = Int(eventString), eventId > 0 {
            AgendaKeychain.shared.didTapEventNotification(eventId)
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
                .onAppear {
                // Only register if permission was previously granted; the bell/reminder
                // flow requests permission in context rather than on first app launch.
                AgendaKeychain.shared.ensureRemoteRegistrationIfPermitted()
            }
                .onChange(of: scenePhase) { phase in
                    switch phase {
                    case .background: AgendaKeychain.shared.feedSceneMovedToBackground()
                    case .active: AgendaKeychain.shared.feedSceneBecameActive()
                    default: break
                    }
                }
        }
    }
}
