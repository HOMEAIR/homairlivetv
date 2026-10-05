package com.homeair.live.data

data class Channel(
    val number: Int,
    val id: String,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val group: String = "Live",
    val tvgId: String? = null,
    val tvgName: String? = null,
    val userAgent: String? = null,
    val referrer: String? = null
)
