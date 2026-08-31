package com.example.jarvis.tools

import com.example.jarvis.tools.impl.CreateReminderTool
import com.example.jarvis.tools.impl.GetBatteryTool
import com.example.jarvis.tools.impl.GetRamTool
import com.example.jarvis.tools.impl.GetStorageTool
import com.example.jarvis.tools.impl.LockScreenTool
import com.example.jarvis.tools.impl.OpenAppTool
import com.example.jarvis.tools.impl.OpenSettingsTool
import com.example.jarvis.tools.impl.ReadNotificationsTool
import com.example.jarvis.tools.impl.SetVolumeTool
import com.example.jarvis.tools.impl.TakePhotoTool
import com.example.jarvis.tools.impl.TorchTool
import com.example.jarvis.tools.impl.accessibility.AccessibilityGoHomeTool
import com.example.jarvis.tools.impl.accessibility.ClickUiElementTool
import com.example.jarvis.tools.impl.accessibility.GoBackTool
import com.example.jarvis.tools.impl.accessibility.InteractWithUiTool
import com.example.jarvis.tools.impl.accessibility.ReadVisibleTextTool
import com.example.jarvis.tools.impl.accessibility.ScrollTool
import com.example.jarvis.tools.impl.alarm.CreateAlarmTool
import com.example.jarvis.tools.impl.alarm.DeleteAlarmTool
import com.example.jarvis.tools.impl.alarm.ListAlarmsTool
import com.example.jarvis.tools.impl.apps.AppInfoTool
import com.example.jarvis.tools.impl.apps.ListAppsTool
import com.example.jarvis.tools.impl.apps.OpenAppSettingsTool
import com.example.jarvis.tools.impl.apps.OpenPlayStoreTool
import com.example.jarvis.tools.impl.apps.RequestAppPermissionTool
import com.example.jarvis.tools.impl.audio.GetVolumeTool
import com.example.jarvis.tools.impl.audio.MediaNextTool
import com.example.jarvis.tools.impl.audio.MediaPauseTool
import com.example.jarvis.tools.impl.audio.MediaPlayTool
import com.example.jarvis.tools.impl.audio.MediaPreviousTool
import com.example.jarvis.tools.impl.audio.MuteTool
import com.example.jarvis.tools.impl.audio.UnmuteTool
import com.example.jarvis.tools.impl.battery.BatterySaverStatusTool
import com.example.jarvis.tools.impl.battery.BatteryStatusTool
import com.example.jarvis.tools.impl.battery.BatteryTemperatureTool
import com.example.jarvis.tools.impl.battery.ChargingStatusTool
import com.example.jarvis.tools.impl.battery.OpenBatterySettingsTool
import com.example.jarvis.tools.impl.browser.OpenBrowserTool
import com.example.jarvis.tools.impl.browser.OpenUrlTool
import com.example.jarvis.tools.impl.browser.WebSearchTool
import com.example.jarvis.tools.impl.media.AppSearchTool
import com.example.jarvis.tools.impl.media.MediaSearchPlayTool
import com.example.jarvis.tools.impl.calendar.CreateEventTool
import com.example.jarvis.tools.impl.calendar.DeleteEventTool
import com.example.jarvis.tools.impl.calendar.ListEventsTool
import com.example.jarvis.tools.impl.call.CallContactTool
import com.example.jarvis.tools.impl.call.DialNumberTool
import com.example.jarvis.tools.impl.call.OpenCallLogTool
import com.example.jarvis.tools.impl.camera.OpenCameraTool
import com.example.jarvis.tools.impl.camera.RecordVideoTool
import com.example.jarvis.tools.impl.contacts.CreateContactTool
import com.example.jarvis.tools.impl.contacts.OpenContactsTool
import com.example.jarvis.tools.impl.contacts.SearchContactTool
import com.example.jarvis.tools.impl.files.CopyFileTool
import com.example.jarvis.tools.impl.files.CreateFolderTool
import com.example.jarvis.tools.impl.files.DeleteFileTool
import com.example.jarvis.tools.impl.files.MoveFileTool
import com.example.jarvis.tools.impl.files.OpenFileTool
import com.example.jarvis.tools.impl.files.RenameFileTool
import com.example.jarvis.tools.impl.files.SearchFilesTool
import com.example.jarvis.tools.impl.files.ShareFileTool
import com.example.jarvis.tools.impl.files.StorageInfoTool
import com.example.jarvis.tools.impl.location.GetLocationTool
import com.example.jarvis.tools.impl.location.OpenLocationSettingsTool
import com.example.jarvis.tools.impl.location.OpenMapTool
import com.example.jarvis.tools.impl.network.BluetoothSettingsTool
import com.example.jarvis.tools.impl.network.BluetoothStatusTool
import com.example.jarvis.tools.impl.network.IpInfoTool
import com.example.jarvis.tools.impl.network.MobileNetworkSettingsTool
import com.example.jarvis.tools.impl.network.NetworkStatusTool
import com.example.jarvis.tools.impl.network.WifiSettingsTool
import com.example.jarvis.tools.impl.network.WifiStatusTool
import com.example.jarvis.tools.impl.notifications.ListNotificationsTool
import com.example.jarvis.tools.impl.notifications.NotificationStatusTool
import com.example.jarvis.tools.impl.notifications.RemoveNotificationTool
import com.example.jarvis.tools.impl.performance.CpuStatusTool
import com.example.jarvis.tools.impl.performance.DeviceInfoTool
import com.example.jarvis.tools.impl.reminder.DeleteReminderTool
import com.example.jarvis.tools.impl.reminder.ListRemindersTool
import com.example.jarvis.tools.impl.sms.ComposeSMSTool
import com.example.jarvis.tools.impl.sms.OpenMessagesTool
import com.example.jarvis.tools.impl.system.OpenHomeTool
import com.example.jarvis.tools.impl.system.OpenNotificationsTool
import com.example.jarvis.tools.impl.system.OpenQuickSettingsTool
import com.example.jarvis.tools.impl.system.OpenRecentsTool
import com.example.jarvis.tools.impl.system.ScreenControlTool
import com.example.jarvis.tools.impl.messaging.SendTelegramMessageTool
import com.example.jarvis.tools.impl.messaging.SendWhatsAppMessageTool
import com.example.jarvis.tools.impl.timer.SetTimerTool
import com.example.jarvis.tools.impl.vision.AnalyzePhotoTool
import com.example.jarvis.tools.impl.voice.SpeakTool
import com.example.jarvis.tools.impl.voice.StartListeningTool
import com.example.jarvis.tools.impl.voice.StopListeningTool
import com.example.jarvis.tools.impl.weather.GetWeatherTool
import java.util.concurrent.ConcurrentHashMap

class ToolRegistry {

    private val toolsMap = ConcurrentHashMap<String, Tool>()

    init {
        registerDefaultTools()
    }

    private fun registerDefaultTools() {
        // --- PHASE 2 CORE TOOLS (PRESERVED) ---
        register(GetBatteryTool())
        register(GetRamTool())
        register(GetStorageTool())
        register(SetVolumeTool())
        register(TorchTool())
        register(OpenSettingsTool())
        register(OpenAppTool())
        register(LockScreenTool())
        register(TakePhotoTool())
        register(CreateReminderTool())
        register(ReadNotificationsTool())

        // --- SYSTEM ---
        register(ScreenControlTool())
        register(OpenHomeTool())
        register(OpenRecentsTool())
        register(OpenNotificationsTool())
        register(OpenQuickSettingsTool())

        // --- APPS ---
        register(ListAppsTool())
        register(AppInfoTool())
        register(OpenAppSettingsTool())
        register(RequestAppPermissionTool())
        register(OpenPlayStoreTool())

        // --- FILES ---
        register(SearchFilesTool())
        register(OpenFileTool())
        register(ShareFileTool())
        register(CopyFileTool())
        register(MoveFileTool())
        register(RenameFileTool())
        register(DeleteFileTool())
        register(CreateFolderTool())
        register(StorageInfoTool())

        // --- BATTERY ---
        register(BatteryStatusTool())
        register(BatteryTemperatureTool())
        register(ChargingStatusTool())
        register(BatterySaverStatusTool())
        register(OpenBatterySettingsTool())

        // --- PERFORMANCE ---
        register(CpuStatusTool())
        register(DeviceInfoTool())

        // --- NETWORK ---
        register(WifiStatusTool())
        register(WifiSettingsTool())
        register(NetworkStatusTool())
        register(IpInfoTool())
        register(BluetoothStatusTool())
        register(BluetoothSettingsTool())
        register(MobileNetworkSettingsTool())

        // --- AUDIO ---
        register(GetVolumeTool())
        register(MuteTool())
        register(UnmuteTool())
        register(MediaPlayTool())
        register(MediaPauseTool())
        register(MediaNextTool())
        register(MediaPreviousTool())

        // --- NOTIFICATIONS ---
        register(ListNotificationsTool())
        register(RemoveNotificationTool())
        register(NotificationStatusTool())

        // --- CAMERA ---
        register(OpenCameraTool())
        register(RecordVideoTool())

        // --- CONTACTS ---
        register(SearchContactTool())
        register(CreateContactTool())
        register(OpenContactsTool())

        // --- CALL ---
        register(DialNumberTool())
        register(CallContactTool())
        register(OpenCallLogTool())

        // --- SMS ---
        register(OpenMessagesTool())
        register(ComposeSMSTool())

        // --- LOCATION ---
        register(GetLocationTool())
        register(OpenLocationSettingsTool())
        register(OpenMapTool())

        // --- VOICE ---
        register(SpeakTool())
        register(StartListeningTool())
        register(StopListeningTool())

        // --- ALARM ---
        register(CreateAlarmTool())
        register(ListAlarmsTool())
        register(DeleteAlarmTool())

        // --- REMINDER ---
        register(DeleteReminderTool())
        register(ListRemindersTool())

        // --- CALENDAR ---
        register(CreateEventTool())
        register(ListEventsTool())
        register(DeleteEventTool())

        // --- BROWSER ---
        register(OpenUrlTool())
        register(WebSearchTool())
        register(OpenBrowserTool())

        // --- MEDIA & SEARCH IN APP ---
        register(MediaSearchPlayTool())
        register(AppSearchTool())

        // --- ACCESSIBILITY ---
        register(ClickUiElementTool())
        register(ScrollTool())
        register(ReadVisibleTextTool())
        register(GoBackTool())
        register(AccessibilityGoHomeTool())
        register(InteractWithUiTool())

        // --- WEATHER ---
        register(GetWeatherTool())

        // --- TIMER & CLOCK ---
        register(SetTimerTool())

        // --- MESSAGING (WhatsApp & Telegram) ---
        register(SendWhatsAppMessageTool())
        register(SendTelegramMessageTool())

        // --- VISION AI ---
        register(AnalyzePhotoTool())
    }

    fun register(tool: Tool) {
        toolsMap[tool.id.uppercase()] = tool
    }

    fun unregister(toolId: String) {
        toolsMap.remove(toolId.uppercase())
    }

    fun getTool(toolId: String): Tool? {
        return toolsMap[toolId.uppercase()]
    }

    fun getAllTools(): List<Tool> {
        return toolsMap.values.toList()
    }

    fun hasTool(toolId: String): Boolean {
        return toolsMap.containsKey(toolId.uppercase())
    }
}
