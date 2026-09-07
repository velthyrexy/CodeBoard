package com.codeboard.keyboard.keyboard

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
    private var selectedLanguage = "Python"
    private var selectedCategoryIndex = 0

    // Complete multi-language database (All titles & UI in English)
    private val languageDatabase = mapOf(
        "Python" to listOf(
            CodeCategory("Keywords", listOf("def ", "class ", "return ", "import ", "from ", "as ", "pass", "lambda ", "global ", "nonlocal ", "yield ", "with ", "raise ", "assert ", "del ")),
            CodeCategory("Control Flow", listOf("if ", "elif ", "else:", "for ", "in ", "while ", "break", "continue", "try:", "except ", "finally:")),
            CodeCategory("Types & Values", listOf("True", "False", "None", "and ", "or ", "not ", "is ", "in ")),
            CodeCategory("Built-ins", listOf("print(", "len(", "range(", "type(", "int(", "str(", "float(", "list(", "dict(", "set(", "tuple(", "enumerate(", "zip(", "map(", "filter(", "input(", "sum(", "min(", "max(", "sorted(")),
            CodeCategory("Methods", listOf(".append(", ".extend(", ".pop(", ".split(", ".replace(", ".join(", ".strip(", ".format(", ".get(", ".keys()", ".values()", ".items()")),
            CodeCategory("Snippets", listOf("if __name__ == '__main__':\n    ", "def __init__(self, ", "try:\n    pass\nexcept Exception as e:\n    pass", "with open('', 'r') as f:\n    "))
        ),
        "JavaScript" to listOf(
            CodeCategory("Declarations", listOf("const ", "let ", "var ", "function ", "=> ", "class ", "extends ", "constructor", "this.", "super(")),
            CodeCategory("Control Flow", listOf("if ", "else ", "switch ", "case ", "default:", "for ", "while ", "do ", "break", "continue", "try ", "catch ", "finally ", "throw ")),
            CodeCategory("Async & Modules", listOf("async ", "await ", "Promise", "import ", "export ", "export default ", "require(", "module.exports")),
            CodeCategory("DOM & Console", listOf("console.log(", "console.error(", "document.getElementById(", "document.querySelector(", "document.querySelectorAll(", "addEventListener(")),
            CodeCategory("Array & Object", listOf(".map(", ".filter(", ".reduce(", ".forEach(", ".find(", ".includes(", ".push(", ".slice(", "Object.keys(", "Object.values(", "JSON.stringify(", "JSON.parse(")),
            CodeCategory("Snippets", listOf("const fetchData = async () => {\n  try {\n    \n  } catch (err) {}\n}", "setTimeout(() => {\n  \n}, 1000);"))
        ),
        "TypeScript" to listOf(
            CodeCategory("Type Defs", listOf("type ", "interface ", "enum ", "as ", "keyof ", "typeof ", "unknown", "never", "any", "void", "string", "number", "boolean")),
            CodeCategory("Generics & Access", listOf("readonly ", "private ", "public ", "protected ", "abstract ", "<T>", "Record<", "Partial<", "Omit<", "Pick<")),
            CodeCategory("Snippets", listOf("interface Props {\n  \n}", "type Response<T> = {\n  data: T;\n};"))
        ),
        "Kotlin" to listOf(
            CodeCategory("Declarations", listOf("val ", "var ", "fun ", "class ", "data class ", "object ", "interface ", "enum class ", "typealias ")),
            CodeCategory("Modifiers", listOf("private ", "protected ", "public ", "internal ", "override ", "open ", "abstract ", "sealed ", "companion object")),
            CodeCategory("Control & Null", listOf("when ", "if ", "else ", "for ", "while ", "try ", "catch ", "?:", "!!", "as?", "is ", "!is ")),
            CodeCategory("Collections", listOf("listOf(", "mutableListOf(", "mapOf(", "mutableMapOf(", "setOf(", ".forEach { ", ".map { ", ".filter { ", ".let { ", ".apply { ", ".also { ")),
            CodeCategory("Coroutines", listOf("suspend ", "launch ", "async ", "Dispatchers.IO", "Dispatchers.Main", "Flow<", "StateFlow<")),
            CodeCategory("Snippets", listOf("companion object {\n    \n}", "data class Model(\n    val id: String\n)"))
        ),
        "Java" to listOf(
            CodeCategory("Modifiers", listOf("public ", "private ", "protected ", "static ", "final ", "abstract ", "class ", "interface ", "extends ", "implements ")),
            CodeCategory("Data Types", listOf("int ", "long ", "double ", "float ", "boolean ", "char ", "String ", "void ", "byte[] ")),
            CodeCategory("Control & IO", listOf("if ", "else ", "switch ", "case ", "for ", "while ", "try ", "catch ", "finally ", "throw ", "System.out.println(")),
            CodeCategory("Collections", listOf("List<", "ArrayList<>", "Map<", "HashMap<>", "Set<", "HashSet<>", ".add(", ".get(", ".size()")),
            CodeCategory("Snippets", listOf("public static void main(String[] args) {\n    \n}", "public class Main {\n    \n}"))
        ),
        "C++" to listOf(
            CodeCategory("Data Types", listOf("int ", "double ", "float ", "char ", "bool ", "auto ", "void ", "long ", "unsigned ", "const ")),
            CodeCategory("Standard Lib", listOf("std::cout << ", "std::cin >> ", "std::endl", "std::string", "std::vector<", "std::map<", "std::pair<")),
            CodeCategory("Pointers & Memory", listOf("new ", "delete ", "nullptr", "sizeof(", "&", "*", "unique_ptr<", "shared_ptr<")),
            CodeCategory("Preprocessor", listOf("#include <iostream>", "#include <vector>", "#include <string>", "#define ", "#ifdef ", "#endif")),
            CodeCategory("Snippets", listOf("#include <iostream>\nusing namespace std;\n\nint main() {\n    return 0;\n}"))
        ),
        "C#" to listOf(
            CodeCategory("Declarations", listOf("public ", "private ", "protected ", "internal ", "static ", "class ", "struct ", "interface ", "namespace ", "using ")),
            CodeCategory("LINQ & Types", listOf("var ", "async ", "await ", "Task<", "List<", "Dictionary<", ".Where(", ".Select(", ".FirstOrDefault(")),
            CodeCategory("Control & IO", listOf("if ", "else ", "switch ", "case ", "foreach ", "while ", "try ", "catch ", "Console.WriteLine(")),
            CodeCategory("Snippets", listOf("using System;\n\nnamespace App {\n    class Program {\n        static void Main() {\n            \n        }\n    }\n}"))
        ),
        "Go" to listOf(
            CodeCategory("Keywords", listOf("func ", "package ", "import ", "type ", "struct ", "interface ", "var ", "const ", "return ", "defer ")),
            CodeCategory("Control & Types", listOf("if ", "else ", "for ", "range ", "switch ", "case ", "select ", "int", "string", "bool", "byte", "error")),
            CodeCategory("Concurrency", listOf("go ", "chan ", "make(", "append(", "len(", "cap(")),
            CodeCategory("Snippets", listOf("package main\n\nimport \"fmt\"\n\nfunc main() {\n    fmt.Println(\"Hello\")\n}"))
        ),
        "Rust" to listOf(
            CodeCategory("Declarations", listOf("fn ", "let ", "mut ", "const ", "struct ", "enum ", "trait ", "impl ", "pub ", "use ", "mod ")),
            CodeCategory("Control & Types", listOf("match ", "if ", "else ", "loop ", "while ", "for ", "in ", "Option<", "Result<", "Some(", "None", "Ok(", "Err(")),
            CodeCategory("Macros & Std", listOf("println!(", "format!(", "vec![", "panic!(", "String::from(", ".unwrap()", ".expect(")),
            CodeCategory("Snippets", listOf("fn main() {\n    println!(\"Hello, world!\");\n}"))
        ),
        "Swift" to listOf(
            CodeCategory("Declarations", listOf("func ", "var ", "let ", "class ", "struct ", "enum ", "protocol ", "extension ", "import ")),
            CodeCategory("Control & Guard", listOf("if ", "else ", "guard ", "switch ", "case ", "for ", "in ", "while ", "repeat ", "defer ")),
            CodeCategory("Optionals & Types", listOf("Int", "String", "Double", "Bool", "Any", "nil", "if let ", "guard let ")),
            CodeCategory("Snippets", listOf("import Foundation\n\nstruct User {\n    let id: String\n}"))
        ),
        "PHP" to listOf(
            CodeCategory("Keywords", listOf("function ", "class ", "public ", "private ", "protected ", "return ", "use ", "namespace ", "echo ")),
            CodeCategory("Control Flow", listOf("if ", "else ", "elseif ", "foreach ", "while ", "switch ", "try ", "catch ")),
            CodeCategory("Built-ins", listOf("array(", "count(", "explode(", "implode(", "isset(", "empty(", "header(")),
            CodeCategory("Snippets", listOf("<?php\n\nnamespace App;\n\nclass Controller {\n    \n}"))
        ),
        "HTML" to listOf(
            CodeCategory("Elements", listOf("<div>", "</div>", "<span>", "</span>", "<p>", "</p>", "<a href=\"\">", "<img src=\"\">", "<ul>", "<li>", "<table>", "<tr>", "<td>")),
            CodeCategory("Forms & Struct", listOf("<form>", "<input type=\"text\"", "<button>", "<select>", "<option>", "<header>", "<footer>", "<section>", "<nav>")),
            CodeCategory("Head & Meta", listOf("<!DOCTYPE html>", "<html>", "<head>", "<meta charset=\"UTF-8\">", "<title>", "<link rel=\"stylesheet\" href=\"\">"))
        ),
        "CSS" to listOf(
            CodeCategory("Layout & Flex", listOf("display: flex;", "display: grid;", "position: absolute;", "position: relative;", "justify-content: center;", "align-items: center;")),
            CodeCategory("Box Model", listOf("width: ", "height: ", "margin: ", "padding: ", "border: ", "box-sizing: border-box;")),
            CodeCategory("Typography & Color", listOf("color: ", "background-color: ", "font-size: ", "font-weight: bold;", "text-align: center;"))
        ),
        "SQL" to listOf(
            CodeCategory("Queries", listOf("SELECT ", "FROM ", "WHERE ", "INSERT INTO ", "UPDATE ", "DELETE ", "CREATE TABLE ", "ALTER TABLE ", "DROP TABLE ")),
            CodeCategory("Clauses & Joins", listOf("JOIN ", "LEFT JOIN ", "INNER JOIN ", "ON ", "GROUP BY ", "ORDER BY ", "HAVING ", "LIMIT ", "AND ", "OR ", "NOT ", "IN ")),
            CodeCategory("Aggregates", listOf("COUNT(", "SUM(", "AVG(", "MAX(", "MIN(", "DISTINCT ", "COALESCE("))
        ),
        "Bash" to listOf(
            CodeCategory("Commands", listOf("echo ", "cd ", "ls -la", "mkdir ", "rm -rf ", "cp ", "mv ", "grep ", "find ", "chmod +x ")),
            CodeCategory("Control Flow", listOf("if [ ]; then", "else", "fi", "for in; do", "done", "while; do", "case in")),
            CodeCategory("Variables & Env", listOf("$1", "$@", "$?", "export ", "source ", "alias ", "PATH=")),
            CodeCategory("Snippets", listOf("#!/bin/bash\n\nset -e\n\necho \"Starting process...\""))
        )
    )

    override fun onCreateInputView(): View {
        keyboardView = LayoutInflater.from(this).inflate(R.layout.keyboard_view, null)
        suggestionContainer = keyboardView.findViewById(R.id.suggestion_container)
        keysContainer = keyboardView.findViewById(R.id.keys_container)

        renderUI()
        return keyboardView
    }

    private fun renderUI() {
        keysContainer.removeAllViews()
        suggestionContainer.removeAllViews()

        when (currentMode) {
            KeyboardViewMode.QWERTY -> buildQwertyLayout()
            KeyboardViewMode.SYMBOLS -> buildSymbolsLayout()
            KeyboardViewMode.CODE_PALETTE -> renderCodePalette()
        }
    }

    // --- 1. QWERTY KEYBOARD ---
    private fun buildQwertyLayout() {
        val rows = listOf(
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
            listOf("Shift", "z", "x", "c", "v", "b", "n", "m", "DEL"),
            listOf("SYM", "CODE", "TAB", "SPACE", "->", "ENTER")
        )
        renderStandardRows(rows)
    }

    // --- 2. SYMBOLS KEYBOARD ---
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

    // --- 3. CODE PALETTE MODE ---
    private fun renderCodePalette() {
        // Top Header: Horizontal Language Selector Bar
        val langScrollView = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
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
                    renderUI()
                }
            }
            langLayout.addView(btn)
        }
        langScrollView.addView(langLayout)
        suggestionContainer.addView(langScrollView)

        // Middle Section: Category Selector Bar
        val categories = languageDatabase[selectedLanguage] ?: return
        val catScrollView = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
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

        // Grid Section: Vertical Scrollable Code Elements
        val elementsScrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        val elementsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

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
                    }
                }
                rowLayout.addView(btn)
            }
            elementsContainer.addView(rowLayout)
        }
        elementsScrollView.addView(elementsContainer)
        keysContainer.addView(elementsScrollView)

        // Bottom Action Bar
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
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
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
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        weight
                    ).apply { setMargins(3, 3, 3, 3) }

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
            "ABC" -> {
                currentMode = KeyboardViewMode.QWERTY
                renderUI()
            }
            "SYM" -> {
                currentMode = KeyboardViewMode.SYMBOLS
                renderUI()
            }
            "CODE" -> {
                currentMode = KeyboardViewMode.CODE_PALETTE
                renderUI()
            }
            "DEL" -> {
                val selectedText = ic.getSelectedText(0)
                if (TextUtils.isEmpty(selectedText)) {
                    ic.deleteSurroundingText(1, 0)
                } else {
                ic.commitText("", 1)
                }
            }
            "SPACE" -> ic.commitText(" ", 1)
            "TAB" -> ic.commitText("    ", 1)
            "ENTER" -> ic.commitText("\n", 1)
            "Shift" -> { }
            else -> ic.commitText(key, 1)
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
