import SwiftUI

struct AppSettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var apiKeyStore: APIKeyStore
    @State private var apiKeyDraft: String

    init(apiKeyStore: APIKeyStore) {
        self.apiKeyStore = apiKeyStore
        _apiKeyDraft = State(initialValue: apiKeyStore.deepSeekAPIKey)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    SecureField("sk-...", text: $apiKeyDraft)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                } header: {
                    Text("DeepSeek API Key")
                } footer: {
                    Text("Key 只保存在本机 Keychain，用于从首页 AI 生成提词器文稿。")
                }
            }
            .navigationTitle("设置")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("取消") {
                        dismiss()
                    }
                }

                ToolbarItem(placement: .topBarTrailing) {
                    Button("保存") {
                        apiKeyStore.deepSeekAPIKey = apiKeyDraft
                        apiKeyStore.saveDeepSeekAPIKey()
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
            }
        }
    }
}
