import SwiftUI
import AgendaShared

struct ContentView: View {
    var body: some View {
        AgendaComposeView()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.white.ignoresSafeArea())
            .ignoresSafeArea(.keyboard)
    }
}

private struct AgendaComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(bridge: AgendaKeychain.shared)
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
