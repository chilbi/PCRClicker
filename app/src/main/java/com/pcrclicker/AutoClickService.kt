package com.pcrclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AutoClickService : AccessibilityService() {
    companion object {
        private var instance: AutoClickService? = null

        fun getInstance(): AutoClickService? = instance

        private fun isStandardAccessibilityEnabled(
            context: Context,
            serviceClass: Class<out AccessibilityService>
        ): Boolean {
            return try {
                val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                    ?.any { enabled ->
                        enabled.resolveInfo?.serviceInfo?.let { info ->
                            info.packageName == context.packageName &&
                                    info.name == serviceClass.name
                        } ?: false
                    } ?: false
            } catch (e: Exception) {
                false
            }
        }

        private fun isSettingsSecureAccessibilityEnabled(
            context: Context,
            serviceClass: Class<out AccessibilityService>
        ): Boolean {
            return try {
                val serviceName = ComponentName(context.packageName, serviceClass.name).flattenToString()
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )?.split(':')?.any { it == serviceName } ?: false
            } catch (e: Exception) {
                false
            }
        }

        private fun isHuaweiAccessibilityEnabled(
            context: Context,
            serviceClass: Class<out AccessibilityService>
        ): Boolean {
            return try {
                // 华为有时使用不同的键值
                val keys = listOf(
                    "enabled_accessibility_services",  // 标准键
                    "accessibility_enabled_services"   // 华为可能使用的键
                )

                keys.any { key ->
                    Settings.Secure.getString(context.contentResolver, key)
                        ?.contains("${context.packageName}/${serviceClass.name}") ?: false
                }
            } catch (e: Exception) {
                false
            }
        }

        private fun isXiaomiAccessibilityEnabled(
            context: Context,
            serviceClass: Class<out AccessibilityService>
        ): Boolean {
            return try {
                // 小米有时需要检查多个设置项
                val globalSetting = Settings.Global.getString(
                    context.contentResolver,
                    "enabled_accessibility_services"
                )

                val secureSetting = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )

                val serviceId = "${context.packageName}/${serviceClass.name}"
                (globalSetting?.contains(serviceId) == true) ||
                (secureSetting?.contains(serviceId) == true)
            } catch (e: Exception) {
                false
            }
        }

        private fun isOppoAccessibilityEnabled(
            context: Context,
            serviceClass: Class<out AccessibilityService>
        ): Boolean {
            return try {
                // OPPO 有时需要特殊处理
                val serviceName = "${context.packageName}/${serviceClass.name}"
                val setting = Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_accessibility_services"
                ) ?: ""

                // OPPO 可能使用 | 分隔
                setting.split(':', '|').any { it == serviceName }
            } catch (e: Exception) {
                false
            }
        }

        private fun isVivoAccessibilityEnabled(
            context: Context,
            serviceClass: Class<out AccessibilityService>
        ): Boolean {
            return try {
                // Vivo 有时需要检查安全中心的状态
                val serviceName = "${context.packageName}/${serviceClass.name}"
                val setting = Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_accessibility_services"
                ) ?: ""

                // Vivo 可能使用不同的分隔符
                setting.split(':', ',', ';').any { it.trim() == serviceName }
            } catch (e: Exception) {
                false
            }
        }

        private fun isAccessibilityServiceReallyEnabled(
            context: Context,
            serviceClass: Class<out AccessibilityService>
        ): Boolean {
            // 按优先级尝试各种检测方法
            return when {
                // 1. 先尝试标准检测
                isStandardAccessibilityEnabled(context, serviceClass) -> true

                // 2. 尝试Settings.Secure标准检测
                isSettingsSecureAccessibilityEnabled(context, serviceClass) -> true

                // 3. 根据厂商尝试特殊检测
                Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true) ||
                        Build.MANUFACTURER.equals("HONOR", ignoreCase = true) ->
                    isHuaweiAccessibilityEnabled(context, serviceClass)

                Build.MANUFACTURER.equals("XIAOMI", ignoreCase = true) ||
                        Build.MANUFACTURER.equals("REDMI", ignoreCase = true) ->
                    isXiaomiAccessibilityEnabled(context, serviceClass)

                Build.MANUFACTURER.equals("OPPO", ignoreCase = true) ->
                    isOppoAccessibilityEnabled(context, serviceClass)

                Build.MANUFACTURER.equals("VIVO", ignoreCase = true) ->
                    isVivoAccessibilityEnabled(context, serviceClass)

                // 4. 最后再尝试一次Settings.Secure
                else -> isSettingsSecureAccessibilityEnabled(context, serviceClass)
            }
        }

        fun isServiceEnabled(context: Context): Boolean {
            return isAccessibilityServiceReallyEnabled(context, AutoClickService::class.java)
//            val am = context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
//            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
//            val serviceJavaName = AutoClickService::class.java.name
//            val isEnabled = enabledServices.any {
//                it.resolveInfo.serviceInfo.packageName == context.packageName &&
//                it.resolveInfo.serviceInfo.name == serviceJavaName
//            }
//            if (isEnabled) {
//                return true
//            }
//            val settingValue = Settings.Secure.getString(
//                context.contentResolver,
//                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
//            ) ?: return false
//
//            val serviceName = "${context.packageName}/${AutoClickService::class.java.name}"
//            return settingValue.split(':').any {
//                it.contains(serviceName)
//            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 可以处理无障碍事件
    }

    override fun onInterrupt() {
        // 服务中断时调用
    }

    fun performClick(
        x: Int,
        y: Int,
        startTime: Long = 0,
        duration: Long = 50
    ) {
        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
            // Swipe
            // moveTo(startX.toFloat(), startY.toFloat())
            // lineTo(endX.toFloat(), endY.toFloat())
        }
        val gestureDescription = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path, startTime, duration
                )
            )
            .build()
        dispatchGesture(gestureDescription, null, null)
    }

    private var clickJob: Job? = null

    fun startAutoClick(
        x: Int,
        y: Int,
        interval: Long,
        times: Int = Int.MAX_VALUE
    ) {
        stopAutoClick()
        clickJob = CoroutineScope(Dispatchers.Default).launch {
            repeat(times) {
                if (isActive) {
                    performClick(x, y)
                    delay(interval)
                }
            }
        }
    }

    fun stopAutoClick() {
        clickJob?.cancel()
        clickJob = null
    }
}