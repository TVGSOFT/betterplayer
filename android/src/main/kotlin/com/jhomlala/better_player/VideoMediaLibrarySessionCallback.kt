/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jhomlala.better_player

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/** A [MediaLibraryService.MediaLibrarySession.Callback] implementation. */
open class VideoMediaLibrarySessionCallback(context: Context) :
  MediaLibraryService.MediaLibrarySession.Callback {

  @SuppressLint("PrivateResource")
  private val customLayoutCommandButtons: List<CommandButton> =
    listOf(
      CommandButton.Builder()
        .setDisplayName(context.getString(R.string.exo_controls_shuffle_on_description))
        .setSessionCommand(
          SessionCommand(
            CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_ON,
            Bundle.EMPTY
          )
        )
        .setIconResId(R.drawable.exo_icon_shuffle_off)
        .build(),
      CommandButton.Builder()
        .setDisplayName(context.getString(R.string.exo_controls_shuffle_off_description))
        .setSessionCommand(
          SessionCommand(
            CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_OFF,
            Bundle.EMPTY
          )
        )
        .setIconResId(R.drawable.exo_icon_shuffle_on)
        .build()
    )

  @OptIn(UnstableApi::class) // MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
  val mediaNotificationSessionCommands =
    MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
      .also { builder ->
        // Put all custom session commands in the list that may be used by the notification.
        customLayoutCommandButtons.forEach { commandButton ->
          commandButton.sessionCommand?.let { builder.add(it) }
        }
      }
      .build()

  // ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
  // ConnectionResult.AcceptedResultBuilder
  @OptIn(UnstableApi::class)
  override fun onConnect(
    session: MediaSession,
    controller: MediaSession.ControllerInfo
  ): MediaSession.ConnectionResult {
    if (
      session.isMediaNotificationController(controller) ||
      session.isAutomotiveController(controller) ||
      session.isAutoCompanionController(controller)
    ) {
      // Select the button to display.
      val customLayout =
        customLayoutCommandButtons[if (session.player.shuffleModeEnabled) 1 else 0]
      return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
        .setAvailableSessionCommands(mediaNotificationSessionCommands)
        .setCustomLayout(ImmutableList.of(customLayout))
        .build()
    }
    // Default commands without custom layout for common controllers.
    return MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
  }

  @OptIn(UnstableApi::class) // MediaSession.isMediaNotificationController
  override fun onCustomCommand(
    session: MediaSession,
    controller: MediaSession.ControllerInfo,
    customCommand: SessionCommand,
    args: Bundle
  ): ListenableFuture<SessionResult> {
    if (CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_ON == customCommand.customAction) {
      // Enable shuffling.
      session.player.shuffleModeEnabled = true
      // Change the custom layout to contain the `Disable shuffling` command.
      session.setCustomLayout(
        session.mediaNotificationControllerInfo!!,
        ImmutableList.of(customLayoutCommandButtons[1])
      )
      return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    if (CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_OFF == customCommand.customAction) {
      // Disable shuffling.
      session.player.shuffleModeEnabled = false
      // Change the custom layout to contain the `Enable shuffling` command.
      session.setCustomLayout(
        session.mediaNotificationControllerInfo!!,
        ImmutableList.of(customLayoutCommandButtons[0])
      )
      return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
  }

  companion object {
    private const val CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_ON =
      "io.flutter.plugins.videoplayer.SHUFFLE_ON"
    private const val CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_OFF =
      "io.flutter.plugins.videoplayer.SHUFFLE_OFF"
  }
}
