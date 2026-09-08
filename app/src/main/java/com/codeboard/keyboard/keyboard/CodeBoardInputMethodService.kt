package com.codeboard.keyboard.keyboard

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
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
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

    override fun onConfigureWindow(
        win: Window,
        isInputViewShown: Boolean,
        isCandidateViewShown: Boolean
    ) {
        super.onConfigureWindow(win, isInputViewShown, isCandidateViewShown)
        win.navigationBarColor = Color.parseColor("#050505")
    }

    override fun onCreateInputView(): View {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.keyboard_view, null)

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
        isShifted = false
        isSymbolMode = false

        renderKeyboardLayout()
        updateSuggestions("")
    }

    private fun getSelectedLanguage(): String {
        val sharedPrefs = getSharedPreferences(
            "codeboard_prefs",
            Context.MODE_PRIVATE
        )

        return sharedPrefs.getString(
            "selected_language",
            "Luau"
        ) ?: "Luau"
    }

    private fun renderKeyboardLayout() {

        val container = keysContainer ?: return
        container.removeAllViews()

        val rows = if (isSymbolMode) {

            listOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),

                listOf(
                    "{", "}", "[", "]", "(", ")",
                    "<", ">", "=", "+"
                ),

                listOf(
                    "-", "*", "/", "%", "&",
                    "|", "!", "?", ":", ";"
                ),

                listOf(
                    "ABC", ",", "Space", "Enter"
                )
            )

        } else {

            val r1 = listOf(
                "q", "w", "e", "r", "t",
                "y", "u", "i", "o", "p"
            )

            val r2 = listOf(
                "a", "s", "d", "f", "g",
                "h", "j", "k", "l"
            )

            val r3 = listOf(
                "Shift", "z", "x", "c", "v",
                "b", "n", "m", "DEL"
            )

            val r4 = listOf(
                "?123", ",", "Space", "Enter"
            )

            if (isShifted) {

                listOf(
                    r1.map { it.uppercase() },
                    r2.map { it.uppercase() },
                    listOf("Shift") +
                            r3.drop(1)
                                .dropLast(1)
                                .map { it.uppercase() } +
                            listOf("DEL"),
                    r4
                )

            } else {

                listOf(
                    r1,
                    r2,
                    r3,
                    r4
                )
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

                    "Space" -> 2.8f

                    "Enter" -> 1.5f

                    "Shift",
                    "DEL",
                    "?123",
                    "ABC" -> 1.2f

                    else -> 1f
                }

                val button = Button(this).apply {

                    text = when (key) {

                        "Shift" -> "⇧"

                        "DEL" -> "⌫"

                        else -> key
                    }

                    val isEnter = key == "Enter"
                    val isShiftActive =
                        key == "Shift" && isShifted

                    if (isEnter || isShiftActive) {

                        setTextColor(Color.BLACK)
                        background =
                            createFilledYellowDrawable()

                    } else {

                        setTextColor(Color.WHITE)
                        background =
                            createOutlineDrawable(12f)
                    }

                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        weight
                    ).apply {

                        setMargins(
                            3,
                            3,
                            3,
                            3
                        )
                    }

                    isAllCaps = false

                    textSize = when (key) {

                        "Enter",
                        "Space" -> 14f

                        else -> 15f
                    }

                    if (key == "DEL") {

                        setupDeleteTouchListener(this)

                    } else {

                        setOnClickListener {
                            handleKeyPress(key)
                        }
                    }
                }

                rowLayout.addView(button)
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

                    deleteHandler.postDelayed(
                        deleteRunnable,
                        400
                    )

                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    isDeleting = false

                    deleteHandler.removeCallbacks(
                        deleteRunnable
                    )

                    true
                }

                else -> false
            }
        }
    }

    private fun createOutlineDrawable(
        cornerRadiusPx: Float = 12f
    ): GradientDrawable {

        return GradientDrawable().apply {

            shape = GradientDrawable.RECTANGLE

            cornerRadius = cornerRadiusPx

            setColor(
                Color.parseColor("#121212")
            )

            setStroke(
                2,
                Color.parseColor("#FFD700")
            )
        }
    }

    private fun createFilledYellowDrawable(): GradientDrawable {

        return GradientDrawable().apply {

            shape = GradientDrawable.RECTANGLE

            cornerRadius = 12f

            setColor(
                Color.parseColor("#FFD700")
            )
        }
    }

    private fun handleKeyPress(key: String) {

        val ic = currentInputConnection ?: return

        when (key) {

            "Shift" -> {

                isShifted = !isShifted

                renderKeyboardLayout()
            }

            "?123" -> {

                isSymbolMode = true

                renderKeyboardLayout()
            }

            "ABC" -> {

                isSymbolMode = false

                renderKeyboardLayout()
            }

            "Space" -> {

                ic.commitText(" ", 1)

                currentWord = ""

                if (isShifted) {

                    isShifted = false

                    renderKeyboardLayout()
                }

                updateSuggestions("")
            }

            "Enter" -> {

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

        val selectedText =
            ic.getSelectedText(0)

        if (TextUtils.isEmpty(selectedText)) {

            ic.deleteSurroundingText(
                1,
                0
            )

        } else {

            ic.commitText(
                "",
                1
            )
        }

        updateCurrentWord()
    }

    private fun updateCurrentWord() {

        val ic = currentInputConnection ?: return

        val textBefore =
            ic.getTextBeforeCursor(
                50,
                0
            ) ?: ""

        val words = textBefore
            .toString()
            .split(
                Regex("[^a-zA-Z0-9_:#!\\-<>]")
            )

        currentWord =
            words.lastOrNull() ?: ""

        updateSuggestions(currentWord)
    }

    private fun updateSuggestions(prefix: String) {

        val container =
            suggestionContainer ?: return

        container.removeAllViews()

        val currentLanguage =
            getSelectedLanguage()

        // HER ZAMAN 8 ÖNERİ İSTİYORUZ
        val suggestions =
            suggestionEngine.getSuggestions(
                currentLanguage,
                prefix,
                8
            )

        for (suggestion in suggestions) {

            val button = Button(this).apply {

                text = suggestion.text

                setTextColor(Color.WHITE)

                background =
                    createOutlineDrawable(10f)

                textSize = 11f

                isAllCaps = false

                maxLines = 1

                ellipsize =
                    TextUtils.TruncateAt.END

                minWidth = 0

                minimumWidth = 0

                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1f
                    ).apply {

                        setMargins(
                            2,
                            2,
                            2,
                            2
                        )
                    }

                setOnClickListener {

                    applySuggestion(
                        suggestion.text
                    )
                }
            }

            container.addView(button)
        }
    }

    private fun applySuggestion(
        suggestionText: String
    ) {

        val ic =
            currentInputConnection ?: return

        if (currentWord.isNotEmpty()) {

            ic.deleteSurroundingText(
                currentWord.length,
                0
            )
        }

        ic.commitText(
            suggestionText,
            1
        )

        currentWord = ""

        updateSuggestions("")
    }
}
