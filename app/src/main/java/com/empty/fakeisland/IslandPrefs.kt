package com.empty.fakeisland

import android.content.Context
import android.content.SharedPreferences

object IslandPrefs {
    private const val FILE = "island_prefs"
    const val KEY_ENABLED = "enabled"
    const val KEY_WIDTH = "width_dp"
    const val KEY_HEIGHT = "height_dp"
    const val KEY_X = "offset_x_dp"
    const val KEY_Y = "offset_y_dp"
    const val KEY_HIDE_LANDSCAPE = "hide_landscape"
    const val DEF_WIDTH = 126f
    const val DEF_HEIGHT = 36f
    const val DEF_X = 0f
    const val DEF_Y = 8f
    const val DEF_HIDE_LANDSCAPE = true

    fun of(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    data class Config(
        val enabled: Boolean,
        val widthDp: Float,
        val heightDp: Float,
        val xDp: Float,
        val yDp: Float,
        val hideInLandscape: Boolean,
    )

    fun read(p: SharedPreferences) = Config(
        enabled = p.getBoolean(KEY_ENABLED, true),
        widthDp = p.getFloat(KEY_WIDTH, DEF_WIDTH),
        heightDp = p.getFloat(KEY_HEIGHT, DEF_HEIGHT),
        xDp = p.getFloat(KEY_X, DEF_X),
        yDp = p.getFloat(KEY_Y, DEF_Y),
        hideInLandscape = p.getBoolean(KEY_HIDE_LANDSCAPE, DEF_HIDE_LANDSCAPE),
    )
}