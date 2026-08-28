package com.example.jarvis.tools.impl.network

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.example.jarvis.domain.model.RiskLevel
import com.example.jarvis.domain.model.ToolParameter
import com.example.jarvis.domain.model.ToolResult
import com.example.jarvis.tools.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface

/** WIFI_STATUS — current Wi-Fi connection info. */
class WifiStatusTool : Tool {
    override val id = "WIFI_STATUS"
    override val name = "Wi-Fi Vəziyyəti"
    override val description = "Wi-Fi bağlantısının vəziyyəti və SSID-i haqqında məlumat verir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network  = cm.activeNetwork
            val caps     = cm.getNetworkCapabilities(network)
            val hasWifi  = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val hasMobile = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val isEnabled = wm.isWifiEnabled

            val msg = when {
                hasWifi   -> "Wi-Fi bağlıdır. (Şəbəkəyə qoşulub)"
                hasMobile -> "Mobil məlumat istifadə olunur. Wi-Fi bağlı deyil."
                isEnabled -> "Wi-Fi açıqdır, lakin şəbəkəyə qoşulmayıb."
                else      -> "Wi-Fi söndürülüb."
            }
            ToolResult.success(id, msg, mapOf("wifiEnabled" to isEnabled, "connectedToWifi" to hasWifi))
        } catch (e: Exception) {
            ToolResult.failed(id, "Wi-Fi vəziyyəti alına bilmədi: ${e.message}")
        }
    }
}

/** WIFI_SETTINGS — open Wi-Fi settings (Android 10+ cannot toggle programmatically). */
class WifiSettingsTool : Tool {
    override val id = "WIFI_SETTINGS"
    override val name = "Wi-Fi Parametrləri"
    override val description = "Wi-Fi parametrləri səhifəsini açır. Android 10+ cihazlarda Wi-Fi proqramlı şəkildə dəyişdirilə bilməz."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            val note = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                " (Android 10+ cihazlarda Wi-Fi yalnız istifadəçi tərəfindən dəyişdirilə bilər.)" else ""
            ToolResult.success(id, "Wi-Fi parametrləri açıldı.$note")
        } catch (e: Exception) {
            ToolResult.failed(id, "Wi-Fi parametrləri açıla bilmədi: ${e.message}")
        }
    }
}

/** NETWORK_STATUS — overall connectivity status. */
class NetworkStatusTool : Tool {
    override val id = "NETWORK_STATUS"
    override val name = "Şəbəkə Vəziyyəti"
    override val description = "İnternet bağlantısının ümumi vəziyyətini yoxlayır."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val cm   = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val net  = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(net)
            val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                              caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            val transportType = when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)     == true -> "Wi-Fi"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobil məlumat"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else                                                                -> "Bilinmir"
            }

            val msg = if (hasInternet) "İnternet bağlantısı var. Bağlantı növü: $transportType."
                      else             "İnternet bağlantısı yoxdur."
            ToolResult.success(id, msg, mapOf("connected" to hasInternet, "transport" to transportType))
        } catch (e: Exception) {
            ToolResult.failed(id, "Şəbəkə vəziyyəti alına bilmədi: ${e.message}")
        }
    }
}

/** IP_INFO — device's local IP address. */
class IpInfoTool : Tool {
    override val id = "IP_INFO"
    override val name = "IP Ünvanı"
    override val description = "Cihazın yerli IP ünvanını göstərir."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val ipList = NetworkInterface.getNetworkInterfaces()?.toList()
                ?.flatMap { it.inetAddresses.toList() }
                ?.filterIsInstance<Inet4Address>()
                ?.filter { !it.isLoopbackAddress }
                ?.map { it.hostAddress }
                ?: emptyList()

            if (ipList.isEmpty()) {
                ToolResult.success(id, "Aktiv IP ünvanı tapılmadı. Şəbəkəyə qoşulub?")
            } else {
                val primary = ipList.first()
                ToolResult.success(id, "Cihazın IP ünvanı: $primary.", mapOf("ips" to ipList, "primary" to primary))
            }
        } catch (e: Exception) {
            ToolResult.failed(id, "IP ünvanı alına bilmədi: ${e.message}")
        }
    }
}

/** BLUETOOTH_STATUS — is Bluetooth enabled and are devices connected? */
class BluetoothStatusTool : Tool {
    override val id = "BLUETOOTH_STATUS"
    override val name = "Bluetooth Vəziyyəti"
    override val description = "Bluetooth-un vəziyyəti və qoşulmuş cihazlar haqqında məlumat."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions: List<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            listOf(android.Manifest.permission.BLUETOOTH_CONNECT) else emptyList()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    @Suppress("MissingPermission")
    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            val bm = android.bluetooth.BluetoothManager::class.java
                .let { context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager }
            val adapter = bm?.adapter
            if (adapter == null) return ToolResult.unsupported(id, "Bu cihaz Bluetooth dəstəkləmir.")
            val enabled = adapter.isEnabled
            if (!enabled) return ToolResult.success(id, "Bluetooth söndürülüb.", mapOf("enabled" to false))

            val bonded = try {
                adapter.bondedDevices?.map { it.name }?.take(5) ?: emptyList()
            } catch (_: SecurityException) { emptyList<String>() }

            val msg = if (bonded.isEmpty()) "Bluetooth açıqdır. Heç bir cihaz cütləşməyib."
                      else "Bluetooth açıqdır. Cütləşmiş cihazlar: ${bonded.joinToString(", ")}."
            ToolResult.success(id, msg, mapOf("enabled" to true, "bondedDevices" to bonded))
        } catch (e: Exception) {
            ToolResult.failed(id, "Bluetooth vəziyyəti alına bilmədi: ${e.message}")
        }
    }
}

/** BLUETOOTH_SETTINGS — open Bluetooth settings page. */
class BluetoothSettingsTool : Tool {
    override val id = "BLUETOOTH_SETTINGS"
    override val name = "Bluetooth Parametrləri"
    override val description = "Bluetooth parametrləri səhifəsini açır."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            ToolResult.success(id, "Bluetooth parametrləri açıldı.")
        } catch (e: Exception) {
            ToolResult.failed(id, "Bluetooth parametrləri açıla bilmədi: ${e.message}")
        }
    }
}

/** MOBILE_NETWORK_SETTINGS — open mobile network settings. */
class MobileNetworkSettingsTool : Tool {
    override val id = "MOBILE_NETWORK_SETTINGS"
    override val name = "Mobil Şəbəkə Parametrləri"
    override val description = "Mobil şəbəkə və məlumat parametrlərini açır."
    override val parameters = emptyList<ToolParameter>()
    override val requiredPermissions = emptyList<String>()
    override val riskLevel = RiskLevel.LOW

    override suspend fun canExecute(context: Context, params: Map<String, String>) = true

    override suspend fun execute(context: Context, params: Map<String, String>): ToolResult {
        return try {
            context.startActivity(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            ToolResult.success(id, "Mobil şəbəkə parametrləri açıldı.")
        } catch (e: Exception) {
            // Fallback
            try {
                context.startActivity(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                ToolResult.success(id, "Mobil şəbəkə parametrləri açıldı.")
            } catch (e2: Exception) {
                ToolResult.failed(id, "Mobil şəbəkə parametrləri açıla bilmədi: ${e2.message}")
            }
        }
    }
}
