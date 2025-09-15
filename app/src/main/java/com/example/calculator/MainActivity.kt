package com.example.calculator

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var tvDisplay: TextView
    private lateinit var tvProcess: TextView

    private var currentNumber: String = "0"
    private var fullExpression: String = ""
    private var lastWasOperator = false
    private var lastWasEquals = false

    // 连续等号功能 - 添加记录上次运算的变量
    private var lastOperator: String = ""
    private var lastOperand: Double = 0.0
    private var lastOperation: String = ""

    // 履歴機能用
    private val calculationHistory = mutableListOf<String>()
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            sharedPrefs = getSharedPreferences("calculator_prefs", Context.MODE_PRIVATE)
            loadHistory()
            initializeViews()
            setupClickListeners()
            updateDisplay("0")
            updateProcessDisplay("")
        } catch (e: Exception) {
            // 初始化失败时不崩溃
        }
    }

    private fun initializeViews() {
        tvDisplay = findViewById(R.id.tvDisplay)
        tvProcess = findViewById(R.id.tvProcess)
    }

    private fun setupClickListeners() {
        // 数字按钮
        val numberIds = intArrayOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        try {
            numberIds.forEach { id ->
                findViewById<Button>(id).setOnClickListener(this)
            }

            // 基本按钮
            findViewById<Button>(R.id.btnPlus).setOnClickListener(this)
            findViewById<Button>(R.id.btnMinus).setOnClickListener(this)
            findViewById<Button>(R.id.btnMultiply).setOnClickListener(this)
            findViewById<Button>(R.id.btnDivide).setOnClickListener(this)
            findViewById<Button>(R.id.btnEquals).setOnClickListener(this)
            findViewById<Button>(R.id.btnClear).setOnClickListener(this)
            findViewById<Button>(R.id.btnBackspace).setOnClickListener(this)
            findViewById<Button>(R.id.btnDecimal).setOnClickListener(this)
            findViewById<Button>(R.id.btnPlusMinus).setOnClickListener(this)

            // 特殊按钮
            findViewById<Button>(R.id.btnLeftBracket).setOnClickListener(this)
            findViewById<Button>(R.id.btnRightBracket).setOnClickListener(this)
            findViewById<Button>(R.id.btnPercent).setOnClickListener(this)

            // 履歴按钮（长按）
            findViewById<Button>(R.id.btnClear).setOnLongClickListener {
                showHistoryDialog()
                true
            }
        } catch (e: Exception) {
            // 按钮设置失败时不崩溃
        }
    }

    override fun onClick(v: View?) {
        try {
            when (v?.id) {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9 -> {
                    val button = v as Button
                    handleNumberInput(button.text.toString())
                }

                R.id.btnPlus -> handleOperatorInput("+")
                R.id.btnMinus -> handleOperatorInput("-")
                R.id.btnMultiply -> handleOperatorInput("×")
                R.id.btnDivide -> handleOperatorInput("÷")

                R.id.btnLeftBracket -> handleLeftBracket()
                R.id.btnRightBracket -> handleRightBracket()
                R.id.btnPercent -> handlePercent()

                R.id.btnEquals -> handleEquals()
                R.id.btnClear -> handleClear()
                R.id.btnBackspace -> handleBackspace()
                R.id.btnDecimal -> handleDecimalInput()
                R.id.btnPlusMinus -> handlePlusMinus()
            }
        } catch (e: Exception) {
            updateDisplay("Error")
        }
    }

    private fun handleNumberInput(digit: String) {
        try {
            val currentDigits = currentNumber.replace(".", "").replace("-", "").replace(",", "")
            if (currentDigits.length >= 12 && !lastWasOperator && !lastWasEquals) {
                return
            }

            // 检查右括号后自动插入乘号
            if (fullExpression.isNotEmpty() && fullExpression.last() == ')') {
                fullExpression += " × "
                lastWasOperator = true
            }

            if (lastWasEquals) {
                // 等号后输入数字，开始新计算
                fullExpression = digit
                currentNumber = digit
                lastWasEquals = false
                lastWasOperator = false
                clearLastOperation() // 清空上次运算记录
            } else if (lastWasOperator) {
                // 运算符后输入数字
                fullExpression += digit
                currentNumber = digit
                lastWasOperator = false
            } else {
                // 继续输入数字
                if (currentNumber == "0" && digit != ".") {
                    // 处理前导0的替换
                    if (fullExpression == "0") {
                        // 如果整个表达式就是"0"，替换为新数字
                        currentNumber = digit
                        fullExpression = digit
                    } else if (fullExpression.endsWith(" 0")) {
                        // 表达式末尾是" 0"（运算符后的0），替换末尾的0
                        currentNumber = digit
                        fullExpression = fullExpression.dropLast(1) + digit
                    } else {
                        // 其他情况，正常追加
                        currentNumber += digit
                        fullExpression += digit
                    }
                } else if (currentNumber == "-0" && digit != ".") {
                    if (digit == "0") {
                        // 特殊情况：-0 + 0 = 0（去除负号）
                        currentNumber = "0"
                        if (fullExpression == "-0") {
                            fullExpression = "0"
                        } else if (fullExpression.endsWith("-0")) {
                            fullExpression = fullExpression.dropLast(2) + "0"
                        } else {
                            fullExpression += digit
                        }
                        Log.d("Calculator", "数字输入: -0 + 0 = 0 (去除负号)")
                    } else {
                        // 特殊处理：-0后输入数字应该变成-数字
                        currentNumber = "-$digit"
                        if (fullExpression == "-0") {
                            fullExpression = "-$digit"
                        } else if (fullExpression.endsWith("-0")) {
                            fullExpression = fullExpression.dropLast(2) + "-$digit"
                        } else {
                            fullExpression += digit
                        }
                        Log.d("Calculator", "数字输入: -0 + $digit = $currentNumber")
                    }
                } else {
                    // 正常情况：追加数字
                    currentNumber += digit
                    fullExpression += digit
                }
            }

            // 显示逻辑优化：-0显示为0
            updateDisplay(currentNumber)
            updateProcessDisplay(fullExpression)
        } catch (e: Exception) {
            updateDisplay("Error")
        }
    }

    private fun handleOperatorInput(operator: String) {
        try {
            if (operator == "-" &&
                (fullExpression.isEmpty() ||
                        fullExpression.endsWith("(") ||
                        fullExpression.endsWith("+") ||
                        fullExpression.endsWith("-") ||
                        fullExpression.endsWith("×") ||
                        fullExpression.endsWith("÷")) &&
                currentNumber == "0"
            ) {
                // 此处为 负号
                currentNumber = "-"
                fullExpression += "-"
                lastWasOperator = false
                updateDisplay(currentNumber)
                updateProcessDisplay(fullExpression)
                return
            }

            // 如果表达式为空且直接输入运算符（除了减号），自动添加0
            if (fullExpression.isEmpty() && operator != "-") {
                Log.d("Calculator", "UX优化: 表达式为空时输入运算符'$operator'，自动添加前导0")
                fullExpression = "0 $operator "
                currentNumber = "0"
                lastWasOperator = true
                lastWasEquals = false

                updateDisplay(currentNumber)
                updateProcessDisplay(fullExpression)
                return
            }

            // 如果当前只有"0"且输入运算符，保持为"0 运算符 "
            if (fullExpression == "0" && currentNumber == "0" && operator != "-") {
                Log.d("Calculator", "UX优化: 初始状态0后输入运算符'$operator'")
                fullExpression = "0 $operator "
                lastWasOperator = true
                lastWasEquals = false

                updateDisplay(currentNumber)
                updateProcessDisplay(fullExpression)
                return
            }

            if (lastWasEquals) {
                // 等号后输入运算符，继续计算
                fullExpression = currentNumber + " $operator "
                lastWasEquals = false
                clearLastOperation() //清空上次运算记录
            } else if (lastWasOperator) {
                // 替换运算符
                fullExpression =
                    fullExpression.trimEnd().dropLastWhile { it != ' ' }
                        .trimEnd() + " $operator "
            } else {
                // 正常添加运算符
                fullExpression += " $operator "
            }

            // 运算符后重置currentNumber
            if (!lastWasEquals) {
                currentNumber = "0"
            }
            lastWasOperator = true

            Log.d(
                "Calculator",
                "运算符输入后 - 表达式: '$fullExpression', currentNumber: '$currentNumber'"
            )

            updateProcessDisplay(fullExpression)
        } catch (e: Exception) {
            updateDisplay("Error")
        }
    }

    private fun handleLeftBracket() {
        try {
            if (lastWasEquals) {
                fullExpression = "("
                lastWasEquals = false
                clearLastOperation() // 清空上次运算记录
            } else if (fullExpression.isEmpty()) {
                // UX优化：空表达式时直接输入左括号
                fullExpression = "("
                currentNumber = ""
            } else if (!lastWasOperator && fullExpression.isNotEmpty() &&
                !fullExpression.endsWith("(") && !fullExpression.endsWith(" ")
            ) {
                // 数字后加左括号，自动添加乘号
                fullExpression += " × ("
            } else {
                fullExpression += "("
            }

            currentNumber = ""
            lastWasOperator = true
            updateProcessDisplay(fullExpression)
        } catch (e: Exception) {
            updateDisplay("Error")
        }
    }

    private fun clearLastOperation() {
        lastOperator = ""
        lastOperand = 0.0
        lastOperation = ""
    }

    private fun handleRightBracket() {
        try {
            // 计算括号数量
            val openCount = fullExpression.count { it == '(' }
            val closeCount = fullExpression.count { it == ')' }

            // 条件检查
            val canAddRightBracket = openCount > closeCount && // 有未匹配的左括号
                    fullExpression.isNotEmpty() && // 表达式非空
                    !fullExpression.endsWith("(") && // 前项不是 (
                    !fullExpression.endsWith(" ") && // 不是空格结尾
                    (fullExpression.last()
                        .isDigit() || fullExpression.last() == ')')// 最后一个字符是 数字,允许右括号再加右括号

            if (canAddRightBracket) {
                fullExpression += ")"
                lastWasOperator = false
                updateProcessDisplay(fullExpression)
            } else {
                // 完全忽略，不做任何操作
            }
        } catch (e: Exception) {
            updateDisplay("Error")
        }
    }

    private fun handlePercent() {
        try {
            if (currentNumber.isNotEmpty() && currentNumber != "0") {
                // 移除逗号后计算
                val cleanNumber = currentNumber.replace(",", "")
                val value = cleanNumber.toDoubleOrNull()
                if (value != null) {

                    Log.d("Calculator", "=== 百分号计算开始 ===")
                    Log.d("Calculator", "当前表达式: '$fullExpression'")
                    Log.d("Calculator", "当前数字: '$currentNumber'")
                    Log.d("Calculator", "百分号值: $value%")

                    // 详细判断逻辑，找出为什么没进入复杂表达式分支
                    val containsMultiply = fullExpression.contains("×")
                    val containsDivide = fullExpression.contains("÷")
                    val containsPlus = fullExpression.contains("+")
                    val containsMinus = fullExpression.contains("-")
                    val endsWithCurrentNumber = fullExpression.endsWith(currentNumber)

                    Log.d("Calculator", "判断条件检查:")
                    Log.d("Calculator", "  包含×: $containsMultiply")
                    Log.d("Calculator", "  包含÷: $containsDivide")
                    Log.d("Calculator", "  包含+: $containsPlus")
                    Log.d("Calculator", "  包含-: $containsMinus")
                    Log.d("Calculator", "  以当前数字结尾: $endsWithCurrentNumber")

                    // === 智能百分号逻辑 ===
                    val result = when {
                        // 情况1：单独的数字 + %（如 "50%" → 0.5）
                        fullExpression == currentNumber || fullExpression == "($currentNumber)" -> {
                            value / 100.0
                        }

                        // 情况2：乘/除法运算 + %（如 "200 ×/÷ 10%" → "200 × 0.1"）
                        (fullExpression.contains("×") || fullExpression.contains("÷"))
                                && fullExpression.endsWith(currentNumber)
                                && !fullExpression.contains("+") && !fullExpression.contains("-") -> {
                            value / 100.0
                        }

                        // 情况3：加/减法运算 + %（如 "200 +/- 10%" → "200 + (200 × 0.1)"）
                        (containsPlus || containsMinus) && endsWithCurrentNumber && !containsMultiply && !containsDivide -> {
                            Log.d("Calculator", "匹配情况3: 简单加减法百分号")
                            // 对于"100 + 200 - 5 %"这种情况，它包含多个运算符，不应该走简单分支

                            // 检查运算符数量
                            val operatorCount = fullExpression.count { it in "+-×÷" }
                            Log.d("Calculator", "  运算符数量: $operatorCount")

                            if (operatorCount == 1) {
                                // 真正的简单表达式
                                calculateSimplePercentage(value)
                            } else {
                                // 多个运算符，应该走复杂表达式逻辑
                                Log.d("Calculator", "  多运算符，转为复杂表达式处理")
                                calculateComplexPercentage(value)
                            }
                        }

                        // 情况4：复杂表达式，如 "200 × 50 + 10% -> 200 * 50 + (200 * 50) * 0.1"
                        else -> {
                            Log.d("Calculator", "情况4: 复杂表达式百分号")
                            calculateComplexPercentage(value)
                        }
                    }

                    // 格式化结果
                    val resultStr = formatNumber(result)
                    Log.d("Calculator", "百分号计算结果: $result -> '$resultStr'")

                    when {
                        // 情况1：括号包围的单个数字 "(90)" → "0.09"
                        fullExpression == "($currentNumber)" -> {
                            fullExpression = resultStr
                        }
                        // 情况2：表达式以当前数字结尾 "5 + 90" → "5 + 0.9"
                        fullExpression.endsWith(currentNumber) -> {
                            fullExpression =
                                fullExpression.dropLast(currentNumber.length) + resultStr
                        }
                        // 情况3：其他复杂情况，重置为百分号结果
                        else -> {
                            fullExpression = resultStr
                        }
                    }

                    currentNumber = resultStr
                    updateDisplay(resultStr) // 自动应用格式化
                    updateProcessDisplay(fullExpression)

                    Log.d("Calculator", "=== 百分号计算结束 ===")
                }
            }
        } catch (e: Exception) {
            updateDisplay("Error")
        }
    }

    private fun calculateComplexPercentage(value: Double): Double {
        val expressionWithoutCurrentNumber =
            fullExpression.dropLast(currentNumber.length).trimEnd()

        if (expressionWithoutCurrentNumber.isEmpty()) return value / 100.0

        val lastOperator = expressionWithoutCurrentNumber.lastOrNull()?.toString()

        Log.d("Calculator", "复杂百分号分析:")
        Log.d("Calculator", "  去掉当前数字后的表达式: '$expressionWithoutCurrentNumber'")
        Log.d("Calculator", "  最后运算符: '$lastOperator'")

        return when (lastOperator) {
            "+", "-" -> {
                // 对于加减法，需要计算前面完整表达式的值作为基数
                val expressionBeforeOperator = expressionWithoutCurrentNumber.dropLast(1).trim()

                Log.d("Calculator", "  运算符前的表达式: '$expressionBeforeOperator'")

                val baseValue = evaluateExpression(expressionBeforeOperator)

                if (baseValue != null) {
                    val percentageAmount = baseValue * (value / 100.0)
                    Log.d("Calculator", "  基数值: $baseValue")
                    Log.d("Calculator", "  百分比金额: $percentageAmount")
                    percentageAmount
                } else {
                    Log.w("Calculator", "  无法计算基数表达式，使用简单百分比")
                    value / 100.0
                }
            }

            "×", "÷" -> {
                Log.d("Calculator", "  乘除法运算，直接转换为小数")
                value / 100.0
            }

            else -> {
                Log.d("Calculator", "  默认情况，直接转换为小数")
                value / 100.0
            }
        }
    }

    private fun calculateSimplePercentage(value: Double): Double {
        val baseValue = when {
            fullExpression.contains("+") -> {
                val parts = fullExpression.split("+")
                if (parts.size == 2) parts[0].trim().replace(",", "").toDoubleOrNull() else null
            }

            fullExpression.contains("-") -> {
                val parts = fullExpression.split("-")
                if (parts.size == 2) parts[0].trim().replace(",", "").toDoubleOrNull() else null
            }

            else -> null
        }
        return baseValue?.let { it * (value / 100.0) } ?: (value / 100.0)
    }

    private fun formatNumber(value: Double): String {
        return if (value == value.toInt().toDouble()) {
            value.toInt().toString()
        } else {
            String.format("%.10f", value).trimEnd('0').trimEnd('.')
        }
    }

    private fun handleEquals() {
        try {
            // UX优化：如果表达式为空，直接按等号显示当前数字
            if (fullExpression.isEmpty()) {
                Log.d("Calculator", "UX优化: 表达式为空时按等号，显示当前数字")
                val displayValue = if (currentNumber.isEmpty()) "0" else currentNumber
                updateDisplay(displayValue)
                updateProcessDisplay("$displayValue =")
                return
            }

            when {
                // 连续按等号的情况
                lastWasEquals && lastOperation.isNotEmpty() -> {
                    Log.d("Calculator", "Repeating last operation: $lastOperation")
                    repeatLastOperation()
                }

                // 首次按等号 - 添加括号自动补全
                fullExpression.isNotEmpty() -> {
                    Log.d("Calculator", "First equals press, evaluating: $fullExpression")

                    // 自动补全缺失的右括号
                    val originalExpression = fullExpression
                    val completedExpression = autoCompleteParentheses(fullExpression)
                    if (completedExpression != originalExpression) {
                        Log.d(
                            "Calculator",
                            "自动补全括号: '$originalExpression' -> '$completedExpression'"
                        )
                        fullExpression = completedExpression
                        updateProcessDisplay(fullExpression)
                    }

                    performInitialCalculation()
                }
            }
        } catch (e: Exception) {
            updateDisplay("Error")
        }
    }

    private fun autoCompleteParentheses(expression: String): String {
        try {
            val openCount = expression.count { it == '(' }
            val closeCount = expression.count { it == ')' }

            Log.d("Calculator", "括号分析: 左括号=$openCount, 右括号=$closeCount")

            return if (openCount > closeCount) {
                // 有未闭合的左括号，自动添加右括号
                val missingClose = openCount - closeCount
                val completed = expression + ")".repeat(missingClose)
                Log.d("Calculator", "自动添加 $missingClose 个右括号")
                completed
            } else {
                // 括号已平衡或没有括号
                expression
            }
        } catch (e: Exception) {
            Log.e("Calculator", "括号补全失败", e)
            return expression
        }
    }

    private fun performInitialCalculation() {
        try {
            val result = calculateExpression(fullExpression)
            if (result != null) {
                // 记录本次运算信息，用于连续等号
                recordLastOperation()

                val resultStr = formatNumber(result)
                val originalExpression = fullExpression

                updateProcessDisplay("$fullExpression =")
                updateDisplay(resultStr)
                addToHistory("$fullExpression = ${formatWithCommas(resultStr)}")

                fullExpression = resultStr
                currentNumber = resultStr
                lastWasEquals = true
                lastWasOperator = false

                Log.d("Calculator", "Calculation result: $originalExpression = $resultStr")
            } else {
                updateDisplay("Error")
            }
        } catch (e: Exception) {
            Log.e("Calculator", "计算过程发生异常")
            updateDisplay("Error")
        }
    }

    private fun repeatLastOperation() {
        val currentValue = currentNumber.toDoubleOrNull() ?: return

        val newResult = when (lastOperator) {
            "+" -> currentValue + lastOperand
            "-" -> currentValue - lastOperand
            "×" -> currentValue * lastOperand
            "÷" -> {
                if (lastOperand != 0.0) {
                    currentValue / lastOperand
                } else {
                    updateDisplay("Error")
                    return
                }
            }

            else -> currentValue
        }

        val resultStr = formatNumber(newResult)
        val operationStr = "$currentValue $lastOperator ${formatNumber(lastOperand)}"

        currentNumber = resultStr
        fullExpression = resultStr

        updateDisplay(resultStr)
        updateProcessDisplay("$operationStr =")
        addToHistory("$operationStr = ${formatWithCommas(resultStr)}")

        Log.d("Calculator", "Repeated operation: $operationStr = $resultStr")
    }

    private fun recordLastOperation() {
        // 从当前表达式中提取最后的运算
        val operators = listOf(" + ", " - ", " × ", " ÷ ")
        var lastOpIndex = -1
        var foundOperator = ""

        for (op in operators) {
            val index = fullExpression.lastIndexOf(op)
            if (index > lastOpIndex) {
                lastOpIndex = index
                foundOperator = op.trim()
            }
        }

        if (lastOpIndex > -1) {
            lastOperator = foundOperator
            val operandStr = fullExpression.substring(lastOpIndex + 3).trim()
            lastOperand = operandStr.replace(",", "").toDoubleOrNull() ?: 0.0
            lastOperation = "$foundOperator $operandStr"

            Log.d(
                "Calculator",
                "Recorded last operation: operator='$lastOperator', operand=$lastOperand"
            )
        } else {
            clearLastOperation()
        }
    }

    private fun handleClear() {
        try {
            fullExpression = ""
            currentNumber = "0" // 确保清楚后显示0而不是空
            lastWasEquals = false
            lastWasOperator = false
            clearLastOperation() // 清空上次运算记录
            updateDisplay("0")
            updateProcessDisplay("")

            Log.d("Calculator", "清除后状态 - 表达式: '$fullExpression', 数字: '$currentNumber'")
        } catch (e: Exception) {
            updateDisplay("0")
        }
    }

    private fun handleBackspace() {
        try {
            if (fullExpression.isEmpty()) {
                currentNumber = "0"
                updateDisplay("0")
                return
            }

            if (lastWasEquals) {
                // 等号后的退格，清除所有内容重新开始
                handleClear()
                return
            }

            val lastChar = fullExpression.last()

            when {
                lastChar.isDigit() || lastChar == '.' -> {
                    Log.d(
                        "Calculator",
                        "Before delete digit: fullExpression='$fullExpression', currentNumber='$currentNumber'"
                    )
                    fullExpression = fullExpression.dropLast(1)

                    if (currentNumber.length > 1) {
                        currentNumber = currentNumber.dropLast(1)
                        Log.d(
                            "Calculator",
                            "Multi-digit case: new currentNumber='$currentNumber'"
                        )
                    } else {
                        Log.d(
                            "Calculator",
                            "Single digit case: fullExpression after dropLast='$fullExpression'"
                        )

                        if (fullExpression.isNotEmpty() && fullExpression.endsWith(" ")) {
                            Log.d(
                                "Calculator",
                                "Found space at end, entering friendly mode"
                            )
                            fullExpression = fullExpression.trimEnd()
                            Log.d(
                                "Calculator",
                                "After trimEnd: fullExpression='$fullExpression'"
                            )
                            val parts = fullExpression.split(" ")
                            Log.d("Calculator", "Split parts: $parts")
                            if (parts.isNotEmpty() && parts.last() in "+-×÷") {
                                if (parts.size >= 2) {
                                    currentNumber = parts[parts.size - 2]
                                    Log.d(
                                        "Calculator",
                                        "Found previous number: '$currentNumber'"
                                    )
                                } else {
                                    currentNumber = "0"
                                }
                            } else {
                                currentNumber = "0"
                            }
                        } else {
                            currentNumber = "0"
                            if (fullExpression.isEmpty()) {
                                lastWasOperator = false
                            }
                        }
                    }
                    lastWasOperator = false
                }

                lastChar == ')' -> {
                    // 删除右括号
                    fullExpression = fullExpression.dropLast(1)
                    lastWasOperator = false
                    rebuildCurrentNumber()
                }

                lastChar == '(' -> {
                    // 删除左括号
                    fullExpression = fullExpression.dropLast(1)

                    // 检查是否需要删除自动添加的乘号
                    if (fullExpression.endsWith(" × ")) {
                        fullExpression = fullExpression.dropLast(3)
                    }

                    lastWasOperator = false
                    rebuildCurrentNumber()
                }

                // 1. 先处理运算符删除
                lastChar in "+-×÷" -> {
                    // 删除运算符,还需要删除前面的空格
                    fullExpression = fullExpression.dropLast(1).trimEnd()
                    lastWasOperator = false
                    rebuildCurrentNumber()
                }

                // 2. 再处理空格删除
                lastChar == ' ' -> {
                    // 删除空格后，检查是否还有运算符需要删除
                    fullExpression = fullExpression.trimEnd()
                    if (fullExpression.isNotEmpty() && fullExpression.last() in "+-×÷") {
                        // 继续删除运算符
                        fullExpression = fullExpression.dropLast(1).trimEnd()
                    }
                    lastWasOperator = false
                    rebuildCurrentNumber()
                }
            }
            updateDisplay(currentNumber)
            updateProcessDisplay(fullExpression)
        } catch (e: Exception) {
            updateDisplay("Error")
        }
    }

    // 重新构建当前数字
    private fun rebuildCurrentNumber() {
        try {
            if (fullExpression.isEmpty()) {
                currentNumber = "0"
                return
            }

            // 向前查找数字的开始位置
            val endIndex = fullExpression.length
            var startIndex = endIndex - 1

            // 使用 startIndex 从后向前查找
            while (startIndex >= 0) {
                val char = fullExpression[startIndex]  // 使用正确的索引变量
                if (char.isDigit() || char == '.' ||
                    (char == '-' && startIndex == 0) ||  // 处理表达式开头的负号
                    (char == '-' && fullExpression[startIndex - 1] in " (")
                ) {  // 处理括号后的负号
                    startIndex--
                } else {
                    break
                }
            }

            // 提取数字
            currentNumber = if (startIndex + 1 < endIndex) {
                fullExpression.substring(startIndex + 1, endIndex)
            } else {
                "0"
            }

            // 确保currentNumber不为空
            if (currentNumber.isEmpty() || currentNumber == "-") {
                currentNumber = "0"
            }
        } catch (e: Exception) {
            currentNumber = "0"
        }
    }

    private fun handleDecimalInput() {
        try {
            if (!currentNumber.contains(".")) {
                if (lastWasOperator || fullExpression.isEmpty()) {
                    // 运算符后或空表达式时输入小数点，自动添加"0."
                    currentNumber = "0."
                    if (fullExpression.isEmpty()) {
                        fullExpression = "0."
                    } else {
                        fullExpression += "0."
                    }
                    lastWasOperator = false
                } else if (currentNumber.isEmpty() || currentNumber == "0") {
                    // 当前数字为空或为0时，输入小数点变成"0."
                    currentNumber = "0."
                    if (fullExpression.endsWith("0")) {
                        fullExpression = fullExpression.dropLast(1) + "0."
                    } else {
                        fullExpression += "0."
                    }
                } else if (currentNumber == "-0") {
                    // 特殊处理：-0后输入小数点应该变成-0.
                    currentNumber = "-0."
                    if (fullExpression == "-0") {
                        fullExpression = "-0."
                    } else if (fullExpression.endsWith("-0")) {
                        fullExpression = fullExpression.dropLast(2) + "-0."
                    } else {
                        fullExpression += "."
                    }
                    Log.d("Calculator", "小数点输入: -0 + . = -0.")
                } else {
                    // 正常情况：在当前数字后添加小数点
                    currentNumber += "."
                    fullExpression += "."
                }
                updateDisplay(currentNumber)
                updateProcessDisplay(fullExpression)
            }
        } catch (e: Exception) {
            updateDisplay("Error")
        }
    }

    private fun handlePlusMinus() {
        try {
            when {
                // 情况1: 初始状态按正负号
                fullExpression.isEmpty() || (fullExpression == "0" && currentNumber == "0") -> {
                    Log.d("Calculator", "情况1: 初始状态正负号")

                    if (currentNumber == "0") {
                        // 第一次按：0 -> -0
                        currentNumber = "-0" // 下
                        fullExpression = "-0" // 上
                        Log.d("Calculator", "  第一次按+/-: 显示-0")
                    } else if (currentNumber == "-0") {
                        // 第二次按：-0 -> 0
                        currentNumber = "0"
                        fullExpression = "0"
                        Log.d("Calculator", "  第二次按+/-: 显示0")
                    } else {
                        // 其他初始状态
                        currentNumber = "-0"
                        fullExpression = "-0"
                        Log.d("Calculator", "  初始化为-0")
                    }
                    lastWasOperator = false
                    lastWasEquals = false
                }

                // 情况2: 等号后按正负号
                lastWasEquals -> {
                    Log.d("Calculator", "+/- after equals, toggling result sign")
                    toggleCurrentNumberSign()
                    fullExpression = currentNumber
                    lastWasEquals = false
                    clearLastOperation()
                }

                // 情况3: 运算符后按正负号（应该开始输入负数）
                lastWasOperator -> {
                    Log.d("Calculator", "情况3: 运算符后正负号")

                    Log.d("Calculator", "  检查当前数字: '$currentNumber'")

                    if (currentNumber == "0" || currentNumber.isEmpty()) {
                        // 开始输入负数
                        currentNumber = "-"
                        fullExpression += "-"
                        lastWasOperator = false // 重要：现在开始输入数字了
                        Log.d("Calculator", "  开始输入负数，表达式变为: '$fullExpression'")
                    } else if (currentNumber == "-") {
                        // 已经是负号，变成正数（移除负号）
                        currentNumber = "0"
                        fullExpression = fullExpression.dropLast(1)
                        Log.d("Calculator", "  取消负号输入")
                    } else {
                        // 运算符后currentNumber不应该有值
                        // 如果有值，说明逻辑有问题
                        Log.d(
                            "Calculator",
                            "  警告：运算符后currentNumber不应该有值: '$currentNumber'"
                        )
                        Log.d("Calculator", "  强制开始负数输入")
                        currentNumber = "-"
                        fullExpression += "-"
                        lastWasOperator = false
                    }
                }

                // 情况4: 正在输入数字时按正负号
                true -> {
                    Log.d("Calculator", "情况4: 输入数字时正负号")

                    // 特殊处理：如果当前是"0"或"-0"
                    if (currentNumber == "0") {
                        currentNumber = "-0"
                        fullExpression = if (fullExpression == "0") "-0" else {
                            fullExpression.dropLast(1) + "-0"
                        }
                        Log.d("Calculator", "  0变为-0")
                    } else if (currentNumber == "-0") {
                        currentNumber = "0"
                        fullExpression = if (fullExpression == "-0") "0" else {
                            fullExpression.dropLast(2) + "0"
                        }
                        Log.d("Calculator", "  -0变为0")
                    } else {
                        // 正常数字的符号切换
                        val oldCurrentNumber = currentNumber
                        toggleCurrentNumberSign()

                        // 更新表达式中的数字
                        if (fullExpression.endsWith(oldCurrentNumber)) {
                            fullExpression =
                                fullExpression.dropLast(oldCurrentNumber.length) + currentNumber
                            Log.d(
                                "Calculator",
                                "  更新表达式中的数字: '$oldCurrentNumber' -> '$currentNumber'"
                            )
                        }
                    }
                }
            }

            updateDisplay(currentNumber)
            updateProcessDisplay(fullExpression)
        } catch (e: Exception) {
            updateDisplay("Error")
        }
    }

    private fun toggleCurrentNumberSign() {
        currentNumber = if (currentNumber.startsWith("-")) {
            currentNumber.substring(1)
        } else {
            "-$currentNumber"
        }
    }

    // 计算函数，更好地处理复杂表达式
    private fun calculateExpression(expr: String): Double? {
        return try {
            if (expr.isBlank()) {
                Log.w("Calculator", "表达式为空")
                return null
            }

            val cleanExpr = expr.replace("×", "*").replace("÷", "/")
            Log.d("Calculator", "标准化后表达式: '$cleanExpr'")

            val result = evaluateExpression(cleanExpr)
            Log.d("Calculator", "计算结果: $result")
            result
        } catch (e: ArithmeticException) {
            Log.e("Calculator", "算术错误: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e("Calculator", "表达式计算错误", e)
            null
        }
    }

    // 检查表达式有效性
    private fun isValidExpression(expression: String): Boolean {
        try {
            // 基本检查
            if (expression.isBlank()) return false

            // 检查括号平衡
            val openCount = expression.count { it == '(' }
            val closeCount = expression.count { it == ')' }

            // 允许左括号多于右括号（会自动补全）
            if (closeCount > openCount) {
                Log.w("Calculator", "右括号过多")
                return false
            }

            // 检查是否以运算符结尾（应该允许）
            val lastChar = expression.trimEnd().lastOrNull()
            if (lastChar != null && lastChar in "+-×÷") {
                Log.d("Calculator", "表达式以运算符结尾，这是允许的")
            }

            return true
        } catch (e: Exception) {
            Log.e("Calculator", "表达式验证错误", e)
            return false
        }
    }

    // 递归计算表达式
    private fun evaluateExpression(expr: String): Double? {
        return try {
            if (expr.isBlank()) return null

            // 移除所有逗号和空格
            val cleanExpression = expr.replace(",", "").replace(" ", "")

            // 如果是单个数字，直接返回
            val singleNumber = cleanExpression.toDoubleOrNull()
            if (singleNumber != null) return singleNumber

            // 调用表达式计算函数
            val result = evaluateExpressionWithParentheses(cleanExpression)
            result
        } catch (e: Exception) {
            null
        }
    }

    private fun evaluateExpressionWithParentheses(expr: String): Double {
        var expression = expr.trim()

        // 处理括号
        while (expression.contains("(")) {
            val start = expression.lastIndexOf("(")
            val end = expression.indexOf(")", start)

            if (end == -1) {
                // 这种情况不应该发生，因为已经在handleEquals中补全了括号
                Log.w("Calculator", "发现未闭合括号，尝试补全")
                expression = autoCompleteParentheses(expression)
                continue
            }

            val subExpr = expression.substring(start + 1, end)
            val result = evaluateSimpleExpression(subExpr)
            expression = expression.substring(0, start) + result + expression.substring(end + 1)
        }
        val finalResult = evaluateSimpleExpression(expression)
        Log.d("Calculator", "最终计算结果: $finalResult")

        return finalResult
    }

    // 简单表达式计算
    private fun evaluateSimpleExpression(expr: String): Double {
        val tokens = tokenizeExpression(expr.trim())
        if (tokens.isEmpty()) return 0.0
        if (tokens.size == 1) return tokens[0].toDouble()

        // 创建可变列表进行计算
        val values = mutableListOf<Double>()
        val operators = mutableListOf<String>()

        // 解析表达式
        var i = 0
        while (i < tokens.size) {
            if (i % 2 == 0) {
                // 期望数字
                values.add(tokens[i].toDouble())
            } else {
                // 期望运算符
                operators.add(tokens[i])
            }
            i++
        }

        // 先处理乘除（从左到右）
        i = 0
        while (i < operators.size) {
            if (operators[i] == "*" || operators[i] == "/") {
                val left = values[i]
                val right = values[i + 1]

                if (operators[i] == "/" && right == 0.0) {
                    throw ArithmeticException("Division by zero")
                }
                val result = if (operators[i] == "*") left * right else left / right

                values[i] = result
                values.removeAt(i + 1)
                operators.removeAt(i)
            } else {
                i++
            }
        }

        // 再处理加减（从左到右）
        i = 0
        while (i < operators.size) {
            val left = values[i]
            val right = values[i + 1]
            val result = if (operators[i] == "+") left + right else left - right

            values[i] = result
            values.removeAt(i + 1)
            operators.removeAt(i)
        }

        return values[0]
    }

    // 改进的表达式分解
    private fun tokenizeExpression(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var current = ""
        var i = 0

        while (i < expr.length) {
            val char = expr[i]
            when (char) {
                ' ' -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current)
                        current = ""
                    }
                }

                '+', '*', '/', '×', '÷' -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current)
                        current = ""
                    }
                    // 标准化运算符 - 统一转换为String
                    val normalizedOperator = when (char) {
                        '×' -> "*"
                        '÷' -> "/"
                        else -> char.toString()
                    }
                    tokens.add(normalizedOperator)
                }

                '-' -> {
                    // 处理负号和减号
                    if (current.isNotEmpty()) {
                        // 这是减号
                        tokens.add(current)
                        current = ""
                        tokens.add("-")
                    } else if (tokens.isEmpty() || tokens.last() in listOf(
                            "+",
                            "-",
                            "*",
                            "/"
                        )
                    ) {
                        // 这是负号
                        current = "-"
                    } else {
                        // 这是减号
                        tokens.add("-")
                    }
                }

                else -> current += char
            }
            i++
        }

        if (current.isNotEmpty()) {
            tokens.add(current)
        }

        return tokens
    }

    private fun updateDisplay(value: String) {
        try {
            val formattedValue = formatWithCommas(value)
            tvDisplay.text = formattedValue
            adjustTextSize(tvDisplay, formattedValue)

            Log.d("Calculator", "下方显示更新为: '$formattedValue'")
        } catch (e: Exception) {
            tvDisplay.text = value
        }
    }

    // 添加文字大小自动调整
    private fun adjustTextSize(textView: TextView, text: String) {
        val maxTextSize = 42f
        val minTextSize = 24f

        when {
            text.length <= 8 -> textView.textSize = maxTextSize
            text.length <= 12 -> textView.textSize = 36f
            text.length <= 16 -> textView.textSize = 30f
            text.length <= 20 -> textView.textSize = 26f
            else -> textView.textSize = minTextSize
        }
    }

    // 三位切分格式化函数
    private fun formatWithCommas(value: String): String {
        return try {
            if (value == "Error" || value.contains("(") || value.contains(")")) {
                return value
            }

            // 特殊处理-0和-0.的情况
            if (value == "-0" || value == "-0." || value.startsWith("-0.")) {
                return value  // 直接返回，不进行数字格式化处理
            }

            val cleanValue = value.replace(",", "")
            val number = value.replace(",", "").toDoubleOrNull() ?: return value

            if (cleanValue.contains(".")) {
                // 处理小数
                val parts = cleanValue.replace(",", "").split(".")
                if (parts.size == 2) {
                    val intPart = parts[0].toLongOrNull() ?: return value
                    val decimalPart = parts[1]
                    return java.text.NumberFormat.getInstance()
                        .format(intPart) + "." + decimalPart
                }
            } else {
                // 处理整数(包括负数)
                val longValue = number.toLong()
                return java.text.NumberFormat.getInstance().format(longValue)
            }
            return value
        } catch (e: Exception) {
            value
        }
    }

    private fun updateProcessDisplay(processText: String) {
        try {
            tvProcess.text = processText
            Log.d("Calculator", "上方显示更新为: '$processText'")
        } catch (e: Exception) {
            // 过程显示更新失败时不崩溃
        }
    }

    // 履歴功能
    private fun addToHistory(calculation: String) {
        try {
            calculationHistory.add(0, calculation)
            if (calculationHistory.size > 100) {
                calculationHistory.removeAt(calculationHistory.size - 1)
            }
            saveHistory()
        } catch (e: Exception) {
            // 履历保存失败时不崩溃
        }
    }

    private fun saveHistory() {
        try {
            val editor = sharedPrefs.edit()
            val historyString = calculationHistory.joinToString("|||")
            editor.putString("history_v2", historyString)
            editor.apply()
        } catch (e: Exception) {
            // 保存失败时不崩溃
        }
    }

    private fun loadHistory() {
        try {
            calculationHistory.clear()
            val historyString = sharedPrefs.getString("history_v2", "")
            if (!historyString.isNullOrEmpty()) {
                calculationHistory.addAll(historyString.split("|||").filter { it.isNotEmpty() })
            }
        } catch (e: Exception) {
            calculationHistory.clear()
        }
    }

    private fun showHistoryDialog() {
        try {
            if (calculationHistory.isEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("履歴")
                    .setMessage("履歴がありません")
                    .setPositiveButton("OK", null)
                    .show()
                return
            }

            val historyArray = calculationHistory.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("計算履歴")
                .setItems(historyArray) { _, which ->
                    try {
                        val selectedHistory = historyArray[which]
                        val result = selectedHistory.substringAfterLast("= ")
                        updateDisplay(result)
                        currentNumber = result
                        fullExpression = result
                        lastWasEquals = true
                        clearLastOperation() // 清空上次运算记录
                    } catch (e: Exception) {
                        updateDisplay("Error")
                    }
                }
                .setNegativeButton("履歴クリア") { _, _ ->
                    clearHistory()
                }
                .setNeutralButton("閉じる", null)
                .show()
        } catch (e: Exception) {
            // 如果历史记录对话框失败，显示简单提示
            try {
                AlertDialog.Builder(this)
                    .setTitle("履歴")
                    .setMessage("履歴の表示に問題があります")
                    .setPositiveButton("OK", null)
                    .show()
            } catch (e2: Exception) {
                // 连简单对话框都失败就忽略
            }
        }
    }

    private fun clearHistory() {
        try {
            calculationHistory.clear()
            saveHistory()
        } catch (e: Exception) {
            // 清理失败时不崩溃
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            saveHistory()
        } catch (e: Exception) {
            // 保存失败时不崩溃
        }
    }
}