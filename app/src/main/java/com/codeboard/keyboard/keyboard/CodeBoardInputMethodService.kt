package com.codeboard.keyboard.keyboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import com.codeboard.keyboard.R
import com.codeboard.keyboard.data.SuggestionEngine

class CodeBoardInputMethodService : InputMethodService() {

    private lateinit var keyboardView: View
    private lateinit var suggestionContainer: LinearLayout
    private lateinit var keysContainer: LinearLayout
    private val suggestionEngine = SuggestionEngine()
    private var currentWord = ""

    override fun onCreateInputView(): View {
        keyboardView = LayoutInflater.from(this).inflate(R.layout.keyboard_view, null)
        suggestionContainer = keyboardView.findViewById(R.id.suggestion_container)
        keysContainer = keyboardView.findViewById(R.id.keys_container)

        setupQwertyLayout()
        updateSuggestions("")
        return keyboardView
    }

    private fun setupQwertyLayout() {
        keysContainer.removeAllViews()

        val rows = listOf(
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
            listOf("Shift", "z", "x", "c", "v", "b", "n", "m", "DEL"),
            listOf("123", "SPACE", "ENTER")
        )

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
                    "ENTER", "DEL", "Shift", "123" -> 1.5f
                    else -> 1f
                }

                val btn = Button(this).apply {
                    text = key
                    setTextColor(Color.WHITE)
                    background = createButtonDrawable(isSelected = false)
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        weight
                    ).apply { setMargins(3, 3, 3, 3) }
                    isAllCaps = false
                    textSize = 14f

                    setOnClickListener { handleKeyPress(key) }
                }
                rowLayout.addView(btn)
            }
            keysContainer.addView(rowLayout)
        }
    }

    private fun createButtonDrawable(isSelected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 10f
            if (isSelected) {
                setColor(Color.parseColor("#FFD700"))
            } else {
                setColor(Color.BLACK)
                setStroke(2, Color.parseColor("#FFD700"))
            }
        }
    }

    private fun handleKeyPress(key: String) {
        val ic = currentInputConnection ?: return

        when (key) {
            "DEL" -> {
                val selectedText = ic.getSelectedText(0)
                if (TextUtils.isEmpty(selectedText)) {
                    ic.deleteSurroundingText(1, 0)
                } else {
                    ic.commitText("", 1)
                }
                updateCurrentWord()
            }
            "SPACE" -> {
                ic.commitText(" ", 1)
                currentWord = ""
                updateSuggestions("")
            }
            "ENTER" -> {
                ic.commitText("\n", 1)
                currentWord = ""
                updateSuggestions("")
            }
            "Shift", "123" -> { }
            else -> {
                ic.commitText(key, 1)
                updateCurrentWord()
            }
        }
    }

    private fun updateCurrentWord() {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(30, 0) ?: ""

        val words = textBefore.toString().split(Regex("[^a-zA-Z0-9_:]"))
        currentWord = words.lastOrNull() ?: ""

        updateSuggestions(currentWord)
    }

    private fun updateSuggestions(prefix: String) {
        suggestionContainer.removeAllViews()
        val suggestions = suggestionEngine.getSuggestions(prefix, 4)

        suggestions.forEach { suggestion ->
            val btn = Button(this).apply {
                text = suggestion.text
                setTextColor(Color.BLACK)
                background = createButtonDrawable(isSelected = true)
                textSize = 11f
                isAllCaps = false
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1f
                ).apply { setMargins(4, 4, 4, 4) }

                setOnClickListener { applySuggestion(suggestion.text) }
            }
            suggestionContainer.addView(btn)
        }
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
