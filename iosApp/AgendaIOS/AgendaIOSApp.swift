import SwiftUI
import UserNotifications

@main
struct AgendaIOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView().onAppear {
                // Apple only permits asking after the first launch, not during installation.
                // A denial is respected; the user can change it in iOS Settings.
                let center = UNUserNotificationCenter.current()
                center.getNotificationSettings { settings in
                    guard settings.authorizationStatus == .notDetermined else { return }
                    center.requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in }
                }
            }
        }
    }
}
