package com.jhomlala.better_player

import android.content.Context
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class ChromeCastButtonFactory : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    override fun create(
        context: Context?,
        viewId: Int,
        args: Any?
    ): PlatformView = ChromeCastButton(
        context = context
    )
}