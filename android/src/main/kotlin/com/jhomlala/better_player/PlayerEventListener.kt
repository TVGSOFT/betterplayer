// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package com.jhomlala.better_player

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player

class PlayerEventListener(private val player: Player, private val events: VideoPlayerCallbacks) :
  Player.Listener {
  private var isBuffering = false
  private var isInitialized = false

  private fun setBuffering(buffering: Boolean) {
    if (isBuffering == buffering) {
      return
    }
    isBuffering = buffering
    if (buffering) {
      events.onBufferingStart()
    } else {
      events.onBufferingEnd()
    }
  }

  private fun sendInitialized() {
    if (isInitialized) {
      return
    }

    isInitialized = true
    val videoSize = player.videoSize
    var rotationCorrection = 0
    var width = videoSize.width
    var height = videoSize.height
    if (width != 0 && height != 0) {
      val rotationDegrees = videoSize.unappliedRotationDegrees
      // Switch the width/height if video was taken in portrait mode
      if (rotationDegrees == 90 || rotationDegrees == 270) {
        width = videoSize.height
        height = videoSize.width
      }
      // Rotating the video with ExoPlayer does not seem to be possible with a Surface,
      // so inform the Flutter code that the widget needs to be rotated to prevent
      // upside-down playback for videos with rotationDegrees of 180 (other orientations work
      // correctly without correction).
      if (rotationDegrees == 180) {
        rotationCorrection = rotationDegrees
      }
    }
    events.onInitialized(width, height, player.duration, rotationCorrection)
  }

  override fun onPlaybackStateChanged(playbackState: Int) {
    when (playbackState) {
      Player.STATE_BUFFERING -> {
        setBuffering(true)
        events.onBufferingUpdate(player.bufferedPosition)
      }

      Player.STATE_READY -> sendInitialized()
      Player.STATE_ENDED -> events.onCompleted()
      Player.STATE_IDLE -> {}
    }
    if (playbackState != Player.STATE_BUFFERING) {
      setBuffering(false)
    }
  }

  override fun onPlayerError(error: PlaybackException) {
    setBuffering(false)
    if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
      // See https://exoplayer.dev/live-streaming.html#behindlivewindowexception-and-error_code_behind_live_window
      player.seekToDefaultPosition()
      player.prepare()
    } else {
      events.onError("VideoError", "Video player had error $error", null)
    }
  }

  override fun onIsPlayingChanged(isPlaying: Boolean) {
    events.onIsPlayingStateUpdate(isPlaying)
  }
}