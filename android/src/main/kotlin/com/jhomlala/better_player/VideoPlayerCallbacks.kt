// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package com.jhomlala.better_player

/**
 * Callbacks representing events invoked by [BetterPlayer].
 *
 *
 * In the actual plugin, this will always be [VideoPlayerEventCallbacks], which creates the
 * expected events to send back through the plugin channel. In tests methods can be overridden in
 * order to assert results.
 *
 *
 * See [androidx.media3.common.Player.Listener] for details.
 */
interface VideoPlayerCallbacks {
  fun onInitialized(width: Int, height: Int, durationInMs: Long, rotationCorrectionInDegrees: Int)

  fun onBufferingStart()

  fun onBufferingUpdate(bufferedPosition: Long)

  fun onBufferingEnd()

  fun onCompleted()

  fun onError(code: String, message: String?, details: Any?)

  fun onIsPlayingStateUpdate(isPlaying: Boolean)

  fun onCastingStart()

  fun onCastingEnd()

  fun onPipStart()

  fun onPipEnd()
}