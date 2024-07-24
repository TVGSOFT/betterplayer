import 'dart:async';

import 'package:better_player/src/core/better_player_controller.dart';
import 'package:better_player/src/video_player/video_player.dart';
import 'package:flutter/material.dart';

class BetterPlayerCastingPlaceholder extends StatefulWidget {
  const BetterPlayerCastingPlaceholder({
    Key? key,
  }) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _BetterPlayerCastingPlaceholderState();
  }
}

class _BetterPlayerCastingPlaceholderState
    extends State<BetterPlayerCastingPlaceholder> {
  VideoPlayerValue? _latestValue;
  VideoPlayerController? _controller;
  BetterPlayerController? _betterPlayerController;

  @override
  Widget build(BuildContext context) {
    final isCasting = _latestValue?.isCasting ?? false;

    if (!isCasting) {
      return const SizedBox.shrink();
    }

    return Container(
      color: Colors.black,
      alignment: Alignment.center,
      child: const Icon(
        Icons.cast_connected,
        color: Colors.white,
        size: 128,
      ),
    );
  }

  @override
  void dispose() {
    _dispose();
    super.dispose();
  }

  void _dispose() {
    _controller!.removeListener(_updateState);
  }

  @override
  void didChangeDependencies() {
    final _oldController = _betterPlayerController;
    _betterPlayerController = BetterPlayerController.of(context);
    _controller = _betterPlayerController!.videoPlayerController;

    if (_oldController != _betterPlayerController) {
      _dispose();
      _initialize();
    }

    super.didChangeDependencies();
  }

  Future<void> _initialize() async {
    _controller!.addListener(_updateState);

    _updateState();
  }

  void _updateState() {
    if (mounted) {
      setState(() {
        _latestValue = _controller!.value;
      });
    }
  }
}
