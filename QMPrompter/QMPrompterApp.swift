import SwiftUI

@main
struct QMPrompterApp: App {
    @StateObject private var scriptStore = ScriptStore()

    var body: some Scene {
        WindowGroup {
            ScriptListView()
                .environmentObject(scriptStore)
        }
    }
}
