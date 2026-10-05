package com.homeair.live.network

import com.homeair.live.BuildConfig

object WorkerConfig {
    const val APP_SECRET_HEADER = "X-App-Secret"

    val proxyUrl: String
        get() = BuildConfig.HOME_AIR_PROXY_URL.trimEnd('/') + "/"

    val appSecret: String
        get() = BuildConfig.HOME_AIR_APP_SECRET
}
