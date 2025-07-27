package com.pcrclicker

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.SparseArray
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.util.forEach
import androidx.core.util.isNotEmpty
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.pcrclicker.ui.FloatingWindow
import com.pcrclicker.ui.ModalWindow

class FloatingWindowService() : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner {
    companion object {
        private var instance: FloatingWindowService? = null
        private val floatingViews = SparseArray<View>()
        var isPaused: Boolean = false
            private set

        const val MODAL_ID = 0
        const val SETTINGS_ID = 10
        const val PAUSE_ID = 100
        const val TOOL_ID = 1000
        const val TIME_ID = 9999

        fun getInstance(): FloatingWindowService? = instance

        fun isEnabledTool() = floatingViews.get(TOOL_ID) != null

        @SuppressLint("ObsoleteSdkInt")
        fun isServiceEnabled(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
    }

    private lateinit var windowManager: WindowManager
    private lateinit var savedStateRegistryController: SavedStateRegistryController
    private val binder = Binder()

    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        savedStateRegistryController = SavedStateRegistryController.create(this)
        savedStateRegistryController.performRestore(null)
        startForegroundService()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingViews.isNotEmpty()) {
            floatingViews.forEach { id, floatingView ->
                windowManager.removeView(floatingView)
            }
            floatingViews.clear()
        }
        viewModelStore.clear()
        instance = null
    }

    fun pauseGame(pauseActionCount: Int) {
        val modal = floatingViews.get(MODAL_ID)
        var params: LayoutParams
        var composeView: View
        if (modal == null) {
            params = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    LayoutParams.TYPE_SYSTEM_ALERT
                },
                LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }
            composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@FloatingWindowService)
                setViewTreeViewModelStoreOwner(this@FloatingWindowService)
                setViewTreeSavedStateRegistryOwner(this@FloatingWindowService)
                setContent {
                    ModalWindow(this@FloatingWindowService::resumeGame)
                }
            }
            floatingViews.put(MODAL_ID, composeView)
        } else {
            params = modal.layoutParams as LayoutParams
            composeView = modal
        }
        for (id in PAUSE_ID until (PAUSE_ID + pauseActionCount)) {
            val pause = floatingViews.get(id)
            if (pause != null) {
                windowManager.removeView(pause)
            }
        }
        windowManager.addView(composeView, params)
        for (id in PAUSE_ID until (PAUSE_ID + pauseActionCount)) {
            val pause = floatingViews.get(id)
            if (pause != null) {
                windowManager.addView(pause, pause.layoutParams)
            }
        }
        isPaused = true
    }

    fun resumeGame() {
        val view = floatingViews.get(MODAL_ID)
        if (view != null) {
            windowManager.removeView(view)
        }
        isPaused = false
    }

    fun addFloatingWindow(
        id: Int,
        position: Position,
        moveable: Boolean,
        onPositionChanged: ((position: Position) -> Unit)?,
        content: @Composable BoxScope.() -> Unit
    ) {
        if (floatingViews.get(id) != null) {
            removeFloatingWindow(id)
        }
        val params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                LayoutParams.TYPE_PHONE
            },
            LayoutParams.FLAG_NOT_FOCUSABLE or
                    LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
            gravity = Gravity.TOP or Gravity.START
            x = position.x
            y = position.y
        }
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingWindowService)
            setViewTreeViewModelStoreOwner(this@FloatingWindowService)
            setViewTreeSavedStateRegistryOwner(this@FloatingWindowService)
            setContent {
                FloatingWindow(
                    moveable,
                    onPositionChange = { dx, dy ->
                        params.x += dx
                        params.y += dy
                        windowManager.updateViewLayout(this, params)
                    },
                    onDragEnd = {
                        onPositionChanged?.invoke(Position(params.x, params.y))
                    },
                    content = content
                )
            }
        }
        windowManager.addView(composeView, params)
        floatingViews.put(id, composeView)
    }

    fun removeFloatingWindow(id: Int) {
        val view = floatingViews.get(id)
        if (view != null) {
            windowManager.removeView(view)
            floatingViews.remove(id)
        }
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "floating_window_channel",
                "悬浮窗服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "悬浮窗通知"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)

            val notification = Notification.Builder(this, "floating_window_channel")
                .setContentTitle("悬浮窗运行中")
                .setContentText("点击返回应用")
//                .setSmallIcon(R.drawable.ic_notification) TODO 添加通知按钮
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1, notification)
            }
        }
    }
}
