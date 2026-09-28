package com.fahim.bingonumbercaller.platform

import android.content.Context

/** Application context for platform code that has no Compose/Activity context (services, servers). */
object AndroidAppContext {
    @Volatile
    var context: Context? = null
        private set

    fun init(context: Context) {
        this.context = context.applicationContext
    }
}
