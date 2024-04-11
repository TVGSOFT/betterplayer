//
//  AirPlayButtonFactory.swift
//  better_player
//
//  Created by Giap Tran Van on 11/04/2024.
//

import Flutter

public class AirPlayButtonFactory: NSObject, FlutterPlatformViewFactory {
    public func create(withFrame frame: CGRect, viewIdentifier viewId: Int64, arguments args: Any?) -> FlutterPlatformView {
        return AirPlayButton(withFrame: frame, viewIdentifier: viewId, arguments: args)
    }

    public func createArgsCodec() -> FlutterMessageCodec & NSObjectProtocol {
        return FlutterStandardMessageCodec.sharedInstance()
    }
}

