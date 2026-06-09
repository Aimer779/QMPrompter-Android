import SwiftUI
import UIKit

struct ScriptEditorView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: ScriptStore

    @State private var script: Script
    @State private var showPrompter = false
    @State private var showTitleEditor = false
    @State private var titleDraft = ""
    @State private var showClearPanel = false
    @State private var selectedTab: EditorTab = .script
    @FocusState private var editorFocused: Bool

    init(script: Script) {
        _script = State(initialValue: script)
    }

    private var canStartPrompting: Bool {
        !script.content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    var body: some View {
        VStack(spacing: 0) {
            Picker("编辑区域", selection: $selectedTab) {
                ForEach(EditorTab.allCases) { tab in
                    Text(tab.title).tag(tab)
                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 6)

            tabContent
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .alert("修改文稿名", isPresented: $showTitleEditor) {
            TextField("文稿名", text: $titleDraft)
                .textInputAutocapitalization(.never)

            Button("取消", role: .cancel) {}
            Button("保存") {
                let nextTitle = titleDraft.trimmingCharacters(in: .whitespacesAndNewlines)
                script.title = nextTitle.isEmpty ? "未命名文稿" : nextTitle
                save()
            }
        }
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 10) {
                if selectedTab == .script {
                    editorActions
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }

                Button {
                    startPrompting()
                } label: {
                    HStack(spacing: 10) {
                        Image(systemName: "play.fill")
                            .font(.system(size: 15, weight: .semibold))
                            .frame(width: 24, height: 24)

                        Text("开始提词")
                            .font(.headline.weight(.semibold))

                        Spacer()

                        Image(systemName: "chevron.right")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundStyle(.secondary)
                    }
                    .foregroundStyle(.primary)
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, 18)
                    .padding(.vertical, 13)
                    .editorGlassButton()
                    .contentShape(Capsule())
                }
                .buttonStyle(.plain)
                .opacity(canStartPrompting ? 1 : 0.48)
                .disabled(!canStartPrompting)
            }
            .padding(.horizontal, 16)
            .padding(.top, 10)
            .padding(.bottom, 8)
            .animation(.snappy(duration: 0.22), value: selectedTab)
            .background {
                Rectangle()
                    .fill(.ultraThinMaterial)
                    .overlay(alignment: .top) {
                        Rectangle()
                            .fill(.white.opacity(0.18))
                            .frame(height: 0.5)
                    }
            }
        }
        .toolbar {
            ToolbarItem(placement: .principal) {
                Button {
                    beginTitleEditing()
                } label: {
                    HStack(spacing: 5) {
                        Text(displayTitle)
                            .font(.headline.weight(.semibold))
                            .lineLimit(1)

                        Image(systemName: "pencil")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(.secondary)
                    }
                    .contentShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("修改文稿名")
            }

            ToolbarItem(placement: .topBarTrailing) {
                Button("保存") {
                    save()
                    dismiss()
                }
                .disabled(script.content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
        }
        .fullScreenCover(isPresented: $showPrompter) {
            PrompterView(script: $script) {
                save()
            }
        }
        .overlay {
            GlassActionPanel(isPresented: showClearPanel) {
                showClearPanel = false
            } content: {
                clearPanelContent
            }
        }
        .onAppear {
            normalizeDisplaySettings()
        }
        .onDisappear {
            if !script.content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                save()
            }
        }
    }

    @ViewBuilder
    private var tabContent: some View {
        switch selectedTab {
        case .script:
            Form {
                Section {
                    TextEditor(text: $script.content)
                        .frame(minHeight: 420)
                        .font(.system(size: 17, weight: .regular, design: .default))
                        .lineSpacing(4)
                        .scrollContentBackground(.hidden)
                        .focused($editorFocused)
                }
            }
            .scrollContentBackground(.hidden)
        case .display:
            Form {
                Section {
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Label("字号", systemImage: "textformat.size")
                            Spacer()
                            Text("\(Int(script.fontSize))")
                                .foregroundStyle(.secondary)
                        }
                        Slider(value: $script.fontSize, in: 12...110, step: 1)
                    }

                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Label("速度", systemImage: "speedometer")
                            Spacer()
                            Text("\(Int(script.scrollSpeed)) 字/分")
                                .foregroundStyle(.secondary)
                        }
                        Slider(value: $script.scrollSpeed, in: 20...220, step: 2)
                    }

                    Picker("文字颜色", selection: $script.textColorPreset) {
                        ForEach(TextColorPreset.editorChoices) { preset in
                            Text(preset.name).tag(preset)
                        }
                    }
                    .pickerStyle(.segmented)

                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Label("摄像头透明度", systemImage: "camera.aperture")
                            Spacer()
                            Text("\(Int(cameraTransparency * 100))%")
                                .foregroundStyle(.secondary)
                        }
                        Slider(value: cameraTransparencyBinding, in: 0.18...0.82, step: 0.02)
                    }
                }
            }
            .scrollContentBackground(.hidden)
        }
    }

    private func save() {
        if script.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            script.title = "未命名文稿"
        }
        store.save(script)
    }

    private func startPrompting() {
        guard canStartPrompting else { return }

        editorFocused = false
        normalizeDisplaySettings()
        save()

        DispatchQueue.main.async {
            showPrompter = true
        }
    }

    private var displayTitle: String {
        let title = script.title.trimmingCharacters(in: .whitespacesAndNewlines)
        return title.isEmpty ? "未命名文稿" : title
    }

    private var cameraTransparency: Double {
        min(0.82, max(0.18, 1 - script.overlayOpacity))
    }

    private var cameraTransparencyBinding: Binding<Double> {
        Binding(
            get: { cameraTransparency },
            set: { script.overlayOpacity = min(0.82, max(0.18, 1 - $0)) }
        )
    }

    private var editorActions: some View {
        HStack(spacing: 10) {
            editorIconButton(
                systemName: "doc.on.clipboard",
                label: "粘贴正文"
            ) {
                pasteClipboardContent()
            }

            editorIconButton(
                systemName: "trash",
                label: "清空正文",
                isDisabled: script.content.isEmpty
            ) {
                showClearPanel = true
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 12)
        .padding(.vertical, 9)
        .background(.ultraThinMaterial, in: Capsule())
        .overlay(
            Capsule()
                .stroke(.white.opacity(0.24), lineWidth: 0.7)
        )
    }

    private func editorIconButton(
        systemName: String,
        label: String,
        isDisabled: Bool = false,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 15, weight: .semibold))
                .frame(width: 34, height: 34)
                .foregroundStyle(isDisabled ? Color.secondary.opacity(0.45) : Color.primary)
                .background(.ultraThinMaterial, in: Circle())
                .overlay(
                    Circle()
                        .stroke(.white.opacity(0.20), lineWidth: 0.7)
                )
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .disabled(isDisabled)
        .accessibilityLabel(label)
    }

    private func beginTitleEditing() {
        titleDraft = displayTitle
        showTitleEditor = true
    }

    private func pasteClipboardContent() {
        guard let pasted = UIPasteboard.general.string,
              !pasted.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        else {
            return
        }

        if script.content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            script.content = pasted
        } else {
            script.content += "\n" + pasted
        }
        editorFocused = true
    }

    private var clearPanelContent: some View {
        VStack(spacing: 10) {
            HStack {
                Text("清空正文")
                    .font(.system(size: 17, weight: .semibold, design: .rounded))

                Spacer()

                Button {
                    showClearPanel = false
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .bold))
                        .frame(width: 30, height: 30)
                        .background(.white.opacity(0.36), in: Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("关闭")
            }
            .padding(.horizontal, 4)
            .padding(.bottom, 4)

            Text("正文会被清空，文稿名和显示设置会保留。")
                .font(.system(size: 14))
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 4)

            GlassActionRow(
                systemName: "trash",
                title: "清空正文",
                subtitle: nil,
                isDestructive: true
            ) {
                script.content = ""
                showClearPanel = false
                editorFocused = true
            }
        }
    }

    private func normalizeDisplaySettings() {
        if !TextColorPreset.editorChoices.contains(script.textColorPreset) {
            script.textColorPreset = .white
        }
        script.fontSize = min(110, max(12, script.fontSize))
    }
}

private enum EditorTab: String, CaseIterable, Identifiable {
    case script
    case display

    var id: String { rawValue }

    var title: String {
        switch self {
        case .script: "文稿"
        case .display: "显示"
        }
    }
}

private extension View {
    @ViewBuilder
    func editorGlassButton() -> some View {
        let shape = Capsule()

        if #available(iOS 26.0, *) {
            glassEffect(.regular.tint(.white.opacity(0.06)).interactive(), in: shape)
                .overlay(
                    shape
                        .stroke(
                            LinearGradient(
                                colors: [
                                    .white.opacity(0.58),
                                    .white.opacity(0.22),
                                    .white.opacity(0.10)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 0.8
                        )
                )
                .shadow(color: .black.opacity(0.08), radius: 18, y: 8)
        } else {
            background(.ultraThinMaterial, in: shape)
                .overlay(
                    shape
                        .stroke(
                            LinearGradient(
                                colors: [
                                    .white.opacity(0.62),
                                    .white.opacity(0.22),
                                    .black.opacity(0.08)
                                ],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 0.8
                        )
                )
                .shadow(color: .black.opacity(0.08), radius: 14, y: 7)
        }
    }

}

#Preview {
    NavigationStack {
        ScriptEditorView(
            script: Script(title: "试用", content: "第一句。\n第二句。")
        )
        .environmentObject(ScriptStore())
    }
}
