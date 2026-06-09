import Combine
import QuartzCore
import SwiftUI

@MainActor
final class ScrollEngine: ObservableObject {
    @Published var offset: CGFloat = 0
    @Published var isPlaying = false
    @Published var speed: Double = 80

    private var displayLink: CADisplayLink?
    private var lastTimestamp: CFTimeInterval = 0
    private var lineHeight: CGFloat = 84
    private var averageCharactersPerLine: CGFloat = 18
    private var maximumOffset: CGFloat = 0
    private var followTargetOffset: CGFloat?

    deinit {
        displayLink?.invalidate()
    }

    func configure(
        speed: Double,
        lineHeight: CGFloat,
        averageCharactersPerLine: CGFloat,
        maximumOffset: CGFloat
    ) {
        self.speed = speed
        self.lineHeight = max(40, lineHeight)
        self.averageCharactersPerLine = max(6, averageCharactersPerLine)
        self.maximumOffset = max(0, maximumOffset)
        offset = min(offset, self.maximumOffset)
        if let followTargetOffset {
            self.followTargetOffset = min(self.maximumOffset, max(0, followTargetOffset))
        }
    }

    func play() {
        guard !isPlaying else { return }
        followTargetOffset = nil
        isPlaying = true
        ensureDisplayLink()
    }

    func pause() {
        isPlaying = false
    }

    func toggle() {
        isPlaying ? pause() : play()
    }

    func setSpeed(_ value: Double) {
        speed = min(260, max(20, value))
    }

    func setOffset(_ value: CGFloat) {
        followTargetOffset = nil
        applyOffset(value)
    }

    func follow(to value: CGFloat) {
        followTargetOffset = min(maximumOffset, max(0, value))
        isPlaying = false
        ensureDisplayLink()
    }

    func stopFollowing() {
        followTargetOffset = nil
    }

    private func applyOffset(_ value: CGFloat) {
        offset = min(maximumOffset, max(0, value))
        if offset >= maximumOffset {
            pause()
        }
    }

    func reset() {
        offset = 0
        followTargetOffset = nil
        pause()
    }

    private func ensureDisplayLink() {
        guard displayLink == nil else { return }
        let link = CADisplayLink(target: self, selector: #selector(tick(_:)))
        if #available(iOS 15.0, *) {
            link.preferredFrameRateRange = CAFrameRateRange(minimum: 30, maximum: 60, preferred: 60)
        } else {
            link.preferredFramesPerSecond = 60
        }
        link.add(to: .main, forMode: .common)
        displayLink = link
    }

    @objc private func tick(_ link: CADisplayLink) {
        guard isPlaying || followTargetOffset != nil else {
            lastTimestamp = link.timestamp
            return
        }

        if lastTimestamp == 0 {
            lastTimestamp = link.timestamp
            return
        }

        let delta = link.timestamp - lastTimestamp
        lastTimestamp = link.timestamp

        if let target = followTargetOffset {
            let response = min(1, CGFloat(delta) * 12)
            let nextOffset = offset + (target - offset) * response

            if abs(nextOffset - target) < 0.5 {
                applyOffset(target)
                followTargetOffset = nil
            } else {
                applyOffset(nextOffset)
            }
            return
        }

        let visualTuningFactor = 1.85
        let linesPerSecond = (speed / Double(averageCharactersPerLine)) / 60 * visualTuningFactor
        let pixelsPerSecond = CGFloat(linesPerSecond) * lineHeight
        applyOffset(offset + pixelsPerSecond * CGFloat(delta))
    }
}
