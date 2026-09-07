package com.codeboard.keyboard.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.codeboard.keyboard.R
import com.codeboard.keyboard.data.SuggestionEngine

class CodeBoardInputMethodService : InputMethodService() {

    private var keyboardView: View? = null
    private var suggestionContainer: LinearLayout? = null
    private var keysContainer: LinearLayout? = null
    private val suggestionEngine = SuggestionEngine()

    private var currentWord = ""
    private var isShifted = false
    private var isSymbolMode = false

    // DEL hold-to-delete mechanism
    private val deleteHandler = Handler(Looper.getMainLooper())
    private var isDeleting = false
    private val deleteRunnable = object : Runnable {
        override fun run() {
            if (isDeleting) {
                performDelete()
                deleteHandler.postDelayed(this, 50)
            }
        }
    }

    override fun onCreateInputView(): View {
        val view = LayoutInflater.from(this).inflate(R.layout.keyboard_view, null)
        keyboardView = view
        suggestionContainer = view.findViewById(R.id.suggestion_container)
        keysContainer = view.findViewById(R.id.keys_container)

        renderKeyboardLayout()
        updateSuggestions("")
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentWord = ""
        updateSuggestions(currentWord)
    }

    private fun getSelectedLanguage(): String {
        val sharedPrefs = getSharedPreferences("codeboard_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getString("selected_language", "Luau") ?: "Luau"
    }

    private fun renderKeyboardLayout() {
        val container = keysContainer ?: return
        container.removeAllViews()

        val rows = if (isSymbolMode) {
            listOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                listOf("{", "}", "[", "]", "(", ")", "<", ">", "=", "+"),
                listOf("-", "*", "/", "%", "&", "|", "!", "?", ":", ";"),
                listOf("ABC", "🌐", "SPACE", "ENTER", "DEL")
            )
        } else {
            val r1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
            val r2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
            val r3 = listOf("Shift", "z", "x", "c", "v", "b", "n", "m", "DEL")
            val r4 = listOf("123", "🌐", "SPACE", "ENTER")

            if (isShifted) {
                listOf(
                    r1.map { it.uppercase() },
                    r2.map { it.uppercase() },
                    listOf("SHIFT") + r3.drop(1).dropLast(1).map { it.uppercase() } + listOf("DEL"),
                    r4
                )
            } else {
                listOf(r1, r2, r3, r4)
            }
        }

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
                    "ENTER", "DEL", "Shift", "SHIFT", "123", "ABC" -> 1.3f
                    "🌐" -> 1f
                    else -> 1f
                }

                val btn = Button(this).apply {
                    text = key
                    setTextColor(Color.WHITE)
                    
                    val activeKey = (key == "Shift" && isShifted) || (key == "SHIFT" && isShifted)
                    background = createButtonDrawable(isSelected = activeKey)
                    
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        weight
                    ).apply { setMargins(3, 3, 3, 3) }
                    isAllCaps = false
                    textSize = 13f

                    if (key == "DEL") {
                        setupDeleteTouchListener(this)
                    } else {
                        setOnClickListener { handleKeyPress(key) }
                    }
                }
                rowLayout.addView(btn)
            }
            container.addView(rowLayout)
        }
    }

    private fun setupDeleteTouchListener(btn: Button) {
        btn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    performDelete()
                    isDeleting = true
                    deleteHandler.postDelayed(deleteRunnable, 400)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDeleting = false
                    deleteHandler.removeCallbacks(deleteRunnable)
                    true
                }
                else -> false
            }
        }
    }

    private fun createButtonDrawable(isSelected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 10f
            if (isSelected) {
                setColor(Color.parseColor("#FFD700"))
            } else {
                setColor(Color.parseColor("#1A1A1A"))
                setStroke(2, Color.parseColor("#FFD700"))
            }
        }
    }

    private fun handleKeyPress(key: String) {
        val ic = currentInputConnection ?: return

        when (key) {
            "Shift", "SHIFT" -> {
                isShifted = !isShifted
                renderKeyboardLayout()
            }
            "123" -> {
                isSymbolMode = true
                renderKeyboardLayout()
            }
            "ABC" -> {
                isSymbolMode = false
                renderKeyboardLayout()
            }
            "🌐" -> {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
            "SPACE" -> {
                ic.commitText(" ", 1)
                currentWord = ""
                if (isShifted) {
                    isShifted = false
                    renderKeyboardLayout()
                }
                updateSuggestions("")
            }
            "ENTER" -> {
                ic.commitText("\n", 1)
                currentWord = ""
                updateSuggestions("")
            }
            else -> {
                ic.commitText(key, 1)
                if (isShifted) {
                    isShifted = false
                    renderKeyboardLayout()
                }
                updateCurrentWord()
            }
        }
    }

    private fun performDelete() {
        val ic = currentInputConnection ?: return
        val selectedText = ic.getSelectedText(0)
        if (TextUtils.isEmpty(selectedText)) {
            ic.deleteSurroundingText(1, 0)
        } else {
            ic.commitText("", 1)
        }
        updateCurrentWord()
    }

    private fun updateCurrentWord() {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(30, 0) ?: ""

        val words = textBefore.toString().split(Regex("[^a-zA-Z0-9_:#!\\-<>]"))
        currentWord = words.lastOrNull() ?: ""

        updateSuggestions(currentWord)
    }

    private fun updateSuggestions(prefix: String) {
        val container = suggestionContainer ?: return
        container.removeAllViews()

        // Clipboard Button
        val pasteBtn = Button(this).apply {
            text = "📋 Paste"
            setTextColor(Color.BLACK)
            background = createButtonDrawable(isSelected = true)
            textSize = 11f
            isAllCaps = false
            setPadding(12, 0, 12, 0)
            setOnClickListener { pasteFromClipboard() }
        }
        container.addView(pasteBtn)

        val currentLang = getSelectedLanguage()
        val suggestions = suggestionEngine.getSuggestions(currentLang, prefix, 15)
        
        suggestions.forEach { suggestion ->
            val btn = Button(this).apply {
                text = suggestion.text
                setTextColor(Color.WHITE)
                background = createButtonDrawable(isSelected = false)
                textSize = 11f
                isAllCaps = false
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply { setMargins(4, 4, 4, 4) }

                setOnClickListener { applySuggestion(suggestion.text) }
            }
            container.addView(btn)
        }
    }

    private fun pasteFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val pasteText = clipData.getItemAt(0).coerceToText(this).toString()
                currentInputConnection?.commitText(pasteText, 1)
                updateCurrentWord()
                return
            }
        }
        Toast.makeText(this, "Clipboard is empty!", Toast.LENGTH_SHORT).show()
    }

    private fun applySuggestion(suggestionText: String) {
        val ic = currentInputConnection ?: return

        if (currentWord.isNotEmpty()) {
            ic.deleteSurroundingText(currentWord.length, 0)
        }

        ic.commitText(suggestionText, 1)
        currentWord = ""
        updateSuggestions("")
    }
}
