// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package com.jhomlala.better_player

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.DrmConfiguration
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import java.util.Locale


class RemoteVideoAsset(
  url: String,
  private val streamingFormat: StreamingFormat,
  private val httpHeaders: Map<String, String>,
  private val metadata: MediaMetadata,
  private val licenseUrl: String?,
  private val drmHeaders: Map<String, String>?,
  private val clearKey: String?,
  private val cacheKey: String?,
) :
  VideoAsset(url) {

  private val mimeType: String
    @OptIn(UnstableApi::class)
    get() {
      return when (streamingFormat) {
        StreamingFormat.SMOOTH -> MimeTypes.APPLICATION_SS
        StreamingFormat.DYNAMIC_ADAPTIVE -> MimeTypes.APPLICATION_MPD
        StreamingFormat.HTTP_LIVE -> MimeTypes.APPLICATION_M3U8
        StreamingFormat.UNKNOWN -> {
          val fileExtension = MimeTypeMap.getFileExtensionFromUrl(
            url
          )

          val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            fileExtension.lowercase(Locale.getDefault())
          )

          type ?: MimeTypes.VIDEO_UNKNOWN
        }
      }
    }

  override
  val mediaItem: MediaItem
    @OptIn(UnstableApi::class)
    get() {
      val builder: MediaItem.Builder = MediaItem.Builder().setUri(url).setMimeType(mimeType)

      if (licenseUrl != null) {
        val drmBuilder =
          DrmConfiguration.Builder(Util.getDrmUuid("widevine")!!)

        if (drmHeaders != null) {
          drmBuilder.setLicenseRequestHeaders(drmHeaders)
        }

        builder.setDrmConfiguration(drmBuilder.build())
      } else if (!clearKey.isNullOrEmpty()) {
        val drmBuilder =
          DrmConfiguration.Builder(Util.getDrmUuid("clearkey")!!)
        builder.setDrmConfiguration(drmBuilder.build())
      }

      if (!cacheKey.isNullOrEmpty()) {
        builder.setCustomCacheKey(cacheKey)
      }

      builder.setMediaMetadata(metadata)

      return builder.build()
    }

  override
  fun getMediaSourceFactory(context: Context): MediaSource.Factory {
    return getMediaSourceFactory(context, DefaultHttpDataSource.Factory())
  }

  /**
   * Returns a configured media source factory, starting at the provided factory.
   *
   *
   * This method is provided for ease of testing without making real HTTP calls.
   *
   * @param context application context.
   * @param initialFactory initial factory, to be configured.
   * @return configured factory, or `null` if not needed for this asset type.
   */
  @VisibleForTesting
  fun getMediaSourceFactory(
    context: Context, initialFactory: DefaultHttpDataSource.Factory
  ): MediaSource.Factory {
    var userAgent: String? = DEFAULT_USER_AGENT
    if (httpHeaders.isNotEmpty() && httpHeaders.containsKey(HEADER_USER_AGENT)) {
      userAgent = httpHeaders[HEADER_USER_AGENT]
    }
    unstableUpdateDataSourceFactory(initialFactory, httpHeaders, userAgent)
    val dataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(
      context, initialFactory
    )
    return DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory)
  }

  companion object {
    private const val DEFAULT_USER_AGENT = "ExoPlayer"
    private const val HEADER_USER_AGENT = "User-Agent"

    // TODO: Migrate to stable API, see https://github.com/flutter/flutter/issues/147039.
    @OptIn(UnstableApi::class)
    private fun unstableUpdateDataSourceFactory(
      factory: DefaultHttpDataSource.Factory,
      httpHeaders: Map<String, String>,
      userAgent: String?
    ) {
      factory.setUserAgent(userAgent).setAllowCrossProtocolRedirects(true)
      if (httpHeaders.isNotEmpty()) {
        factory.setDefaultRequestProperties(httpHeaders)
      }
    }
  }
}