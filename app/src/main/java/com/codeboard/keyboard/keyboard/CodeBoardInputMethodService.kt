package com.codeboard.keyboard.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import com.codeboard.keyboard.R

data class CodeCategory(
    val title: String,
    val elements: List<String>
)

enum class KeyboardViewMode {
    QWERTY, SYMBOLS, CODE_PALETTE
}

class CodeBoardInputMethodService : InputMethodService() {

    private lateinit var keyboardView: View
    private lateinit var suggestionContainer: LinearLayout
    private lateinit var keysContainer: LinearLayout

    private var currentMode = KeyboardViewMode.CODE_PALETTE
    private var selectedLanguage = "Luau"
    private var selectedCategoryIndex = 0

    // Database including Luau (Roblox) and standard languages
    private val languageDatabase = mapOf(
        "Luau" to listOf(
            CodeCategory("Roblox Basics", listOf("local ", "function ", "game:GetService(\"", "Instance.new(\"", "workspace.", "script.Parent", "task.wait(", "task.spawn(", "warn(", "print(")),
            CodeCategory("Control & Flow", listOf("if ", "then", "else", "elseif ", "end", "for ", "in ", "pairs(", "ipairs(", "while ", "do", "repeat", "until ", "break", "return ")),
            CodeCategory("Data & Math", listOf("Vector3.new(", "CFrame.new(", "Color3.fromRGB(", "UDim2.new(", "BrickColor.new(", "math.clamp(", "math.rad(", "math.random(")),
            CodeCategory("Events & OOP", listOf(":Connect(function(", ":Destroy()", ":Clone()", ":WaitForChild(\"", ":FindFirstChild(\"", ":GetChildren()", "setmetatable(")),
            CodeCategory("Snippets", listOf("local Players = game:GetService(\"Players\")\n", "script.Parent.Touched:Connect(function(hit)\n    \nend)", "task.spawn(function()\n    \nend)"))
        ),
        "Python" to listOf(
            CodeCategory("Keywords", listOf("def ", "class ", "return ", "import ", "from ", "as ", "pass", "lambda ", "global ", "yield ", "with ")),
            CodeCategory("Control Flow", listOf("if ", "elif ", "else:", "for ", "in ", "while ", "break", "continue", "try:", "except ")),
            CodeCategory("Built-ins", listOf("print(", "len(", "range(", "type(", "int(", "str(", "float(", "list(", "dict(", "set(", "enumerate(")),
            CodeCategory("Methods", listOf(".append(", ".extend(", ".pop(", ".split(", ".replace(", ".join(", ".strip(", ".get("))
        ),
        "JavaScript" to listOf(
            CodeCategory("Declarations", listOf("const ", "let ", "var ", "function ", "=> ", "class ", "extends ", "this.")),
            CodeCategory("Control Flow", listOf("if ", "else ", "switch ", "case ", "for ", "while ", "try ", "catch ")),
            CodeCategory("DOM & Console", listOf("console.log(", "document.getElementById(", "document.querySelector(", "addEventListener(")),
            CodeCategory("Array & Object", listOf(".map(", ".filter(", ".reduce(", ".forEach(", "JSON.stringify(", "JSON.parse("))
        ),
        "TypeScript" to listOf(
            CodeCategory("Type Defs", listOf("type ", "interface ", "enum ", "as ", "keyof ", "typeof ", "unknown", "never")),
            CodeCategory("Generics", listOf("readonly ", "private ", "public ", "protected ", "<T>", "Record<", "Partial<"))
        ),
        "Kotlin" to listOf(
            CodeCategory("Declarations", listOf("val ", "var ", "fun ", "class ", "data class ", "object ", "interface ")),
            CodeCategory("Control & Null", listOf("when ", "if ", "else ", "for ", "while ", "try ", "catch ", "?:", "!!")),
            CodeCategory("Collections", listOf("listOf(", "mutableListOf(", "mapOf(", ".forEach { ", ".map { ", ".filter { "))
        ),
        "C++" to listOf(
            CodeCategory("Types & Lib", listOf("int ", "double ", "float ", "bool ", "auto ", "void ", "std::cout << ", "std::vector<")),
            CodeCategory("Pointers", listOf("new ", "delete ", "nullptr", "sizeof(", "&", "*", "unique_ptr<"))
        ),
        "C#" to listOf(
            CodeCategory("Declarations", listOf("public ", "private ", "static ", "class ", "using ", "var ", "async ", "await ")),
            CodeCategory("Control & IO", listOf("if ", "else ", "foreach ", "while ", "try ", "catch ", "Console.WriteLine("))
        ),
        "HTML" to listOf(
            CodeCategory("Elements", listOf("<div>", "</div>", "<span>", "</span>", "<p>", "</p>", "<a href=\"\">", "<img src=\"\">")),
            CodeCategory("Forms", listOf("<form>", "<input type=\"text\"", "<button>", "<select>", "<option>"))
        ),
        "CSS" to listOf(
            CodeCategory("Layout", listOf("display: flex;", "display: grid;", "position: absolute;", "justify-content: center;")),
            CodeCategory("Box & Style", listOf("width: ", "height: ", "margin: ", "padding: ", "color: ", "background-color: "))
        ),
        "SQL" to listOf(
            CodeCategory("Queries", listOf("SELECT ", "FROM ", "WHERE ", "INSERT INTO ", "UPDATE ", "DELETE ", "JOIN ", "GROUP BY "))
        )
    )

    override fun onCreateInputView(): View {
        keyboardView = LayoutInflater.from(this).inflate(R.layout.keyboard_view, null)
        suggestionContainer = keyboardView.findViewById(R.id.suggestion_container)
        keysContainer = keyboardView.findViewById(R.id.keys_container)

        loadSelectedLanguageFromApp()
        renderUI()
        return keyboardView
    }

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        loadSelectedLanguageFromApp()
        updateLiveSuggestions()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        updateLiveSuggestions()
    }

    // Uygulamanın 'Select Language' menüsünden kaydedilen dili okur
    private fun loadSelectedLanguageFromApp() {
        val prefs = getSharedPreferences("CodeBoardPrefs", Context.MODE_PRIVATE)
        val savedLang = prefs.getString("SELECTED_LANGUAGE", "Luau") ?: "Luau"
        if (languageDatabase.containsKey(savedLang)) {
            selectedLanguage = savedLang
        }
    }

    // Canlı Öneriler Çubuğu (Yazılan kelimeye göre anlık filtreler)
    private fun updateLiveSuggestions() {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(20, 0)?.toString() ?: ""
        val lastWord = textBefore.substringAfterLast(" ").substringAfterLast("\n")

        suggestionContainer.removeAllViews()

        if (lastWord.isEmpty()) {
            renderLanguageBarInSuggestions()
            return
        }

        val allElements = languageDatabase[selectedLanguage]?.flatMap { it.elements } ?: emptyList()
        val matches = allElements.filter { it.trim().startsWith(lastWord, ignoreCase = true) }.distinct()

        if (matches.isEmpty()) {
            renderLanguageBarInSuggestions()
            return
        }

        val suggestionScrollView = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val suggestionLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        matches.take(10).forEach { suggestion ->
            val btn = Button(this).apply {
                text = suggestion.trim()
                setTextColor(Color.BLACK)
                background = createDrawable("#00E676", "#00C853")
                textSize = 12f
                isAllCaps = false
                setOnClickListener {
                    ic.deleteSurroundingText(lastWord.length, 0)
                    ic.commitText(suggestion, 1)
                    updateLiveSuggestions()
                }
            }
            suggestionLayout.addView(btn)
        }
        suggestionScrollView.addView(suggestionLayout)
        suggestionContainer.addView(suggestionScrollView)
    }

    private fun renderLanguageBarInSuggestions() {
        suggestionContainer.removeAllViews()
        val langScrollView = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val langLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        languageDatabase.keys.forEach { lang ->
            val isSelected = lang == selectedLanguage
            val btn = Button(this).apply {
                text = lang
                setTextColor(if (isSelected) Color.BLACK else Color.WHITE)
                background = createDrawable(if (isSelected) "#00E676" else "#212121", "#00E676")
                textSize = 12f
                isAllCaps = false
                setOnClickListener {
                    selectedLanguage = lang
                    selectedCategoryIndex = 0
                    
                    // Tercihi hafızaya yaz
                    getSharedPreferences("CodeBoardPrefs", Context.MODE_PRIVATE)
                        .edit().putString("SELECTED_LANGUAGE", lang).apply()

                    renderUI()
                }
            }
            langLayout.addView(btn)
        }
        langScrollView.addView(langLayout)
        suggestionContainer.addView(langScrollView)
    }

    private fun renderUI() {
        keysContainer.removeAllViews()

        when (currentMode) {
            KeyboardViewMode.QWERTY -> buildQwertyLayout()
            KeyboardViewMode.SYMBOLS -> buildSymbolsLayout()
            KeyboardViewMode.CODE_PALETTE -> renderCodePalette()
        }
        updateLiveSuggestions()
    }

    private fun buildQwertyLayout() {
        val rows = listOf(
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
            listOf("Shift", "z", "x", "c", "v", "b", "n", "m", "DEL"),
            listOf("SYM", "CODE", "TAB", "SPACE", "ENTER")
        )
        renderStandardRows(rows)
    }

    private fun buildSymbolsLayout() {
        val rows = listOf(
            listOf("{", "}", "[", "]", "(", ")", "<", ">", ";", ":"),
            listOf("=", "+", "-", "*", "/", "\\", "|", "&", "^", "%"),
            listOf("$", "#", "@", "!", "?", "~", "_", "\"", "'", "`"),
            listOf("==", "!=", "->", "=>", "&&", "||", "++", "--", "+=", "-="),
            listOf("ABC", "CODE", "TAB", "SPACE", "DEL", "ENTER")
        )
        renderStandardRows(rows)
    }

    private fun renderCodePalette() {
        val categories = languageDatabase[selectedLanguage] ?: return
        val catScrollView = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val catLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        categories.forEachIndexed { index, cat ->
            val isSelected = index == selectedCategoryIndex
            val btn = Button(this).apply {
                text = cat.title
                setTextColor(Color.WHITE)
                background = createDrawable(if (isSelected) "#374151" else "#1F2937", if (isSelected) "#60A5FA" else "#374151")
                textSize = 11f
                isAllCaps = false
                setOnClickListener {
                    selectedCategoryIndex = index
                    renderUI()
                }
            }
            catLayout.addView(btn)
        }
        catScrollView.addView(catLayout)
        keysContainer.addView(catScrollView)

        val elementsScrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val elementsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val activeCategory = categories.getOrNull(selectedCategoryIndex) ?: categories.first()
        activeCategory.elements.chunked(3).forEach { rowElements ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            for (elem in rowElements) {
                val btn = Button(this).apply {
                    text = elem.trim()
                    setTextColor(Color.WHITE)
                    textSize = 12f
                    isAllCaps = false
                    background = createDrawable("#161B22", "#30363D")
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(4, 4, 4, 4)
                    }
                    setOnClickListener {
                        currentInputConnection?.commitText(elem, 1)
                        updateLiveSuggestions()
                    }
                }
                rowLayout.addView(btn)
            }
            elementsContainer.addView(rowLayout)
        }
        elementsScrollView.addView(elementsContainer)
        keysContainer.addView(elementsScrollView)

        val bottomNav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 110)
        }
        val navKeys = listOf("ABC", "SYM", "TAB", "SPACE", "DEL", "ENTER")
        for (key in navKeys) {
            val weight = if (key == "SPACE") 2f else 1f
            val btn = Button(this).apply {
                text = key
                setTextColor(Color.WHITE)
                textSize = 12f
                isAllCaps = false
                background = createDrawable("#21262D", "#00E676")
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight).apply {
                    setMargins(3, 3, 3, 3)
                }
                setOnClickListener { handleKeyPress(key) }
            }
            bottomNav.addView(btn)
        }
        keysContainer.addView(bottomNav)
    }

    private fun renderStandardRows(rows: List<List<String>>) {
        for (row in rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            }

            for (key in row) {
                val weight = when (key) {
                    "SPACE" -> 3f
                    "ENTER", "DEL", "ABC", "SYM", "CODE", "Shift" -> 1.5f
                    "TAB" -> 1.2f
                    else -> 1f
                }

                val isSpecial = key in listOf("ABC", "SYM", "CODE", "DEL", "ENTER", "TAB", "Shift")
                val btnBgColor = if (isSpecial) "#21262D" else "#161B22"
                val borderColor = if (isSpecial) "#00E676" else "#30363D"

                val btn = Button(this).apply {
                    text = key
                    setTextColor(Color.WHITE)
                    textSize = if (key.length > 3) 11f else 14f
                    isAllCaps = false
                    background = createDrawable(btnBgColor, borderColor)
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight).apply {
                        setMargins(3, 3, 3, 3)
                    }
                    setOnClickListener { handleKeyPress(key) }
                }
                rowLayout.addView(btn)
            }
            keysContainer.addView(rowLayout)
        }
    }

    private fun handleKeyPress(key: String) {
        val ic = currentInputConnection ?: return

        when (key) {
            "ABC" -> { currentMode = KeyboardViewMode.QWERTY; renderUI() }
            "SYM" -> { currentMode = KeyboardViewMode.SYMBOLS; renderUI() }
            "CODE" -> { currentMode = KeyboardViewMode.CODE_PALETTE; renderUI() }
            "DEL" -> {
                val selectedText = ic.getSelectedText(0)
                if (TextUtils.isEmpty(selectedText)) {
                    ic.deleteSurroundingText(1, 0)
                } else {
                    ic.commitText("", 1)
                }
                updateLiveSuggestions()
            }
            "SPACE" -> { ic.commitText(" ", 1); updateLiveSuggestions() }
            "TAB" -> { ic.commitText("    ", 1); updateLiveSuggestions() }
            "ENTER" -> { ic.commitText("\n", 1); updateLiveSuggestions() }
            "Shift" -> { }
            else -> { ic.commitText(key, 1); updateLiveSuggestions() }
        }
    }

    private fun createDrawable(bgColor: String, borderColor: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 10f
            setColor(Color.parseColor(bgColor))
            setStroke(2, Color.parseColor(borderColor))
        }
    }
}
