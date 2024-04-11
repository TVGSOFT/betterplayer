//
//  AirPlayButtonPlugin.swift
//  better_player
//
//  Created by Giap Tran Van on 11/04/2024.
//

import Flutter
import UIKit

public class AirPlayPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let factory = AirPlayButtonFactory()
    
    registrar.register(factory, withId: "AirPlayButton")
  }
}
