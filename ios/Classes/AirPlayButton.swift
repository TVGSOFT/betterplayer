//
//  AirPlayButton.swift
//  better_player
//
//  Created by Giap Tran Van on 11/04/2024.
//

import Flutter
import AVKit

public class AirPlayButton: NSObject, FlutterPlatformView {

    // MARK: - Internal properties

    private let airPlayButton: AVRoutePickerView
    private let audioSession = AVAudioSession.sharedInstance()
    private let routeDetector = AVRouteDetector()

    // MARK: - Init

    init(
        withFrame frame: CGRect,
        viewIdentifier viewId: Int64,
        arguments args: Any?
    ) {
        self.airPlayButton = AVRoutePickerView(frame: frame)
        super.init()
        self.configure(arguments: args)
    }

    public func view() -> UIView {
        return airPlayButton
    }

    private func configure(arguments args: Any?) {
        airPlayButton.delegate = self
        setTintColor(arguments: args)
        setActiveTintColor(arguments: args)
    }

    // MARK: - Styling

    private func setTintColor(arguments args: Any?) {
        guard
            let args = args as? [String: Any],
            let red = args["red"] as? CGFloat,
            let green = args["green"] as? CGFloat,
            let blue = args["blue"] as? CGFloat,
            let alpha = args["alpha"] as? Int
            else {
                return
        }
        airPlayButton.tintColor = UIColor(
            red: red / 255,
            green: green / 255,
            blue: blue / 255,
            alpha: CGFloat(alpha) / 255
        )
    }

    private func setActiveTintColor(arguments args: Any?) {
        guard
            let args = args as? [String: Any],
            let red = args["activeRed"] as? CGFloat,
            let green = args["activeGreen"] as? CGFloat,
            let blue = args["activeBlue"] as? CGFloat,
            let alpha = args["activeAlpha"] as? Int
            else {
                return
        }
        airPlayButton.activeTintColor = UIColor(
            red: red / 255,
            green: green / 255,
            blue: blue / 255,
            alpha: CGFloat(alpha) / 255
        )
    }

    private func isConnected() -> Bool {
        return audioSession.currentRoute.outputs.contains { $0.portType == AVAudioSession.Port.airPlay }
    }
}

// MARK: - AVRoutePickerViewDelegate

extension AirPlayButton: AVRoutePickerViewDelegate {
    public func routePickerViewWillBeginPresentingRoutes(_ routePickerView: AVRoutePickerView) {
        
    }

    public func routePickerViewDidEndPresentingRoutes(_ routePickerView: AVRoutePickerView) {
        
    }
}

