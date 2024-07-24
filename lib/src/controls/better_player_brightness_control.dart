import 'package:flutter/material.dart';
import 'package:screen_brightness/screen_brightness.dart';

class BetterPlayerBrightnessControl extends StatelessWidget {
  const BetterPlayerBrightnessControl({
    super.key,
    this.onDragStart,
    this.onDragEnd,
  });

  static const _iconNames = [
    'ic_brightness_low.png',
    'ic_brightness_low.png',
    'ic_brightness_medium.png',
    'ic_brightness_high.png',
  ];

  final Function()? onDragStart;
  final Function()? onDragEnd;

  Future<void> setBrightness(double brightness) async {
    try {
      await ScreenBrightness.instance.setScreenBrightness(brightness);
    } catch (e) {
      debugPrint(e.toString());
    }
  }

  String _getIcon(double value) {
    if (value <= 0) {
      return _iconNames[0];
    }

    if (value <= 0.4) {
      return _iconNames[1];
    }

    if (value <= 0.7) {
      return _iconNames[2];
    }

    return _iconNames[3];
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<double>(
      future: ScreenBrightness.instance.current,
      builder: (context, snapshot) {
        final currentBrightness = snapshot.data ?? 0.0;

        return StreamBuilder<double>(
          stream: ScreenBrightness.instance.onCurrentBrightnessChanged,
          builder: (context, snapshot) {
            final brightness = snapshot.data ?? currentBrightness;

            return Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                Image.asset(
                  'assets/${_getIcon(brightness)}',
                  package: 'better_player',
                  width: 16,
                  height: 16,
                ),
                RotatedBox(
                  quarterTurns: -1,
                  child: SliderTheme(
                    data: SliderTheme.of(context).copyWith(
                      trackHeight: 4,
                      thumbColor: Colors.transparent,
                      thumbShape: RoundSliderThumbShape(
                        enabledThumbRadius: 3.0,
                        elevation: 0,
                      ),
                    ),
                    child: Slider(
                      min: 0,
                      max: 1,
                      activeColor: Colors.white,
                      secondaryActiveColor: Colors.grey,
                      value: brightness,
                      onChanged: setBrightness,
                      onChangeStart: (value) => onDragStart?.call(),
                      onChangeEnd: (value) => onDragEnd?.call(),
                    ),
                  ),
                ),
              ],
            );
          },
        );
      },
    );
  }
}
