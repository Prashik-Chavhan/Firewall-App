package com.prashik.firewallapp.ui.utils

import com.prashik.firewallapp.model.data.TrafficLogResponse

object Utils {
    val dummyTrafficLogs = listOf(
        TrafficLogResponse(
            appName = "Chrome",
            protocol = "TCP",
            srcIp = "10.1.10.1 (42640)",
            dstIp = "142.250.198.3 (443)",
            timeStamp = "2023-09-10 10:51:12"
        ),
        TrafficLogResponse(
            appName = "Youtube",
            protocol = "TCP",
            srcIp = "10.1.10.1 (42640)",
            dstIp = "142.250.198.3 (443)",
            timeStamp = "2023-09-10 10:51:12"
        ),
        TrafficLogResponse(
            appName = "WhatsApp",
            protocol = "TCP",
            srcIp = "10.1.10.1 (42640)",
            dstIp = "142.250.198.3 (443)",
            timeStamp = "2023-09-10 10:51:12"
        )
    )
}