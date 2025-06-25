package com.prashik.firewallapp.model.data

data class TrafficLogResponse(
    val appName: String,
    val protocol: String,
    val srcIp: String,
    val dstIp: String,
    val timeStamp: String
)