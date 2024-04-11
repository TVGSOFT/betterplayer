package com.jhomlala.better_player

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import io.flutter.plugin.platform.PlatformView

class ChromeCastButton(
    context: Context?
) : PlatformView{
    private val chromeCastButton = MediaRouteButton(ContextThemeWrapper(context, R.style.Theme_AppCompat_NoActionBar))

    init {
        CastButtonFactory.setUpMediaRouteButton(context as Context, chromeCastButton)
    }

    override fun getView() = chromeCastButton

    override fun dispose() {
    }

}