package com.pcrclicker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF006A8E),      // 深蓝色 - 夏日海洋
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC1E8FF), // 浅蓝色 - 清澈海水
    onPrimaryContainer = Color(0xFF001E2C),

    secondary = Color(0xFFF4B400),     // 阳光黄色 - 夏日阳光
    onSecondary = Color(0xFF3E2800),
    secondaryContainer = Color(0xFFFFE08C), // 浅黄色 - 阳光反射
    onSecondaryContainer = Color(0xFF271900),

    tertiary = Color(0xFFFE6D73),      // 珊瑚红色 - 夏日日落
    onTertiary = Color(0xFF5E0E12),
    tertiaryContainer = Color(0xFFFFDAD8), // 浅珊瑚色
    onTertiaryContainer = Color(0xFF410005),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFF8FDFF),    // 非常浅的蓝色 - 晴朗天空
    onBackground = Color(0xFF001F25),

    surface = Color(0xFFF8FDFF),
    onSurface = Color(0xFF001F25),
    surfaceVariant = Color(0xFFDEE3EB), // 浅灰色带蓝色调
    onSurfaceVariant = Color(0xFF41474D),

    outline = Color(0xFF72787E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FD2FF),      // 明亮的蓝色 - 夜晚的泳池
    onPrimary = Color(0xFF003549),
    primaryContainer = Color(0xFF004C68),
    onPrimaryContainer = Color(0xFFC1E8FF),

    secondary = Color(0xFFFFC107),     // 保持明亮的黄色 - 夜晚的灯光
    onSecondary = Color(0xFF422C00),
    secondaryContainer = Color(0xFF5E4200),
    onSecondaryContainer = Color(0xFFFFE08C),

    tertiary = Color(0xFFFFB3AE),      // 柔和的珊瑚色 - 夜晚的霓虹
    onTertiary = Color(0xFF68000F),
    tertiaryContainer = Color(0xFF92001E),
    onTertiaryContainer = Color(0xFFFFDAD8),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF001F25),    // 深蓝色 - 夏夜天空
    onBackground = Color(0xFFA6EEFF),

    surface = Color(0xFF001F25),
    onSurface = Color(0xFFA6EEFF),
    surfaceVariant = Color(0xFF41474D),
    onSurfaceVariant = Color(0xFFC1C7CE),

    outline = Color(0xFF8B9198),
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}

//@Composable
//fun AppTheme(
//    darkTheme: Boolean = isSystemInDarkTheme(),
//    // Dynamic color is available on Android 12+
//    dynamicColor: Boolean = true,
//    content: @Composable () -> Unit
//) {
//    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }
//
//    MaterialTheme(
//        colorScheme = colorScheme,
//        typography = Typography,
//        content = content
//    )
//}