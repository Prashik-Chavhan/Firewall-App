package com.prashik.firewallapp.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prashik.firewallapp.R
import com.prashik.firewallapp.ui.components.Custom_Bold_Text
import com.prashik.firewallapp.ui.components.TrafficLog_Item
import com.prashik.firewallapp.ui.components.VpnKeyAsTextIcon
import com.prashik.firewallapp.ui.utils.Utils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Main_Screen(
    modifier: Modifier = Modifier
) {
    var switchState by rememberSaveable { mutableStateOf(false) }

    var showDialog by rememberSaveable { mutableStateOf(false) }

    val dummyTrafficLogs = Utils.dummyTrafficLogs

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Custom_Bold_Text(text = "Connection request")
            },
            text = {
                Column {
                    Text(
                        text = "Firewall App wants to set up a VPN connection that allows it to monitor" +
                                " network traffic. Only accept if you trust the source.",
                        fontSize = 18.sp,
                        color = Color.DarkGray
                    )
                    Spacer(Modifier.height(12.dp))
                    VpnKeyAsTextIcon()
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        switchState = true
                        showDialog = false
                    }
                ) {
                    Text(
                        text = "Ok"
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text(
                        text = "Cancel"
                    )
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
    ) {
        stickyHeader {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = colorResource(R.color.home_header_container))
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Firewall App",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (switchState) colorResource(R.color.home_header_on_text) else Color.Black
                )
                Switch(
                    checked = switchState,
                    onCheckedChange = {
                        if (switchState) {
                            switchState = false
                        } else {
                            showDialog = true
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colorResource(R.color.home_header_on_text),
                        checkedTrackColor = colorResource(R.color.home_header_on_text).copy(alpha = 0.3f),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Black.copy(alpha = 0.1f),
                        checkedBorderColor = Color.Transparent,
                        uncheckedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }
        }
        if (switchState) {
            items(items = dummyTrafficLogs) { trafficLog ->
                TrafficLog_Item(
                    trafficLogResponse = trafficLog,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
        }
    }
}