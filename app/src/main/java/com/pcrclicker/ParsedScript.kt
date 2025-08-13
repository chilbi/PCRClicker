package com.pcrclicker

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ParsedScript(
    val team: List<Chara>,
    val lines: List<List<Operate>>,
    val startSec: Int = 90
) {
    private val recordBuilder = StringBuilder()
    private var recordMessage = ""
    private var lastClickTime = 0L
    private var delayOn = 0
    private var delayOff = 0
    private var autoSpeed = false
    private var lineIndex = 0
    private var operateIndex = 0
    private var currentLine = lines[lineIndex]

    private val _currentOperate = MutableStateFlow(currentLine[operateIndex])
    val currentOperate= _currentOperate.asStateFlow()

    private val _summary = MutableStateFlow(getSummary())
    val summary = _summary.asStateFlow()

    private val _isOn = MutableStateFlow(false)
    val isOn = _isOn.asStateFlow()

    private val _recording = MutableStateFlow(false)
    val recording = _recording.asStateFlow()

    private val _autoClick = MutableStateFlow(false)
    val autoClick = _autoClick.asStateFlow()

    fun isFirstLine() = lineIndex <= 0
    fun isLastLine() = lineIndex >= lines.size - 1
    fun isStart()= lineIndex <= 0 && operateIndex <= 0
    fun isEnd() = lineIndex >= lines.size - 1 && operateIndex >= currentLine.size - 1

    fun nextOperate() {
        if (!isEnd()) {
            operateIndex += 1
            if (operateIndex < currentLine.size) {
                _currentOperate.value = currentLine[operateIndex]
            } else {
                lineIndex += 1
                currentLine = lines[lineIndex]
                operateIndex = 0
                _currentOperate.value = currentLine[operateIndex]
            }
            _summary.value = getSummary()
            _isOn.value = false
        }
    }

    fun prevOperate() {
        if (!isStart()) {
            operateIndex -= 1
            if (operateIndex > -1) {
                _currentOperate.value = currentLine[operateIndex]
            } else {
                lineIndex -= 1
                currentLine = lines[lineIndex]
                operateIndex = currentLine.size - 1
                _currentOperate.value = currentLine[operateIndex]
            }
            _summary.value = getSummary()
            _isOn.value = false
        }
    }

    fun nextLine() {
        if (!isLastLine()) {
            lineIndex += 1
            currentLine = lines[lineIndex]
            operateIndex = 0
            _currentOperate.value = currentLine[operateIndex]
            _summary.value = getSummary()
            _isOn.value = false
        }
    }

    fun prevLine() {
        if (!isFirstLine()) {
            lineIndex -= 1
            currentLine = lines[lineIndex]
            operateIndex = 0
            _currentOperate.value = currentLine[operateIndex]
            _summary.value = getSummary()
            _isOn.value = false
        }
    }

    fun restart() {
        if (!isStart() || _isOn.value) {
            lineIndex = 0
            currentLine = lines[lineIndex]
            operateIndex = 0
            _currentOperate.value = currentLine[operateIndex]
            _summary.value = getSummary()
            _isOn.value = false
        }
    }

    suspend fun handleClickOperate(settings: Settings, save: (content: String) -> Unit) {
        val operate = _currentOperate.value
        if (operate is Operate.Click) {
            operate.type.getPosition(settings).performClick()
            if (_recording.value) {
                val currentTime = System.currentTimeMillis()
                val timeDiff = (currentTime - lastClickTime).toInt()
                lastClickTime = currentTime
                if (_isOn.value) {
                    delayOff = timeDiff
                    if (operateIndex <= 0) {
                        recordBuilder.append("\n${operate.sec.toNumMinute()} ${operate.getText(team)}")
                    } else {
                        recordBuilder.append(" ${operate.getText(team)}")
                        if (operate.sec != currentLine[operateIndex - 1].sec) {
                            recordBuilder.append("(${operate.sec.toNumMinute()})")
                        }
                    }
                    recordBuilder.append("[$delayOn,$delayOff]")
                    delayOn = 0
                    delayOff = 0
                    if (isEnd()) {
                        stopRecord(settings, save)
                    }
                } else {
                    delayOn = timeDiff
                    if (!autoSpeed) {
                        settings.speedPosition.performClick(60L)
                        autoSpeed = true
                    }
                }
            }
        }
        if (_isOn.value) {
            nextOperate()
            if (_recording.value) {
                requireStopRecord(settings, save)
            }
        } else {
            _isOn.value = true
        }
    }

    fun handleClickSpeed(settings: Settings) {
        settings.speedPosition.performClick()
    }

    fun handleClickMenu(settings: Settings) {
        settings.menuPosition.performClick()
    }

    suspend fun startAutoClick(settings: Settings) {
        _autoClick.value = true
        autoSpeed = false
        nextOperate()
        //必须处于游戏暂停界面
        settings.blankPosition.performClick()
        autoClick(settings)
    }

    suspend fun stopAutoClick(settings: Settings) {
        if (_autoClick.value) {
            //关加速
            if (autoSpeed) {
                delay(60L)
                settings.speedPosition.performClick()
                autoSpeed = false
                delay(60L)
            }
            //暂停游戏
            settings.menuPosition.performClick()
            _autoClick.value = false
        } else {
            nextOperate()
        }
    }

    suspend fun startRecord(settings: Settings, save: (content: String) -> Unit) {
        _recording.value = true
        autoSpeed = false
        val recordOperate = currentOperate.value
        recordMessage = recordOperate.message
        recordBuilder.clear()
        recordBuilder.append(team.joinToString(" ") { "${it.num}=${it.name}" })
        recordBuilder.append("\n")
        if (lineIndex > 0) {
            for (i in 0 until lineIndex) {
                recordBuilder.append(operateListToString(lines[i]))
                recordBuilder.append("\n")
            }
        }
        if (operateIndex > 0) {
            val operates = currentLine.take(operateIndex)
            recordBuilder.append(operateListToString(operates))
        } else {
            recordBuilder.append(recordOperate.sec.toNumMinute())
        }
        recordBuilder.append(" start$recordMessage")
        nextOperate()
        if (!requireStopRecord(settings, save)) {
            //必须处于游戏暂停界面
            settings.blankPosition.performClick()
            lastClickTime = System.currentTimeMillis()
        }
    }

    suspend fun stopRecord(settings: Settings, save: (content: String) -> Unit) {
        //关加速
        if (autoSpeed) {
            delay(60L)
            settings.speedPosition.performClick()
            autoSpeed = false
            delay(60L)
        }
        //暂停游戏
        settings.menuPosition.performClick()
        _recording.value = false
        val countdown = Countdown(currentLine[0].sec)
        if (operateIndex <= 0) {
            recordBuilder.append("\n${countdown.sec.toNumMinute()}")
        } else {
            countdown.sec = currentLine[operateIndex - 1].sec
        }
        recordBuilder.append(" stop$recordMessage")
        if (operateIndex < currentLine.size) {
            for (i in operateIndex until currentLine.size) {
                recordBuilder.append(" ")
                recordBuilder.append(operateToString(currentLine[i], countdown))
            }
        }
        if (lineIndex < lines.size - 1) {
            for (i in lineIndex + 1 until lines.size) {
                recordBuilder.append("\n")
                recordBuilder.append(operateListToString(lines[i]))
            }
        }
        save(recordBuilder.toString())
        recordMessage = ""
        delayOn = 0
        delayOff = 0
        recordBuilder.clear()
    }

    fun toCompensation(sec: Int): ParsedScript {
        val diff = 90 - sec
        val newLines = mutableListOf<List<Operate>>()
        for (operates in lines) {
            val newOperates = mutableListOf<Operate>()
            for (operate in operates) {
                val newSec = operate.sec - diff
                if (newSec < 0) break
                newOperates.add(
                    when (operate) {
                        is Operate.Click -> operate.copy(sec = newSec)
                        is Operate.Confirm -> operate.copy(sec = newSec)
                        is Operate.Record -> operate.copy(sec = newSec)
                        is Operate.Start -> operate.copy(sec = newSec)
                        is Operate.Stop -> operate.copy(sec = newSec)
                    }
                )
            }
            if (newOperates.isEmpty()) break
            newLines.add(newOperates)
        }
        return ParsedScript(team, newLines, sec)
    }

    private suspend fun autoClick(settings: Settings) {
        val operate = currentOperate.value
        when (operate) {
            is Operate.Stop -> {
                stopAutoClick(settings)
            }
            is Operate.Click -> {
                val delayTime = if (_isOn.value) operate.delayOff else operate.delayOn
                delay((delayTime as Int).toLong())
                handleClickOperate(settings) {}
                if (!autoSpeed) {
                    settings.speedPosition.performClick(60L)
                    autoSpeed = true
                }
                if (_autoClick.value) {
                    autoClick(settings)
                }
            }
            else -> {
                nextOperate()
                autoClick(settings)
            }
        }
    }

    private suspend fun requireStopRecord(settings: Settings, save: (content: String) -> Unit): Boolean {
        var stop = false
        val operate = _currentOperate.value
        if (operate !is Operate.Click || operate.type == ClickType.MENU) {
            stop = true
        }
        if (isBossUb(operate)) {
            stop = false
        }
        if (stop) {
            stopRecord(settings, save)
        }
        return stop
    }

    private fun isBossUb(operate: Operate): Boolean {
        if (operate is Operate.Confirm) {
            if (arrayOf("boss大招", "bossub", "bub").contains(operate.message.lowercase())) {
                return true
            }
        }
        return false
    }

    private fun operateListToString(operates: List<Operate>): String {
        return buildString {
            val countdown = Countdown(operates[0].sec)
            append(countdown.sec.toNumMinute())
            for (operate in operates) {
                append(" ")
                append(operateToString(operate, countdown))
            }
        }
    }

    private fun operateToString(operate: Operate, countdown: Countdown): String {
        return buildString {
            append(operate.getText(team))
            val currSec = operate.sec
            if (currSec != countdown.sec) {
                append("(${currSec.toNumMinute()})")
                countdown.sec = currSec
            }
            if (operate is Operate.Click) {
                if (operate.delayOn != null && operate.delayOff != null) {
                    append("[${operate.delayOn},${operate.delayOff}]")
                }
            }
        }
    }

    private fun getSummary(): List<String> {
        val current = _currentOperate.value.getText(team)
        val currIndex = operateIndex
        val currLine = currentLine
        var prev: String
        if (isFirstLine()) {
            prev = "${startSec.toMinute()} 开始\n"
        } else {
            val prevLine = lines[lineIndex - 1]
            prev = prevLine[0].sec.toMinute()
            for (i in 0 until prevLine.size) {
                prev += " " + prevLine[i].getText(team)
            }
            prev += "\n"
        }
        prev += currLine[0].sec.toMinute() + " "
        var next = ""
        if (currIndex > 0) {
            for (i in 0 until currIndex) {
                prev += currLine[i].getText(team) + " "
            }
        }
        if (currIndex < currLine.size - 1) {
            for (i in (currIndex + 1) until currLine.size) {
                next += " " + currLine[i].getText(team)
            }
        }
        next += "\n"
        if (isLastLine()) {
            next += "0 结束"
        } else {
            val nextLine = lines[lineIndex + 1]
            next += nextLine[0].sec.toMinute()
            for (i in 0 until nextLine.size) {
                next += " " + nextLine[i].getText(team)
            }
        }

        return listOf(prev, current, next)
    }
}

data class Chara(
    val num: Int,
    val name: String
)

enum class ClickType {
    SET1, SET2, SET3, SET4, SET5, AUTO1, AUTO2, AUTO3, AUTO4, AUTO5, MENU;
    fun getPosition(settings: Settings): Position {
        return when (this) {
            SET1 -> settings.ub1Position
            SET2 -> settings.ub2Position
            SET3 -> settings.ub3Position
            SET4 -> settings.ub4Position
            SET5 -> settings.ub5Position
            MENU -> settings.menuPosition
            else -> settings.autoPosition
        }
    }

    fun getText(team: List<Chara>): String {
        return when (this) {
            SET1 -> team.find { it.num == 1 }?.name ?: "?"
            SET2 -> team.find { it.num == 2 }?.name ?: "?"
            SET3 -> team.find { it.num == 3 }?.name ?: "?"
            SET4 -> team.find { it.num == 4 }?.name ?: "?"
            SET5 -> team.find { it.num == 5 }?.name ?: "?"
            AUTO1 -> "auto" + (team.find { it.num == 1 }?.name ?: "?")
            AUTO2 -> "auto" + (team.find { it.num == 2 }?.name ?: "?")
            AUTO3 -> "auto" + (team.find { it.num == 3 }?.name ?: "?")
            AUTO4 -> "auto" + (team.find { it.num == 4 }?.name ?: "?")
            AUTO5 -> "auto" + (team.find { it.num == 5 }?.name ?: "?")
            MENU -> "menu"
        }
    }

    fun getBadgeText(): String {
        return when (this) {
            MENU -> "MENU"
            SET1, SET2, SET3, SET4, SET5 -> "SET"
            else -> "AUTO"
        }
    }
}

sealed class Operate {
    abstract val sec: Int
    abstract val message: String

    data class Click(override val sec: Int, val type: ClickType, val delayOn: Int?, val delayOff: Int?) : Operate() {
        override val message: String = ""
    }
    data class Confirm(override val sec: Int, override val message: String) : Operate()
    data class Record(override val sec: Int, override val message: String) : Operate()
    data class Start(override val sec: Int, override val message: String) : Operate()
    data class Stop(override val sec: Int, override val message: String) : Operate()

    fun getText(team: List<Chara>): String {
        return when (this) {
            is Click -> this.type.getText(team)
            is Confirm -> this.message
            is Record -> "record${this.message}"
            is Start -> "start${this.message}"
            is Stop -> "stop${this.message}"
        }
    }
}

private class Countdown(var sec: Int)

class SyntaxException(val lineNum: Int, val mes: String) : Exception("第${lineNum}行语法错误: $mes")

@Throws(SyntaxException::class)
fun analyzeSyntax(textLines: List<String>): ParsedScript? {
    if (textLines.isEmpty()) {
        throw SyntaxException(1, "文件不能为空")
    }

    val firstLine = textLines[0]
    val teamDefine = firstLine.split(' ').filter { it.isNotBlank() }

    if (teamDefine.isEmpty()) {
        throw SyntaxException(1, "第1行必须定义队伍，例：1=佩可 2=凯露 3=可可萝")
    }

    val team = mutableListOf<Chara>()
    for (part in teamDefine) {
        val charaDefine = part.split("=")
        if (charaDefine.size < 2) {
            throw SyntaxException(1, "角色定义格式：1=佩可")
        }
        val charaNum = charaDefine[0].toIntOrNull()
        if (charaNum == null) {
            throw SyntaxException(1, "=号前必须为数字")
        }
        if (charaNum < 1 || charaNum > 5) {
            throw SyntaxException(1, "=号前的数字必须在1-5之间")
        }
        val charaName = charaDefine[1]
        if (charaName.isEmpty()) {
            throw SyntaxException(1, "=号后的角色名不能空")
        }
        if (team.isNotEmpty()) {
            if (team.find { it.num == charaNum } != null) {
                throw SyntaxException(1, "重复定义角色编号：$charaNum")
            }
            if (team.find { it.name == charaName } != null) {
                throw SyntaxException(1, "重复定义角色名字：$charaName")
            }
        }
        team.add(Chara(charaNum, charaName))
    }
    if (team.isEmpty()) {
        throw SyntaxException(1, "第1行必须定义队伍，例：1=佩可 2=凯露 3=可可萝")
    }

    val lines = mutableListOf<List<Operate>>()
    val countdown = Countdown(90)
    for (lineIndex in 1 until textLines.size) {
        val line = textLines[lineIndex]
        if (line.isBlank()) continue
        val lineNum = lineIndex + 1
        val operates = mutableListOf<Operate>()
        val trimmedLine = line.trim()
        val operateDefineList = trimmedLine.split(' ').filter { it.isNotBlank() }

        val firstDefine = operateDefineList[0]
        val firstSec = firstDefine.toSec(lineNum)
        if (firstSec == null) {
            val operate = analyzeOperateDefine(firstDefine, lineNum, countdown, team)
            operates.add(operate)
        } else {
            if (firstSec > countdown.sec) {
                throw SyntaxException(lineNum, "秒数不能大于前面的秒数(${countdown.sec})")
            }
            countdown.sec = firstSec
        }

        if (operateDefineList.size > 1) {
            for (operateIndex in 1 until operateDefineList.size) {
                val operateDefine = operateDefineList[operateIndex]
                val operate = analyzeOperateDefine(operateDefine, lineNum, countdown, team)
                operates.add(operate)
            }
        }
        if (operates.isNotEmpty()) {
            lines.add(operates)
        }
    }

    if (lines.isEmpty()) {
        throw SyntaxException(2, "轴不能空，例：121 佩可 凯露 可可萝(113) auto凯露(59) BOSS大招(57)")
    }
    try {
        return ParsedScript(team.sortedByDescending { it.num }, lines)
    } catch (_: Throwable) {}
    return null
}

private val nameSecPattern = Regex("""(\S+)\((\d+)\)$""")
private val delayPattern = Regex("""(\S+)\[(\d+),(\d+)]$""")

@Throws(SyntaxException::class)
private fun analyzeOperateDefine(operateDefine: String, lineNum: Int, countdown: Countdown, team: List<Chara>): Operate {
    var defineText = operateDefine

    if (defineText == "menu") {
        return Operate.Click(countdown.sec, ClickType.MENU, null, null)
    }

    if (defineText.startsWith("record")) {
        return Operate.Record(countdown.sec, defineText.substring(6))
    }

    if (defineText.startsWith("start")) {
        return Operate.Start(countdown.sec, defineText.substring(5))
    }

    if (defineText.startsWith("stop")) {
        return Operate.Stop(countdown.sec, defineText.substring(4))
    }

    var isAuto = false
    if (defineText.startsWith("auto")) {
        defineText = operateDefine.substring(4)
        isAuto = true
    }

    var delayOn: Int? = null
    var delayOff: Int? = null
    val delayMatchResult = delayPattern.find(defineText)
    if (delayMatchResult != null) {
        val (noDelayText, delayOnStr, delayOffStr) = delayMatchResult.destructured
        delayOn = delayOnStr.toIntOrNull()
        delayOff = delayOffStr.toIntOrNull()
        defineText = noDelayText
    }

    val nameSecMatchResult = nameSecPattern.find(defineText)
    if (nameSecMatchResult != null) {//匹配 name(sec)
        val (noSecText, secStr) = nameSecMatchResult.destructured
        val sec = secStr.toSec(lineNum)
        if (sec != null) {
            if (sec > countdown.sec) {
                throw SyntaxException(lineNum, "秒数不能大于前面的秒数(${countdown.sec})")
            }
            countdown.sec = sec
            defineText = noSecText
        }
    }

    val findChara = team.find { it.name == defineText }
    if (findChara != null) {
        return Operate.Click(
            countdown.sec,
            if (isAuto) getAUTOType(findChara.num) else getSETType(findChara.num),
            delayOn,
            delayOff
        )
    }
    return Operate.Confirm(countdown.sec, defineText)
}

private fun getSETType(charaNum: Int): ClickType {
    return when (charaNum) {
        1 -> ClickType.SET1
        2 -> ClickType.SET2
        3 -> ClickType.SET3
        4 -> ClickType.SET4
        else -> ClickType.SET5
    }
}

private fun getAUTOType(charaNum: Int): ClickType {
    return when (charaNum) {
        1 -> ClickType.AUTO1
        2 -> ClickType.AUTO2
        3 -> ClickType.AUTO3
        4 -> ClickType.AUTO4
        else -> ClickType.AUTO5
    }
}

// 例如把120或1:20的时间转为秒数80
@Throws(SyntaxException::class)
private fun String.toSec(lineNum: Int): Int? {
    val time = this.toIntOrNull()
     if (time == null) {
        if (this.contains(":")) {
            val noColonTimeList = this.split(":")
            if (noColonTimeList.size == 2 && noColonTimeList.all { it.toIntOrNull() != null }) {
                val noColonTime = noColonTimeList[0] + noColonTimeList[1]
                return noColonTime.toSec(lineNum)
            } else {
                return null
            }
        } else {
            return null
        }
    } else {
        return time.toSec(lineNum)
    }
}

@Throws(SyntaxException::class)
private fun Int.toSec(lineNum: Int): Int {
    if (this < 0) {
        throw SyntaxException(lineNum, "秒数不能小于0")
    }
    if (this > 130 || (this < 100 && this > 90)) {
        throw SyntaxException(lineNum, "秒数不能大于90")
    }
    return if (this >= 100) {
        this - 40//120(即1:20)->120-100+60(即去掉首位1后加上60秒)
    } else {
        this
    }
}

fun Int.toMinute(): String {
    return if (this > 59) {
        val sec = this - 60
        "1:${if (sec > 9) "" else "0"}$sec"
    } else {
        this.toString()
    }
}

fun Int.toNumMinute(): String {
    return if (this > 59) {
        val sec = this - 60
        "1${if (sec > 9) "" else "0"}$sec"
    } else {
        this.toString()
    }
}

fun Position.performClick(startTime: Long = 0) {
    AutoClickService.getInstance()?.performClick(x, y, startTime)
}
