package com.jhomlala.better_player

import android.net.Uri
import androidx.media3.common.MediaMetadata

class MediaMetadataUtils private constructor() {

  companion object {
    fun toMediaMetadata(map: Map<String, String>): MediaMetadata {
      val builder = MediaMetadata.Builder()

      if (!map["title"].isNullOrEmpty()) {
        builder.setTitle(map["title"])
      }

      if (!map["artworkUri"].isNullOrEmpty()) {
        builder.setArtworkUri(Uri.parse(map["artworkUri"]))
      }

      return builder.build()
    }
  }
}