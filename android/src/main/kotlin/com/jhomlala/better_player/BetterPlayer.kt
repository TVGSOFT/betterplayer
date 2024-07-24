package com.jhomlala.better_player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.view.Surface
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.android.gms.cast.framework.CastContext
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import io.flutter.view.TextureRegistry.SurfaceTextureEntry
import kotlin.math.max
import kotlin.math.min

@OptIn(UnstableApi::class)
internal class BetterPlayer(
  private val context: Context,
  private val videoPlayerEvents: VideoPlayerCallbacks,
  private val textureEntry: SurfaceTextureEntry,
  isCastEnabled: Boolean,
) : SessionAvailabilityListener {
  private var controllerFuture: ListenableFuture<MediaController>? = null
  private var mediaController: MediaController? = null
  private var currentPlayer: Player? = null
  private var castPlayer: CastPlayer? = null
  private val surface: Surface = Surface(textureEntry.surfaceTexture())
  private var mediaItem: MediaItem? = null

  private var playerEventListener: PlayerEventListener? = null

  init {
    if (isCastEnabled) {
      CastContext.getSharedInstance(context, MoreExecutors.directExecutor())
        .addOnCompleteListener { task ->
          initializeController(task.result)
        }
    } else {
      initializeController()
    }
  }

  private fun initializeController(castContext: CastContext? = null) {
    val controllerFuture =
      MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, VideoPlaybackService::class.java))
      )
        .buildAsync()
    controllerFuture.addListener({
      if (controllerFuture.isDone && !controllerFuture.isCancelled) {
        if (castContext != null) {
          val castPlayer = CastPlayer(castContext)
          castPlayer.setSessionAvailabilityListener(this)

          this.castPlayer = castPlayer

          if (castPlayer.isCastSessionAvailable) {
            setCurrentPlayer(castPlayer)

            videoPlayerEvents.onCastingStart()

            return@addListener
          }
        }

        val mediaController = controllerFuture.get()
        setCurrentPlayer(mediaController)

        this.mediaController = mediaController
      }
    }, MoreExecutors.directExecutor())

    this.controllerFuture = controllerFuture
  }

  private fun releaseController() {
    controllerFuture?.let { MediaController.releaseFuture(it) }
  }

  private fun setCurrentPlayer(player: Player) {

    if (currentPlayer == player) {
      return
    }

    preparePlayer(player)

    this.currentPlayer = player
  }

  private fun preparePlayer(player: Player) {
    val mediaItem = mediaItem ?: return

    var playbackPositionMs = C.TIME_UNSET
    var playWhenReady = false

    val previousPlayer = this.currentPlayer

    previousPlayer?.let {
      // Save state from the previous player.
      val playbackState = it.playbackState
      if (playbackState != Player.STATE_ENDED) {
        playbackPositionMs = it.currentPosition
        playWhenReady = it.playWhenReady
      }

      playerEventListener?.let { pIt ->
        it.removeListener(pIt)
      }

      it.clearVideoSurface()
      it.clearMediaItems()
      it.stop()
    }

    player.setVideoSurface(surface)

    val playerEventListener = PlayerEventListener(player, videoPlayerEvents)
    player.addListener(playerEventListener)
    player.setMediaItem(mediaItem, playbackPositionMs)
    player.playWhenReady = playWhenReady
    player.prepare()

    this.playerEventListener = playerEventListener
  }

  private fun setAudioAttributes(player: Player, isMixMode: Boolean) {
    player.setAudioAttributes(
      AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
      !isMixMode
    )
  }

  fun setupDataSource(asset: VideoAsset) {
    mediaItem = asset.mediaItem

    currentPlayer?.let { preparePlayer(it) }
  }

  fun sendBufferingUpdate() {
    val player = currentPlayer ?: return

    videoPlayerEvents.onBufferingUpdate(player.bufferedPosition)
  }

  fun onPictureInPictureStatusChanged(inPip: Boolean) {
    if (inPip) {
      videoPlayerEvents.onPipStart()
    } else {
      videoPlayerEvents.onPipEnd()
    }
  }

  fun play() {
    currentPlayer?.playWhenReady = true
  }

  fun pause() {
    currentPlayer?.playWhenReady = false
  }

  fun setLooping(value: Boolean) {
    currentPlayer?.repeatMode = if (value) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
  }

  fun setVolume(value: Double) {
    val bracketedValue = max(0.0, min(1.0, value))
      .toFloat()
    currentPlayer?.volume = bracketedValue
  }

  fun setSpeed(value: Double) {
    val bracketedValue = value.toFloat()
    val playbackParameters = PlaybackParameters(bracketedValue)
    currentPlayer?.playbackParameters = playbackParameters
  }

  fun setTrackParameters(width: Int, height: Int, bitrate: Int) {
    val player = currentPlayer ?: return

    val parametersBuilder = player.trackSelectionParameters.buildUpon()
    if (width != 0 && height != 0) {
      parametersBuilder.setMaxVideoSize(width, height)
    }
    if (bitrate != 0) {
      parametersBuilder.setMaxVideoBitrate(bitrate)
    }
    if (width == 0 && height == 0 && bitrate == 0) {
      parametersBuilder.clearVideoSizeConstraints()
      parametersBuilder.setMaxVideoBitrate(Int.MAX_VALUE)
    }

    player.trackSelectionParameters = parametersBuilder.build()
  }

  fun seekTo(location: Int) {
    currentPlayer?.seekTo(location.toLong())
  }

  val position: Long
    get() = currentPlayer?.currentPosition ?: 0L

  val absolutePosition: Long
    get() {
      val timeline = currentPlayer?.currentTimeline
      timeline?.let {
        if (!timeline.isEmpty) {
          val windowStartTimeMs =
            timeline.getWindow(0, Timeline.Window()).windowStartTimeMs
          val pos = currentPlayer?.currentPosition ?: 0L
          return windowStartTimeMs + pos
        }
      }
      return currentPlayer?.currentPosition ?: 0L
    }

  fun setAudioTrack(name: String, index: Int) {
    try {
      val currentTracks = currentPlayer?.currentTracks ?: return

      for (groupIndex in 0 until currentTracks.groups.size) {
        if (currentTracks.groups[groupIndex].type != C.TRACK_TYPE_AUDIO) {
          continue
        }

        val tracksGroup = currentTracks.groups[groupIndex]
        var hasElementWithoutLabel = false
        var hasStrangeAudioTrack = false
        for (trackIndex in 0 until tracksGroup.length) {
          val format = tracksGroup.getTrackFormat(trackIndex)
          if (format.label == null) {
            hasElementWithoutLabel = true
          }
          if (format.id != null && format.id == "1/15") {
            hasStrangeAudioTrack = true
          }
        }
        for (trackIndex in 0 until tracksGroup.length) {
          val label = tracksGroup.getTrackFormat(trackIndex).label
          if (name == label && index == trackIndex) {
            setAudioTrack(groupIndex, trackIndex)
            return
          }

          ///Fallback option
          if (!hasStrangeAudioTrack && hasElementWithoutLabel && index == trackIndex) {
            setAudioTrack(groupIndex, trackIndex)
            return
          }
          ///Fallback option
          if (hasStrangeAudioTrack && name == label) {
            setAudioTrack(groupIndex, trackIndex)
            return
          }
        }
      }
    } catch (exception: Exception) {
      Log.e(TAG, "setAudioTrack failed$exception")
    }
  }

  private fun setAudioTrack(groupIndex: Int, trackIndex: Int) {
    val player = currentPlayer ?: return

    player.trackSelectionParameters =
      player.trackSelectionParameters
        .buildUpon()
        .setOverrideForType(
          TrackSelectionOverride(
            player.currentTracks.groups[groupIndex].mediaTrackGroup,
            trackIndex
          )
        )
        .build()
  }

  fun setMixWithOthers(mixWithOthers: Boolean) {
    currentPlayer?.let { setAudioAttributes(it, mixWithOthers) }
  }

  fun dispose() {
    castPlayer?.let {
      it.setSessionAvailabilityListener(null)
      it.clearVideoSurface()
      it.release()
    }

    currentPlayer?.let {
      it.clearVideoSurface()
      it.release()
    }

    textureEntry.release()
    surface.release()

    releaseController()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val that = other as BetterPlayer
    if (if (currentPlayer != null) currentPlayer != that.currentPlayer else that.currentPlayer != null) return false
    return surface == that.surface
  }

  override fun hashCode(): Int {
    var result = currentPlayer?.hashCode() ?: 0
    result = 31 * result + surface.hashCode()
    return result
  }

  override fun onCastSessionAvailable() {
    val castPlayer = castPlayer ?: return

    setCurrentPlayer(castPlayer)

    videoPlayerEvents.onCastingStart()
  }

  override fun onCastSessionUnavailable() {
    mediaController?.let { setCurrentPlayer(it) }

    videoPlayerEvents.onCastingEnd()
  }

  companion object {
    private const val TAG = "BetterPlayer"
  }
}