package com.pcrclicker.viewModels

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.ViewModel
import com.pcrclicker.AutoClickService
import com.pcrclicker.FloatingWindowService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ServicesViewModel() : ViewModel() {
    private val _accessibilityEnabled = MutableStateFlow(false)
    val accessibilityEnabled = _accessibilityEnabled.asStateFlow()

    private val _overlayEnabled = MutableStateFlow(false)
    val overlayEnabled = _overlayEnabled.asStateFlow()

    private val _floatingWindowServiceConnected = MutableStateFlow(false)
    val floatingWindowServiceConnected = _floatingWindowServiceConnected.asStateFlow()

    fun checkAccessibilityStatus(context: Context) {
        _accessibilityEnabled.value = AutoClickService.isServiceEnabled(context)
    }

    fun checkOverlayStatus(context: Context) {
        _overlayEnabled.value = FloatingWindowService.isServiceEnabled(context)
    }

    fun startFloatingWindowService(context: Context) {
        if (!_overlayEnabled.value || _floatingWindowServiceConnected.value) {
            return
        }
        val intent = Intent(context, FloatingWindowService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        context.bindService(
            intent,
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    _floatingWindowServiceConnected.value = true
                }
                override fun onServiceDisconnected(name: ComponentName?) {
                    _floatingWindowServiceConnected.value = false
                }
            },
            Context.BIND_AUTO_CREATE
        )
    }
}
