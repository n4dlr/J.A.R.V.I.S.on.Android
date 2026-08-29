package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager

class TestContext(base: Context? = null) : ContextWrapper(base) {
    override fun getApplicationContext(): Context = this
    override fun getPackageName(): String = "com.example.jarvis"

    override fun checkSelfPermission(permission: String): Int {
        return PackageManager.PERMISSION_DENIED
    }

    override fun checkPermission(permission: String, pid: Int, uid: Int): Int {
        return PackageManager.PERMISSION_DENIED
    }

    override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?): Intent? {
        val intent = Intent(Intent.ACTION_BATTERY_CHANGED)
        intent.putExtra(BatteryManager.EXTRA_LEVEL, 85)
        intent.putExtra(BatteryManager.EXTRA_SCALE, 100)
        intent.putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING)
        intent.putExtra(BatteryManager.EXTRA_TEMPERATURE, 310)
        return intent
    }

    override fun getSystemService(name: String): Any? {
        return null
    }
}
