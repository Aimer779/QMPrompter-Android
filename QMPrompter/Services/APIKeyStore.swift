import Foundation
import Security

@MainActor
final class APIKeyStore: ObservableObject {
    @Published var deepSeekAPIKey: String = ""

    private let account = "deepseek-api-key"
    private let service = "com.qiaomu.Prompter"

    var hasDeepSeekAPIKey: Bool {
        !deepSeekAPIKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    init() {
        deepSeekAPIKey = Self.read(account: account, service: service) ?? ""
    }

    func saveDeepSeekAPIKey() {
        let key = deepSeekAPIKey.trimmingCharacters(in: .whitespacesAndNewlines)
        deepSeekAPIKey = key

        if key.isEmpty {
            Self.delete(account: account, service: service)
        } else {
            Self.save(key, account: account, service: service)
        }
    }

    private static func save(_ value: String, account: String, service: String) {
        guard let data = value.data(using: .utf8) else { return }
        delete(account: account, service: service)

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: account,
            kSecAttrService as String: service,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            kSecValueData as String: data
        ]

        SecItemAdd(query as CFDictionary, nil)
    }

    private static func read(account: String, service: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: account,
            kSecAttrService as String: service,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        guard status == errSecSuccess,
              let data = item as? Data
        else {
            return nil
        }

        return String(data: data, encoding: .utf8)
    }

    private static func delete(account: String, service: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: account,
            kSecAttrService as String: service
        ]

        SecItemDelete(query as CFDictionary)
    }
}
