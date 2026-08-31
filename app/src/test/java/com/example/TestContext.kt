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

    private val mockPrefs = MockSharedPreferences()

    override fun getFilesDir(): java.io.File {
        val dir = java.io.File(System.getProperty("java.io.tmpdir"), "jarvis_test_files")
        dir.mkdirs()
        return dir
    }

    override fun getSharedPreferences(name: String?, mode: Int): android.content.SharedPreferences {
        return mockPrefs
    }
}

class MockSharedPreferences : android.content.SharedPreferences {
    private val map = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = map
    override fun getString(key: String?, defValue: String?): String? = (map[key] as? String) ?: defValue
    override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? = (map[key] as? Set<String>) ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun edit(): android.content.SharedPreferences.Editor = Editor(map)
    override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class Editor(private val map: MutableMap<String, Any?>) : android.content.SharedPreferences.Editor {
        override fun putString(key: String?, value: String?) = apply { map[key ?: ""] = value }
        override fun putStringSet(key: String?, values: Set<String>?) = apply { map[key ?: ""] = values }
        override fun putInt(key: String?, value: Int) = apply { map[key ?: ""] = value }
        override fun putLong(key: String?, value: Long) = apply { map[key ?: ""] = value }
        override fun putFloat(key: String?, value: Float) = apply { map[key ?: ""] = value }
        override fun putBoolean(key: String?, value: Boolean) = apply { map[key ?: ""] = value }
        override fun remove(key: String?) = apply { map.remove(key) }
        override fun clear() = apply { map.clear() }
        override fun commit(): Boolean = true
        override fun apply() {}
    }
}

