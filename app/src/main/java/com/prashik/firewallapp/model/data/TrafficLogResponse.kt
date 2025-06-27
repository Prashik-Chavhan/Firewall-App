package com.prashik.firewallapp.model.data

data class TrafficLogResponse(
    val appName: String,
    val protocol: String,
    val srcIp: String,
    val srcPort: Int,
    val dstIp: String,
    val dstPort: Int,
    val timeStamp: String
)