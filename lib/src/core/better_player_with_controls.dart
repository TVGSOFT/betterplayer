import 'dart:async';
import 'dart:io';
import 'dart:math';

import 'package:better_player/better_player.dart';
import 'package:better_player/src/configuration/better_player_controller_event.dart';
import 'package:better_player/src/controls/better_player_casting_placeholder.dart';
import 'package:better_player/src/controls/better_player_cupertino_controls.dart';
import 'package:better_player/src/controls/better_player_material_controls.dart';
import 'package:better_player/src/core/better_player_utils.dart';
import 'package:better_player/src/subtitles/better_player_subtitles_drawer.dart';
import 'package:better_player/src/video_player/video_player.dart';
import 'package:flutter/material.dart';

class BetterPlayerWithControls extends StatefulWidget {
  final BetterPlayerController? controller;

  const BetterPlayerWithControls({Key? key, this.controller}) : super(key: key);

  @override
  _BetterPlayerWithControlsState createState() =>
      _BetterPlayerWithControlsState();
}

class _BetterPlayerWithControlsState extends State<BetterPlayerWithControls> {
  BetterPlayerSubtitlesConfiguration get subtitlesConfiguration =>
      widget.controller!.betterPlayerConfiguration.subtitlesConfiguration;

  BetterPlayerControlsConfiguration get controlsConfiguration =>
      widget.controller!.betterPlayerControlsConfiguration;

  final StreamController<bool> playerVisibilityStreamController =
      StreamController();

  bool _initialized = false;

  StreamSubscription? _controllerEventSubscription;

  double _aspectRatio = 16 / 9;
  BoxFit _boxFit = BoxFit.contain;

  final playerKey = GlobalKey();

  @override
  void initState() {
    playerVisibilityStreamController.add(true);
    _controllerEventSubscription =
        widget.controller!.controllerEventStream.listen(_onControllerChanged);
    super.initState();
  }

  @override
  void didUpdateWidget(BetterPlayerWithControls oldWidget) {
    if (oldWidget.controller != widget.controller) {
      _controllerEventSubscription?.cancel();
      _controllerEventSubscription =
          widget.controller!.controllerEventStream.listen(_onControllerChanged);
    }
    super.didUpdateWidget(oldWidget);
  }

  @override
  void dispose() {
    playerVisibilityStreamController.close();
    _controllerEventSubscription?.cancel();
    super.dispose();
  }

  void _onControllerChanged(BetterPlayerControllerEvent event) {
    if (!_initialized) {
      setState(() {
        _initialized = true;
      });
    }

    switch (event) {
      case BetterPlayerControllerEvent.changeAspectRatio:
        setState(() {
          _aspectRatio = widget.controller!.getAspectRatio() ?? 16 / 9;
        });
        break;
      case BetterPlayerControllerEvent.changeBoxFit:
        setState(() {
          _boxFit = widget.controller!.getFit();
        });
        break;
      case BetterPlayerControllerEvent.openFullscreen:
      case BetterPlayerControllerEvent.hideFullscreen:
        setState(() {
          if (widget.controller!.isFullScreen) {
            if (widget.controller!.betterPlayerConfiguration
                    .autoDetectFullscreenDeviceOrientation ||
                widget.controller!.betterPlayerConfiguration
                    .autoDetectFullscreenAspectRatio) {
              _aspectRatio =
                  widget.controller!.videoPlayerController?.value.aspectRatio ??
                      1.0;

              return;
            } else if (widget.controller!.betterPlayerConfiguration
                    .fullScreenAspectRatio !=
                null) {
              _aspectRatio = widget
                  .controller!.betterPlayerConfiguration.fullScreenAspectRatio!;

              return;
            }
          }

          _aspectRatio = widget.controller!.getAspectRatio() ?? 16 / 9;
        });
        break;
      default:
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    final BetterPlayerController betterPlayerController =
        BetterPlayerController.of(context);

    return Container(
      key: playerKey,
      width: double.infinity,
      color: betterPlayerController
          .betterPlayerConfiguration.controlsConfiguration.backgroundColor,
      alignment: betterPlayerController.betterPlayerConfiguration.expandToFill
          ? Alignment.center
          : null,
      child: _buildPlayerWithControls(
        betterPlayerController,
        _aspectRatio,
        context,
      ),
    );
  }

  Widget _buildPlayerWithControls(BetterPlayerController betterPlayerController,
      double aspectRatio, BuildContext context) {
    final configuration = betterPlayerController.betterPlayerConfiguration;
    var rotation = configuration.rotation;

    if (!(rotation <= 360 && rotation % 90 == 0)) {
      BetterPlayerUtils.log("Invalid rotation provided. Using rotation = 0");
      rotation = 0;
    }
    if (betterPlayerController.betterPlayerDataSource == null) {
      return Container();
    }
    _initialized = true;

    final bool placeholderOnTop =
        betterPlayerController.betterPlayerConfiguration.placeholderOnTop;

    if (betterPlayerController.betterPlayerConfiguration.controlsConfiguration
        .enableControlsFullScreen) {
      return Container(
        child: Stack(
          fit: StackFit.passthrough,
          children: <Widget>[
            if (placeholderOnTop) _buildPlaceholder(betterPlayerController),
            Center(
              child: AspectRatio(
                aspectRatio: aspectRatio,
                child: Transform.rotate(
                  angle: rotation * pi / 180,
                  child: _BetterPlayerVideoFitWidget(
                    betterPlayerController,
                    _boxFit,
                  ),
                ),
              ),
            ),
            betterPlayerController.betterPlayerConfiguration.overlay ??
                SizedBox.shrink(),
            BetterPlayerSubtitlesDrawer(
              betterPlayerController: betterPlayerController,
              betterPlayerSubtitlesConfiguration: subtitlesConfiguration,
              subtitles: betterPlayerController.subtitlesLines,
              playerVisibilityStream: playerVisibilityStreamController.stream,
            ),
            if (!placeholderOnTop) _buildPlaceholder(betterPlayerController),
            BetterPlayerCastingPlaceholder(),
            _buildControls(context, betterPlayerController),
          ],
        ),
      );
    }

    return AspectRatio(
      aspectRatio: aspectRatio,
      child: Container(
        child: Stack(
          fit: StackFit.passthrough,
          children: <Widget>[
            if (placeholderOnTop) _buildPlaceholder(betterPlayerController),
            Transform.rotate(
              angle: rotation * pi / 180,
              child: _BetterPlayerVideoFitWidget(
                betterPlayerController,
                _boxFit,
              ),
            ),
            betterPlayerController.betterPlayerConfiguration.overlay ??
                SizedBox.shrink(),
            BetterPlayerSubtitlesDrawer(
              betterPlayerController: betterPlayerController,
              betterPlayerSubtitlesConfiguration: subtitlesConfiguration,
              subtitles: betterPlayerController.subtitlesLines,
              playerVisibilityStream: playerVisibilityStreamController.stream,
            ),
            if (!placeholderOnTop) _buildPlaceholder(betterPlayerController),
            BetterPlayerCastingPlaceholder(),
            _buildControls(context, betterPlayerController),
          ],
        ),
      ),
    );
  }

  Widget _buildPlaceholder(BetterPlayerController betterPlayerController) {
    return betterPlayerController.betterPlayerDataSource!.placeholder ??
        betterPlayerController.betterPlayerConfiguration.placeholder ??
        Container();
  }

  Widget _buildControls(
    BuildContext context,
    BetterPlayerController betterPlayerController,
  ) {
    if (controlsConfiguration.showControls) {
      BetterPlayerTheme? playerTheme = controlsConfiguration.playerTheme;
      if (playerTheme == null) {
        if (Platform.isAndroid) {
          playerTheme = BetterPlayerTheme.material;
        } else {
          playerTheme = BetterPlayerTheme.cupertino;
        }
      }

      if (controlsConfiguration.customControlsBuilder != null &&
          playerTheme == BetterPlayerTheme.custom) {
        return controlsConfiguration.customControlsBuilder!(
            betterPlayerController, onControlsVisibilityChanged);
      } else if (playerTheme == BetterPlayerTheme.material) {
        return _buildMaterialControl();
      } else if (playerTheme == BetterPlayerTheme.cupertino) {
        return _buildCupertinoControl();
      }
    }

    return const SizedBox();
  }

  Widget _buildMaterialControl() {
    return BetterPlayerMaterialControls(
      playerKey: playerKey,
      onControlsVisibilityChanged: onControlsVisibilityChanged,
      controlsConfiguration: controlsConfiguration,
    );
  }

  Widget _buildCupertinoControl() {
    return BetterPlayerCupertinoControls(
      playerKey: playerKey,
      onControlsVisibilityChanged: onControlsVisibilityChanged,
      controlsConfiguration: controlsConfiguration,
    );
  }

  void onControlsVisibilityChanged(bool state) {
    playerVisibilityStreamController.add(state);
  }
}

///Widget used to set the proper box fit of the video. Default fit is 'fill'.
class _BetterPlayerVideoFitWidget extends StatefulWidget {
  const _BetterPlayerVideoFitWidget(
    this.betterPlayerController,
    this.boxFit, {
    Key? key,
  }) : super(key: key);

  final BetterPlayerController betterPlayerController;
  final BoxFit boxFit;

  @override
  _BetterPlayerVideoFitWidgetState createState() =>
      _BetterPlayerVideoFitWidgetState();
}

class _BetterPlayerVideoFitWidgetState
    extends State<_BetterPlayerVideoFitWidget> {
  VideoPlayerController? get controller =>
      widget.betterPlayerController.videoPlayerController;

  bool _initialized = false;

  VoidCallback? _initializedListener;

  bool _started = false;

  StreamSubscription? _controllerEventSubscription;

  @override
  void initState() {
    super.initState();
    if (!widget.betterPlayerController.betterPlayerConfiguration
        .showPlaceholderUntilPlay) {
      _started = true;
    } else {
      _started = widget.betterPlayerController.hasCurrentDataSourceStarted;
    }

    _initialize();
  }

  @override
  void didUpdateWidget(_BetterPlayerVideoFitWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.betterPlayerController.videoPlayerController != controller) {
      if (_initializedListener != null) {
        oldWidget.betterPlayerController.videoPlayerController!
            .removeListener(_initializedListener!);
      }
      _initialized = false;
      _initialize();
    }
  }

  void _initialize() {
    if (controller?.value.initialized == false) {
      _initializedListener = () {
        if (!mounted) {
          return;
        }

        if (_initialized != controller!.value.initialized) {
          _initialized = controller!.value.initialized;
          setState(() {});
        }
      };
      controller!.addListener(_initializedListener!);
    } else {
      _initialized = true;
    }

    _controllerEventSubscription =
        widget.betterPlayerController.controllerEventStream.listen((event) {
      switch (event) {
        case BetterPlayerControllerEvent.play:
          if (!_started) {
            setState(() {
              _started =
                  widget.betterPlayerController.hasCurrentDataSourceStarted;
            });
          }
          break;
        case BetterPlayerControllerEvent.setupDataSource:
          if (!_started) {
            setState(() {
              _started = false;
            });
          }
          break;
        default:
          break;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_initialized && _started) {
      return Center(
        child: ClipRect(
          child: Container(
            width: double.infinity,
            height: double.infinity,
            child: FittedBox(
              fit: widget.boxFit,
              child: SizedBox(
                width: controller!.value.size?.width ?? 0,
                height: controller!.value.size?.height ?? 0,
                child: VideoPlayer(controller),
              ),
            ),
          ),
        ),
      );
    } else {
      return const SizedBox();
    }
  }

  @override
  void dispose() {
    if (_initializedListener != null) {
      widget.betterPlayerController.videoPlayerController!
          .removeListener(_initializedListener!);
    }
    _controllerEventSubscription?.cancel();
    super.dispose();
  }
}
