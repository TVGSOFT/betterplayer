// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package com.jhomlala.better_player

import androidx.annotation.VisibleForTesting
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler

class VideoPlayerEventCallbacks(private val eventSink: EventSink) : VideoPlayerCallbacks {
  override fun onInitialized(
    width: Int, height: Int, durationInMs: Long, rotationCorrectionInDegrees: Int
  ) {
    val event: MutableMap<String, Any> = HashMap()
    event["event"] = "initialized"
    event["width"] = width
    event["height"] = height
    event["duration"] = durationInMs
    if (rotationCorrectionInDegrees != 0) {
      event["rotationCorrection"] = rotationCorrectionInDegrees
    }
    eventSink.success(event)
  }

  override fun onBufferingStart() {
    val event: MutableMap<String, Any> = HashMap()
    event["event"] = "bufferingStart"
    eventSink.success(event)
  }

  override fun onBufferingUpdate(bufferedPosition: Long) {
    // iOS supports a list of buffered ranges, so we send as a list with a single range.
    val event: MutableMap<String, Any> = HashMap()
    event["event"] = "bufferingUpdate"

    val range = listOf(0, bufferedPosition)
    event["values"] = listOf(range)
    eventSink.success(event)
  }

  override fun onBufferingEnd() {
    val event: MutableMap<String, Any> = HashMap()
    event["event"] = "bufferingEnd"
    eventSink.success(event)
  }

  override fun onCompleted() {
    val event: MutableMap<String, Any> = HashMap()
    event["event"] = "completed"
    eventSink.success(event)
  }

  override fun onError(code: String, message: String?, details: Any?) {
    eventSink.error(code, message ?: "", details ?: emptyMap<String, Any>())
  }

  override fun onIsPlayingStateUpdate(isPlaying: Boolean) {
    val event: MutableMap<String, Any> = HashMap()
    event["event"] = "isPlayingStateUpdate"
    event["isPlaying"] = isPlaying
    eventSink.success(event)
  }

  override fun onCastingStart() {
    val event: MutableMap<String, Any> = HashMap()
    event["event"] = "castingStart"
    eventSink.success(event)
  }

  override fun onCastingEnd() {
    val event: MutableMap<String, Any> = HashMap()
    event["event"] = "castingEnd"
    eventSink.success(event)
  }

  override fun onPipStart() {
    val event: MutableMap<String, Any> = HashMap()
    event["event"] = "pipStart"
    eventSink.success(event)
  }

  override fun onPipEnd() {
    val event: MutableMap<String, Any> = HashMap()
    event["event"] = "pipEnd"
    eventSink.success(event)
  }


  companion object {
    fun bindTo(eventChannel: EventChannel): VideoPlayerEventCallbacks {
      val eventSink = QueuingEventSink()
      eventChannel.setStreamHandler(object : StreamHandler {
        override fun onListen(arguments: Any?, events: EventSink?) {
          eventSink.setDelegate(events)
        }

        override fun onCancel(arguments: Any?) {
          eventSink.setDelegate(null)
        }
      })
      return withSink(eventSink)
    }

    @VisibleForTesting
    fun withSink(eventSink: EventSink): VideoPlayerEventCallbacks {
      return VideoPlayerEventCallbacks(eventSink)
    }
  }

}