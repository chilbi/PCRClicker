package com.pcrclicker

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ParsedScript(
    val team: List<Chara>,
    val lines: List<List<Operate>>,
    val startSec: Int = 90
) {
    private val lineIndex = MutableStateFlow(0)
    private val operateIndex = MutableStateFlow(0)

    private val currentLine = MutableStateFlow(lines[lineIndex.value])

    private val _currentOperate = MutableStateFlow(currentLine.value[operateIndex.value])
    val currentOperate= _currentOperate.asStateFlow()

    private val _summary = MutableStateFlow(getSummary())
    val summary = _summary.asStateFlow()

    private val _isOn = MutableStateFlow(false)
    val isOn = _isOn.asStateFlow()

    fun isFirstLine() = lineIndex.value <= 0
    fun isLastLine() = lineIndex.value >= lines.size - 1
    fun isStart()= lineIndex.value <= 0 && operateIndex.value <= 0
    fun isEnd() = lineIndex.value >= lines.size - 1 && operateIndex.value >= currentLine.value.size - 1

    fun nextOperate() {
        if (!isEnd()) {
            operateIndex.value += 1
            if (operateIndex.value < currentLine.value.size) {
                _currentOperate.value = currentLine.value[operateIndex.value]
            } else {
                lineIndex.value += 1
                currentLine.value = lines[lineIndex.value]
                operateIndex.value = 0
                _currentOperate.value = currentLine.value[operateIndex.value]
            }
            _summary.value = getSummary()
            _isOn.value = false
        }
    }

    fun prevOperate() {
        if (!isStart()) {
            operateIndex.value -= 1
            if (operateIndex.value > -1) {
                _currentOperate.value = currentLine.value[operateIndex.value]
            } else {
                lineIndex.value -= 1
                currentLine.value = lines[lineIndex.value]
                operateIndex.value = currentLine.value.size - 1
                _currentOperate.value = currentLine.value[operateIndex.value]
            }
            _summary.value = getSummary()
            _isOn.value = false
        }
    }

    fun nextLine() {
        if (!isLastLine()) {
            lineIndex.value += 1
            currentLine.value = lines[lineIndex.value]
            operateIndex.value = 0
            _currentOperate.value = currentLine.value[operateIndex.value]
            _summary.value = getSummary()
            _isOn.value = false
        }
    }

    fun prevLine() {
        if (!isFirstLine()) {
            lineIndex.value -= 1
            currentLine.value = lines[lineIndex.value]
            operateIndex.value = 0
            _currentOperate.value = currentLine.value[operateIndex.value]
            _summary.value = getSummary()
            _isOn.value = false
        }
    }

    fun restart() {
        if (!isStart() || _isOn.value) {
            lineIndex.value = 0
            currentLine.value = lines[lineIndex.value]
            operateIndex.value = 0
            _currentOperate.value = currentLine.value[operateIndex.value]
            _summary.value = getSummary()
            _isOn.value = false
        }
    }

    fun handleClickOperate(settings: Settings) {
        val operate = _currentOperate.value
        if (operate is Operate.Click) {
            val position = operate.type.getPosition(settings)
            AutoClickService.getInstance()?.performClick(position.x, position.y)
        }
        if (_isOn.value) {
            nextOperate()
        } else {
            _isOn.value = true
        }
    }

    fun handleClickSpeed(settings: Settings) {
        val position = settings.speedPosition
        AutoClickService.getInstance()?.performClick(position.x, position.y)
    }

    fun handleClickMenu(settings: Settings) {
        val position = settings.menuPosition
        AutoClickService.getInstance()?.performClick(position.x, position.y)
    }

    fun toCompensation(sec: Int): ParsedScript {
        val diff = 90 - sec
        val newLines = mutableListOf<List<Operate>>()
        for (operates in lines) {
            val newOperates = mutableListOf<Operate>()
            for (operate in operates) {
                if (operate is Operate.Click) {
                    val newSec = operate.sec - diff
                    if (newSec < 0) break
                    newOperates.add(operate.copy(sec = newSec))
                } else if (operate is Operate.Confirm) {
                    val newSec = operate.sec - diff
                    if (newSec < 0) break
                    newOperates.add(operate.copy(sec = newSec))
                }
            }
            if (newOperates.isEmpty()) break
            newLines.add(newOperates)
        }
        return ParsedScript(team, newLines, sec)
    }

    private fun getSummary(): List<String> {
        val current = _currentOperate.value.getName(team)
        val currIndex = operateIndex.value
        val currLine = currentLine.value
        var prev: String
        if (isFirstLine()) {
            prev = "${startSec.toMinute()} 开始\n"
        } else {
            val prevLine = lines[lineIndex.value - 1]
            prev = prevLine[0].getSeconds().toMinute()
            for (i in 0 until prevLine.size) {
                prev += " " + prevLine[i].getName(team)
            }
            prev += "\n"
        }
        prev += currLine[0].getSeconds().toMinute() + " "
        var next = ""
        if (currIndex > 0) {
            for (i in 0 until currIndex) {
                prev += currLine[i].getName(team) + " "
            }
        }
        if (currIndex < currLine.size - 1) {
            for (i in (currIndex + 1) until currLine.size) {
                next += " " + currLine[i].getName(team)
            }
        }
        next += "\n"
        if (isLastLine()) {
            next += "0 结束"
        } else {
            val nextLine = lines[lineIndex.value + 1]
            next += nextLine[0].getSeconds().toMinute()
            for (i in 0 until nextLine.size) {
                next += " " + nextLine[i].getName(team)
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

    fun getButtonName(team: List<Chara>): String {
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
            else -> "游戏菜单"
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
    data class Click(val sec: Int, val type: ClickType) : Operate()
    data class Confirm(val sec: Int, val message: String) : Operate()

    fun getName(team: List<Chara>): String {
        return when (this) {
            is Click -> this.type.getButtonName(team)
            is Confirm -> this.message
        }
    }

    fun getSeconds(): Int {
        return when (this) {
            is Click -> this.sec
            is Confirm -> this.sec
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

@Throws(SyntaxException::class)
private fun analyzeOperateDefine(operateDefine: String, lineNum: Int, countdown: Countdown, team: List<Chara>): Operate {
    var defineText = operateDefine
    var isAuto = false
    if (operateDefine.startsWith("auto")) {
        defineText = operateDefine.substring(4)
        isAuto = true
    }

    val matchResult = nameSecPattern.find(defineText)
    if (matchResult != null) {//匹配 name(sec)
        val (noSecText, secStr) = matchResult.destructured
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
            if (isAuto) getAUTOType(findChara.num) else getSETType(findChara.num)
        )
    }
    if (defineText == "menu") {
        return Operate.Click(countdown.sec, ClickType.MENU)
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
