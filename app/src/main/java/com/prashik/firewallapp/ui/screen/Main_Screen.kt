package com.prashik.firewallapp.ui.screen

import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.prashik.firewallapp.FirewallVpnService
import com.prashik.firewallapp.R
import com.prashik.firewallapp.ui.components.TrafficLog_Item

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Main_Screen(
    modifier: Modifier = Modifier
) {
    var switchState by rememberSaveable { mutableStateOf(false) }
    var showSetting by rememberSaveable { mutableStateOf(false) }
    var showAllLogs by rememberSaveable { mutableStateOf(false) }

    var allTrafficLogs = FirewallVpnService.allTrafficLogs

    val realTrafficLogs = FirewallVpnService.filteredTrafficLogs

    val filteredTrafficLogs by remember(showAllLogs, allTrafficLogs, realTrafficLogs) {
        derivedStateOf {
            if (showAllLogs) allTrafficLogs else realTrafficLogs
        }
    }

    val context = LocalContext.current

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val intent = Intent(context, FirewallVpnService::class.java)
        ContextCompat.startForegroundService(context, intent)
        switchState = true
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
                    fontSize = 22.sp,
                    color = if (switchState) colorResource(R.color.home_header_on_text) else Color.Black
                )
                Row {
                    Switch(
                        checked = switchState,
                        onCheckedChange = {
                            if (!switchState) {
                                val vpnIntent = VpnService.prepare(context)
                                if (vpnIntent != null) {
                                    vpnPermissionLauncher.launch(vpnIntent)
                                } else {
                                    ContextCompat.startForegroundService(
                                        context,
                                        Intent(context, FirewallVpnService::class.java)
                                    )
                                    switchState = true
                                }
                            } else {
                                val stopIntent = Intent(
                                    context,
                                    FirewallVpnService::class.java
                                ).apply { action = "STOP_VPN_SERVICE" }

                                context.startService(stopIntent)
                                switchState = false
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colorResource(R.color.home_header_on_text),
                            checkedTrackColor = colorResource(R.color.home_header_on_text).copy(
                                alpha = 0.3f
                            ),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.Black.copy(alpha = 0.4f),
                            checkedBorderColor = Color.Transparent,
                            uncheckedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                    Column {
                        IconButton(
                            onClick = { showSetting = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (switchState) colorResource(R.color.home_header_on_text) else Color.Black
                            )
                        }
                        DropdownMenu(
                            expanded = showSetting,
                            onDismissRequest = { showSetting = false }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Show all Logs",
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(12.dp))
                                Switch(
                                    checked = showAllLogs,
                                    onCheckedChange = { showAllLogs = !showAllLogs },
                                    modifier = Modifier.scale(0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
        if (switchState) {
            items(items = filteredTrafficLogs.toList().reversed()) { trafficLog ->
                TrafficLog_Item(
                    trafficLogResponse = trafficLog,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
        }
    }
}