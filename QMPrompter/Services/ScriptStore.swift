import Foundation

@MainActor
final class ScriptStore: ObservableObject {
    @Published private(set) var scripts: [Script] = []

    private let fileURL: URL

    init() {
        let directory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        fileURL = directory.appendingPathComponent("scripts.json")
        load()
    }

    func script(with id: Script.ID) -> Script? {
        scripts.first { $0.id == id }
    }

    func createDraft() -> Script {
        Script(title: "未命名文稿", content: "")
    }

    func createScript(title: String, content: String) -> Script {
        Script(
            title: title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "未命名文稿" : title,
            content: content
        )
    }

    func save(_ script: Script) {
        var next = script
        next.updatedAt = Date()

        if let index = scripts.firstIndex(where: { $0.id == next.id }) {
            scripts[index] = next
        } else {
            scripts.insert(next, at: 0)
        }

        scripts.sort { $0.updatedAt > $1.updatedAt }
        persist()
    }

    func delete(_ script: Script) {
        scripts.removeAll { $0.id == script.id }
        persist()
    }

    func delete(at offsets: IndexSet) {
        scripts.remove(atOffsets: offsets)
        persist()
    }

    private func load() {
        guard FileManager.default.fileExists(atPath: fileURL.path) else {
            scripts = [Self.sampleScript]
            persist()
            return
        }

        do {
            let data = try Data(contentsOf: fileURL)
            scripts = try JSONDecoder.qmPrompter.decode([Script].self, from: data)
        } catch {
            scripts = [Self.sampleScript]
        }
    }

    private func persist() {
        do {
            let data = try JSONEncoder.qmPrompter.encode(scripts)
            try data.write(to: fileURL, options: [.atomic])
        } catch {
            assertionFailure("Failed to save scripts: \(error)")
        }
    }

    private static let sampleScript = Script(
        title: "试用文稿",
        content: """
        大家好，这里是乔木提词器的第一版测试。

        这个版本先不录视频，只显示前置摄像头预览，让我可以看着镜头练习表达。

        点击屏幕中央可以播放或暂停。
        左侧上下滑动可以调整速度。
        右侧上下滑动可以跳转进度。

        如果这套基础体验顺手，下一步再接远程网页粘贴和同步 API。
        """,
        fontSize: Script.defaultFontSize,
        scrollSpeed: 78
    )
}

private extension JSONEncoder {
    static var qmPrompter: JSONEncoder {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        return encoder
    }
}

private extension JSONDecoder {
    static var qmPrompter: JSONDecoder {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }
}
