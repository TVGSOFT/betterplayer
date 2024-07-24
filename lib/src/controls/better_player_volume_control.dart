import 'package:flutter/material.dart';

class BetterPlayerVolumeControl extends StatelessWidget {
  const BetterPlayerVolumeControl({
    super.key,
    required this.volume,
    required this.onChanged,
    this.onDragStart,
    this.onDragEnd,
  });

  static const _iconNames = [
    'ic_volume_mute.png',
    'ic_volume_low.png',
    'ic_volume_medium.png',
    'ic_volume_high.png',
  ];

  final double volume;
  final void Function(double value) onChanged;
  final Function()? onDragStart;
  final Function()? onDragEnd;

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
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Image.asset(
          'assets/${_getIcon(volume)}',
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
              value: volume,
              onChanged: onChanged,
              onChangeStart: (value) => onDragStart?.call(),
              onChangeEnd: (value) => onDragEnd?.call(),
            ),
          ),
        ),
      ],
    );
  }
}
