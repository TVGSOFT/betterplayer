// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package com.jhomlala.better_player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource

class LocalVideoAsset(url: String) : VideoAsset(url) {
  override
  val mediaItem: MediaItem
    get() = MediaItem.Builder().setUri(url).build()

  override
  fun getMediaSourceFactory(context: Context): MediaSource.Factory {
    return DefaultMediaSourceFactory(context)
  }
}