// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package com.jhomlala.better_player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.LongSparseArray
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.view.TextureRegistry
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import javax.net.ssl.HttpsURLConnection

/**
 * Android platform implementation of the VideoPlayerPlugin.
 */
class BetterPlayerPlugin : FlutterPlugin, ActivityAware, MethodCallHandler {
  private val videoPlayers = LongSparseArray<BetterPlayer>()
  private var flutterState: FlutterState? = null
  private var activity: Activity? = null
  private var pipHandler: Handler? = null
  private var pipRunnable: Runnable? = null

  override fun onAttachedToEngine(binding: FlutterPluginBinding) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      try {
        HttpsURLConnection.setDefaultSSLSocketFactory(CustomSSLSocketFactory())
      } catch (e: KeyManagementException) {
        Log.w(
          TAG,
          "Failed to enable TLSv1.1 and TLSv1.2 Protocols for API level 19 and below.\n"
              + "For more information about Socket Security, please consult the following link:\n"
              + "https://developer.android.com/reference/javax/net/ssl/SSLSocket",
          e
        )
      } catch (e: NoSuchAlgorithmException) {
        Log.w(
          TAG,
          (("Failed to enable TLSv1.1 and TLSv1.2 Protocols for API level 19 and below.\n"
              + "For more information about Socket Security, please consult the following link:\n"
              + "https://developer.android.com/reference/javax/net/ssl/SSLSocket")),
          e
        )
      }
    }

    val injector = FlutterInjector.instance()

    flutterState = FlutterState(
      binding.applicationContext,
      binding.binaryMessenger,
      { asset: String ->
        injector.flutterLoader().getLookupKeyForAsset(asset)
      },
      { asset: String, packageName: String ->
        injector.flutterLoader().getLookupKeyForAsset(
          asset, packageName
        )
      },
      binding.textureRegistry
    )
    flutterState?.startListening(this)

    binding
      .platformViewRegistry
      .registerViewFactory(
        "ChromeCastButton",
        ChromeCastButtonFactory(),
      )
  }

  override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
    if (flutterState == null) {
      Log.wtf(TAG, "Detached from the engine before registering to it.")
    }

    flutterState?.stopListening()
    flutterState = null

    onDestroy()
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onDetachedFromActivityForConfigChanges() {}

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}

  override fun onDetachedFromActivity() {}

  private fun disposeAllPlayers() {
    for (i in 0 until videoPlayers.size()) {
      videoPlayers.valueAt(i).dispose()
    }
    videoPlayers.clear()
  }

  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    val flutterState = flutterState
    val textureRegistry = flutterState?.textureRegistry

    if (flutterState == null || textureRegistry == null) {
      result.error("no_activity", "better_player plugin requires a foreground activity", null)
      return
    }

    when (call.method) {
      INIT_METHOD -> initialize()
      CREATE_METHOD -> {
        val handle = textureRegistry.createSurfaceTexture()
        val eventChannel = EventChannel(
          flutterState.binaryMessenger, EVENTS_CHANNEL + handle.id()
        )

        val player = BetterPlayer(
          flutterState.applicationContext,
          VideoPlayerEventCallbacks.bindTo(eventChannel),
          handle,
          call.argument<Boolean>("isCastEnabled") ?: false,
        )
        videoPlayers.put(handle.id(), player)

        result.success(mapOf("textureId" to handle.id()))
      }

      else -> {
        val textureId = call.argument<Number>(TEXTURE_ID_PARAMETER)?.toLong() ?: -1
        val player = videoPlayers[textureId]
        if (player == null) {
          result.error(
            "Unknown textureId",
            "No video player associated with texture id $textureId",
            null
          )
          return
        }
        onMethodCall(call, result, textureId, player)
      }
    }
  }

  private fun onMethodCall(
    call: MethodCall,
    result: MethodChannel.Result,
    textureId: Long,
    player: BetterPlayer
  ) {
    when (call.method) {
      SET_DATA_SOURCE_METHOD -> {
        val dataSource = call.argument<Map<String, Any>>(DATA_SOURCE_PARAMETER) ?: HashMap()
        val videoAsset: VideoAsset
        val asset = getParameter(dataSource, ASSET_PARAMETER, "")
        if (asset.isNotEmpty()) {
          val packageName = getParameter(dataSource, PACKAGE_PARAMETER, "")

          val assetLookupKey = if (packageName.isEmpty()) {
            flutterState?.keyForAsset?.get(asset)
          } else {
            flutterState?.keyForAssetAndPackageName?.get(asset, packageName)
          }

          videoAsset = VideoAsset.fromAssetUrl("asset:///$assetLookupKey")
        } else {
          val uri = getParameter(dataSource, URI_PARAMETER, "")
          val httpHeaders =
            getParameter<Map<String, String>>(dataSource, HEADERS_PARAMETER, HashMap())
          val metadata =
            getParameter<Map<String, String>>(dataSource, METADATA_PARAMETER, HashMap())

          val streamingFormat = when (getParameter(dataSource, FORMAT_HINT_PARAMETER, "")) {
            "ss" -> VideoAsset.StreamingFormat.SMOOTH
            "dash" -> VideoAsset.StreamingFormat.DYNAMIC_ADAPTIVE
            "hls" -> VideoAsset.StreamingFormat.HTTP_LIVE
            else -> VideoAsset.StreamingFormat.UNKNOWN
          }

          videoAsset = VideoAsset.fromRemoteUrl(
            uri,
            streamingFormat,
            httpHeaders,
            MediaMetadataUtils.toMediaMetadata(metadata),
            getParameter(dataSource, LICENSE_URL_PARAMETER, null),
            getParameter<Map<String, String>?>(dataSource, DRM_HEADERS_PARAMETER, null),
            getParameter(dataSource, DRM_CLEARKEY_PARAMETER, null),
            getParameter(dataSource, CACHE_KEY_PARAMETER, null),
          )
        }

        player.setupDataSource(videoAsset)
        result.success(null)
      }

      SET_LOOPING_METHOD -> {
        player.setLooping(call.argument(LOOPING_PARAMETER)!!)
        result.success(null)
      }

      SET_VOLUME_METHOD -> {
        player.setVolume(call.argument(VOLUME_PARAMETER)!!)
        result.success(null)
      }

      PLAY_METHOD -> {
        player.play()
        result.success(null)
      }

      PAUSE_METHOD -> {
        player.pause()
        result.success(null)
      }

      SEEK_TO_METHOD -> {
        val location = (call.argument<Any>(LOCATION_PARAMETER) as Number?)!!.toInt()
        player.seekTo(location)
        result.success(null)
      }

      POSITION_METHOD -> {
        player.sendBufferingUpdate()
        result.success(player.position)
      }

      ABSOLUTE_POSITION_METHOD -> result.success(player.absolutePosition)
      SET_SPEED_METHOD -> {
        player.setSpeed(call.argument(SPEED_PARAMETER)!!)
        result.success(null)
      }

      SET_TRACK_PARAMETERS_METHOD -> {
        player.setTrackParameters(
          call.argument(WIDTH_PARAMETER)!!,
          call.argument(HEIGHT_PARAMETER)!!,
          call.argument(BITRATE_PARAMETER)!!
        )
        result.success(null)
      }

      ENABLE_PICTURE_IN_PICTURE_METHOD -> {
        enablePictureInPicture(player)
        result.success(null)
      }

      DISABLE_PICTURE_IN_PICTURE_METHOD -> {
        disablePictureInPicture(player)
        result.success(null)
      }

      IS_PICTURE_IN_PICTURE_SUPPORTED_METHOD -> result.success(
        isPictureInPictureSupported()
      )

      SET_AUDIO_TRACK_METHOD -> {
        val name = call.argument<String?>(NAME_PARAMETER)
        val index = call.argument<Int?>(INDEX_PARAMETER)
        if (name != null && index != null) {
          player.setAudioTrack(name, index)
        }
        result.success(null)
      }

      SET_MIX_WITH_OTHERS_METHOD -> {
        val mixWitOthers = call.argument<Boolean?>(
          MIX_WITH_OTHERS_PARAMETER
        )
        if (mixWitOthers != null) {
          player.setMixWithOthers(mixWitOthers)
        }

        result.success(null)
      }

      DISPOSE_METHOD -> {
        dispose(player, textureId)
        result.success(null)
      }

      else -> result.notImplemented()
    }
  }

  private fun onDestroy() {
    // The whole FlutterView is being destroyed. Here we release resources acquired for all
    // instances
    // of VideoPlayer. Once https://github.com/flutter/flutter/issues/19358 is resolved this may
    // be replaced with just asserting that videoPlayers.isEmpty().
    // https://github.com/flutter/flutter/issues/20989 tracks this.
    disposeAllPlayers()
  }

  private fun initialize() {
    disposeAllPlayers()
  }

  private fun getTextureId(betterPlayer: BetterPlayer): Long? {
    for (index in 0 until videoPlayers.size()) {
      if (betterPlayer === videoPlayers.valueAt(index)) {
        return videoPlayers.keyAt(index)
      }
    }
    return null
  }

  private fun isPictureInPictureSupported(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null && activity!!.packageManager
      .hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
  }

  private fun enablePictureInPicture(player: BetterPlayer) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return
    }

    activity?.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
    startPictureInPictureListenerTimer(player)
    player.onPictureInPictureStatusChanged(true)
  }

  private fun disablePictureInPicture(player: BetterPlayer) {
    stopPipHandler()
    activity?.moveTaskToBack(false)
    player.onPictureInPictureStatusChanged(false)
  }

  private fun startPictureInPictureListenerTimer(player: BetterPlayer) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
      return
    }

    val pipHandler = Handler(Looper.getMainLooper())
    val pipRunnable = Runnable {
      val activity = this.activity ?: return@Runnable

      if (activity.isInPictureInPictureMode) {
        pipRunnable?.let { pipHandler.postDelayed(it, 100) }
      } else {
        player.onPictureInPictureStatusChanged(false)
        stopPipHandler()
      }
    }
    pipHandler.post(pipRunnable)

    this.pipHandler = pipHandler
    this.pipRunnable = pipRunnable
  }

  private fun dispose(player: BetterPlayer, textureId: Long) {
    player.dispose()
    videoPlayers.remove(textureId)
    stopPipHandler()
  }

  private fun stopPipHandler() {
    pipHandler?.removeCallbacksAndMessages(null)
    pipHandler = null
    pipRunnable = null
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T> getParameter(parameters: Map<String, Any?>?, key: String, defaultValue: T): T {
    if (parameters?.containsKey(key) == true) {
      val value = parameters[key]
      if (value != null) {
        return value as T
      }
    }
    return defaultValue
  }

  private fun interface KeyForAssetFn {
    operator fun get(asset: String): String
  }

  private fun interface KeyForAssetAndPackageName {
    operator fun get(asset: String, packageName: String): String
  }

  private class FlutterState(
    val applicationContext: Context,
    val binaryMessenger: BinaryMessenger,
    val keyForAsset: KeyForAssetFn,
    val keyForAssetAndPackageName: KeyForAssetAndPackageName,
    val textureRegistry: TextureRegistry?
  ) {
    private val methodChannel: MethodChannel = MethodChannel(binaryMessenger, CHANNEL)

    fun startListening(methodCallHandler: BetterPlayerPlugin?) {
      methodChannel.setMethodCallHandler(methodCallHandler)
    }

    fun stopListening() {
      methodChannel.setMethodCallHandler(null)
    }
  }

  companion object {
    private const val TAG = "BetterPlayerPlugin"
    private const val CHANNEL = "better_player_channel"
    private const val EVENTS_CHANNEL = "better_player_channel/videoEvents"
    private const val DATA_SOURCE_PARAMETER = "dataSource"
    private const val KEY_PARAMETER = "key"
    private const val HEADERS_PARAMETER = "headers"
    private const val ASSET_PARAMETER = "asset"
    private const val PACKAGE_PARAMETER = "package"
    private const val METADATA_PARAMETER = "metadata"
    private const val URI_PARAMETER = "uri"
    private const val FORMAT_HINT_PARAMETER = "formatHint"
    private const val TEXTURE_ID_PARAMETER = "textureId"
    private const val LOOPING_PARAMETER = "looping"
    private const val VOLUME_PARAMETER = "volume"
    private const val LOCATION_PARAMETER = "location"
    private const val SPEED_PARAMETER = "speed"
    private const val WIDTH_PARAMETER = "width"
    private const val HEIGHT_PARAMETER = "height"
    private const val BITRATE_PARAMETER = "bitrate"
    private const val NAME_PARAMETER = "name"
    private const val INDEX_PARAMETER = "index"
    private const val LICENSE_URL_PARAMETER = "licenseUrl"
    private const val DRM_HEADERS_PARAMETER = "drmHeaders"
    private const val DRM_CLEARKEY_PARAMETER = "clearKey"
    private const val MIX_WITH_OTHERS_PARAMETER = "mixWithOthers"
    private const val CACHE_KEY_PARAMETER = "cacheKey"
    private const val INIT_METHOD = "init"
    private const val CREATE_METHOD = "create"
    private const val SET_DATA_SOURCE_METHOD = "setDataSource"
    private const val SET_LOOPING_METHOD = "setLooping"
    private const val SET_VOLUME_METHOD = "setVolume"
    private const val PLAY_METHOD = "play"
    private const val PAUSE_METHOD = "pause"
    private const val SEEK_TO_METHOD = "seekTo"
    private const val POSITION_METHOD = "position"
    private const val ABSOLUTE_POSITION_METHOD = "absolutePosition"
    private const val SET_SPEED_METHOD = "setSpeed"
    private const val SET_TRACK_PARAMETERS_METHOD = "setTrackParameters"
    private const val SET_AUDIO_TRACK_METHOD = "setAudioTrack"
    private const val ENABLE_PICTURE_IN_PICTURE_METHOD = "enablePictureInPicture"
    private const val DISABLE_PICTURE_IN_PICTURE_METHOD = "disablePictureInPicture"
    private const val IS_PICTURE_IN_PICTURE_SUPPORTED_METHOD = "isPictureInPictureSupported"
    private const val SET_MIX_WITH_OTHERS_METHOD = "setMixWithOthers"
    private const val DISPOSE_METHOD = "dispose"
  }
}