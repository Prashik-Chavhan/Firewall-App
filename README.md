# Andorid Firewall App With VPN Logging

🚀 **Firewall App** is a lightweight Android VPN-based firewall logger built with **Kotlin + Jetpack Compose**. It loggs **which app is accessing which IP address** in real-time, displaying protocol, ports and timestamps while providing user control to filter logs.

---

## ✨ Features

✅ Logs **real network traffic** using a local VPN service.

✅ Displays:
- App Name (if UID is available)
- Destination IP and Port
- Source IP and Port
- Protocol (TCP/UDP)
- Timestamp per packet

✅ User controls:
- Toggle **Show All Packets** on/off
- Toggle **Show Unknown Apps (UID -1)** on/off
- Live updating UI using **Jetpack Compose**

✅ Supports **IPv4 and IPv6** parsing

✅ Clean, minimal UI

---

## ⚙️ How It Works
- Uses a **fake VPN interface** to capture packets locally.
- Parses packets headers to extract IP, port and protocol.
- Maps UID to app names using 'PackageManager'.
- Displays logs live in Compose.
- Allows toggling unknown UID logs.

## ⚠️ Limitations

⚠️ App name may show **Unknown App (UID -1)** for:
- System/kernel traffic
- IPv6 system-level packets
- When UID mapping is restricted by the OS
