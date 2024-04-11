import 'dart:io';

import 'package:better_player/src/air_play/air_play_button.dart';
import 'package:better_player/src/chrome_cast/chrome_cast_button.dart';
import 'package:flutter/material.dart';

class BetterPlayerCastButton extends StatelessWidget {
  final Color iconColor;
  final double iconSize;

  const BetterPlayerCastButton({
    Key? key,
    required this.iconColor,
    required this.iconSize,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    if (Platform.isAndroid) {
      return ChromeCastButton(
        color: iconColor,
      );
    }

    if (Platform.isIOS) {
      return AirPlayButton(
        color: iconColor,
      );
    }
    return SizedBox.shrink();
  }
}
