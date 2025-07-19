package com.pcrclicker

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class Settings(
    val blankPosition: Position = Position(150, 75),
    val menuPosition: Position = Position(2250, 40),
    val autoPosition: Position = Position(2280, 830),
    val speedPosition: Position = Position(2280, 970),
    val ub1Position: Position = Position(1660, 870),
    val ub2Position: Position = Position(1420, 870),
    val ub3Position: Position = Position(1180, 870),
    val ub4Position: Position = Position(940, 870),
    val ub5Position: Position = Position(700, 870),
    val pauseActions: List<PauseAction> = listOf(
        PauseAction(
            "0ms",
            Position(30, 200),
            600,
            0
        ),
        PauseAction(
            "150ms",
            Position(30, 350),
            600,
            150
        )
    )
)

@Serializable
data class Position(
    val x: Int,
    val y: Int
)

@Serializable
data class PauseAction(
    val name: String,
    val position: Position,
    val closePauseWindowDelayTime: Long,
    val pauseBattleDelayTime: Long
)

class PositionData(
    val position: Position,
    val name: String
)

fun Settings.getPositions(): Array<PositionData> {
    return arrayOf(
        PositionData(blankPosition, "空白位置"),
        PositionData(menuPosition, "菜单位置"),
        PositionData(autoPosition, "Auto位置"),
        PositionData(speedPosition, "加速位置"),
        PositionData(ub1Position, "1号位置"),
        PositionData(ub2Position, "2号位置"),
        PositionData(ub3Position, "3号位置"),
        PositionData(ub4Position, "4号位置"),
        PositionData(ub5Position, "5号位置"),
    )
}

object UserSettingsSerializer : Serializer<Settings> {
    override val defaultValue = Settings()

    override suspend fun readFrom(input: InputStream): Settings {
        return try {
            Json.Default.decodeFromString<Settings>(input.readBytes().decodeToString())
        } catch (e: Exception) {
            throw CorruptionException("Cannot read settings", e)
        }
    }

    override suspend fun writeTo(t: Settings, output: OutputStream) {
        output.write(Json.Default.encodeToString(t).toByteArray())
    }
}

val Context.settingsDataStore: DataStore<Settings> by dataStore(
    fileName = "user_settings.pb",
    serializer = UserSettingsSerializer
)