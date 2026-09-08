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
                deleteHandler.postDelayed(this, 55)
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
        win.statusBarColor = Color.parseColor("#050505")
    }

    override fun onCreateInputView(): View {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.keyboard_view, null)

        keyboardView = view

        suggestionContainer =
            view.findViewById(R.id.suggestion_container)

        keysContainer =
            view.findViewById(R.id.keys_container)

        renderKeyboardLayout()
        updateSuggestions("")

        return view
    }

    override fun onStartInputView(
        info: EditorInfo?,
        restarting: Boolean
    ) {
        super.onStartInputView(info, restarting)

        currentWord = ""
        isShifted = false
        isSymbolMode = false

        renderKeyboardLayout()
        updateSuggestions("")
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)

        isDeleting = false
        deleteHandler.removeCallbacks(deleteRunnable)
    }

    private fun getSelectedLanguage(): String {
        val preferences = getSharedPreferences(
            "codeboard_prefs",
            Context.MODE_PRIVATE
        )

        return preferences.getString(
            "selected_language",
            "Luau"
        ) ?: "Luau"
    }

    private fun renderKeyboardLayout() {

        val container = keysContainer ?: return

        container.removeAllViews()

        val rows = if (isSymbolMode) {

            listOf(
                listOf(
                    "1", "2", "3", "4", "5",
                    "6", "7", "8", "9", "0"
                ),

                listOf(
                    "{", "}", "[", "]", "(",
                    ")", "<", ">", "=", "+"
                ),

                listOf(
                    "-", "_", "*", "/", "%",
                    "&", "|", "!", "?", ":"
                ),

                listOf(
                    "ABC", ",", ".", "'", "\"",
                    "Space", "Enter"
                )
            )

        } else {

            listOf(
                listOf(
                    "q", "w", "e", "r", "t",
                    "y", "u", "i", "o", "p"
                ),

                listOf(
                    "a", "s", "d", "f", "g",
                    "h", "j", "k", "l"
                ),

                listOf(
                    "Shift",
                    "z", "x", "c", "v",
                    "b", "n", "m",
                    "DEL"
                ),

                listOf(
                    "?123",
                    ",",
                    "Space",
                    ".",
                    "Enter"
                )
            )
        }

        rows.forEach { row ->

            val rowLayout = LinearLayout(this).apply {

                orientation = LinearLayout.HORIZONTAL

                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }

            row.forEach { key ->

                val weight = when (key) {
                    "Space" -> 2.8f
                    "Enter" -> 1.45f
                    "Shift", "DEL", "?123", "ABC" -> 1.25f
                    else -> 1f
                }

                val button = Button(this).apply {

                    text = when (key) {
                        "Shift" -> "⇧"
                        "DEL" -> "⌫"
                        else -> key
                    }

                    isAllCaps = false
                    includeFontPadding = false
                    maxLines = 1

                    textSize = when {
                        key == "Space" -> 13f
                        key == "Enter" -> 13f
                        key == "Shift" -> 20f
                        key == "DEL" -> 18f
                        key.length > 5 -> 11f
                        else -> 15f
                    }

                    val isEnter = key == "Enter"
                    val isShiftActive = key == "Shift" && isShifted

                    if (isEnter || isShiftActive) {
                        setTextColor(Color.BLACK)
                        background = createFilledYellowDrawable()
                    } else {
                        setTextColor(
                            if (key == "Space") {
                                Color.parseColor("#FFD700")
                            } else {
                                Color.WHITE
                            }
                        )

                        background = createOutlineDrawable()
                    }

                    stateListAnimator = null

                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        weight
                    ).apply {
                        setMargins(
                            2,
                            2,
                            2,
                            2
                        )
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

    private fun setupDeleteTouchListener(button: Button) {

        button.setOnTouchListener { _, event ->

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

    private fun createOutlineDrawable(): GradientDrawable {

        return GradientDrawable().apply {

            shape = GradientDrawable.RECTANGLE

            cornerRadius = 7f

            setColor(
                Color.parseColor("#101010")
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

            cornerRadius = 7f

            setColor(
                Color.parseColor("#FFD700")
            )
        }
    }

    private fun createSuggestionDrawable(): GradientDrawable {

        return GradientDrawable().apply {

            shape = GradientDrawable.RECTANGLE

            cornerRadius = 7f

            setColor(
                Color.parseColor("#0C0C0C")
            )

            setStroke(
                2,
                Color.parseColor("#FFD700")
            )
        }
    }

    private fun handleKeyPress(key: String) {

        val inputConnection =
            currentInputConnection ?: return

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

                inputConnection.commitText(
                    " ",
                    1
                )

                currentWord = ""

                if (isShifted) {
                    isShifted = false
                    renderKeyboardLayout()
                }

                updateSuggestions("")
            }

            "Enter" -> {

                inputConnection.commitText(
                    "\n",
                    1
                )

                currentWord = ""

                updateSuggestions("")
            }

            else -> {

                inputConnection.commitText(
                    key,
                    1
                )

                if (isShifted) {

                    isShifted = false

                    renderKeyboardLayout()
                }

                updateCurrentWord()
            }
        }
    }

    private fun performDelete() {

        val inputConnection =
            currentInputConnection ?: return

        val selectedText =
            inputConnection.getSelectedText(0)

        if (TextUtils.isEmpty(selectedText)) {

            inputConnection.deleteSurroundingText(
                1,
                0
            )

        } else {

            inputConnection.commitText(
                "",
                1
            )
        }

        updateCurrentWord()
    }

    private fun updateCurrentWord() {

        val inputConnection =
            currentInputConnection ?: return

        val textBefore =
            inputConnection.getTextBeforeCursor(
                80,
                0
            ) ?: return

        val text =
            textBefore.toString()

        val match = Regex(
            "[a-zA-Z0-9_:.!#<>+\\-]*$"
        ).find(text)

        currentWord =
            match?.value ?: ""

        updateSuggestions(currentWord)
    }

    private fun updateSuggestions(prefix: String) {

        val container =
            suggestionContainer ?: return

        container.removeAllViews()

        val language =
            getSelectedLanguage()

        val suggestions =
            suggestionEngine
                .getSuggestions(
                    language,
                    prefix,
                    4
                )
                .take(4)

        repeat(4) { index ->

            val suggestion =
                suggestions.getOrNull(index)

            val button = Button(this).apply {

                text =
                    suggestion?.text ?: ""

                isAllCaps = false
                includeFontPadding = false
                maxLines = 1

                ellipsize =
                    TextUtils.TruncateAt.END

                textSize = when {
                    suggestion == null -> 11f
                    suggestion.text.length > 22 -> 9f
                    suggestion.text.length > 15 -> 10f
                    else -> 11.5f
                }

                setTextColor(
                    if (suggestion != null) {
                        Color.WHITE
                    } else {
                        Color.TRANSPARENT
                    }
                )

                background =
                    createSuggestionDrawable()

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

                isClickable =
                    suggestion != null

                if (suggestion != null) {

                    setOnClickListener {

                        applySuggestion(
                            suggestion.text
                        )
                    }
                }
            }

            container.addView(button)
        }
    }

    private fun applySuggestion(
        suggestionText: String
    ) {

        val inputConnection =
            currentInputConnection ?: return

        if (currentWord.isNotEmpty()) {

            inputConnection.deleteSurroundingText(
                currentWord.length,
                0
            )
        }

        inputConnection.commitText(
            suggestionText,
            1
        )

        currentWord = ""

        updateSuggestions("")
    }

    override fun onDestroy() {

        isDeleting = false

        deleteHandler.removeCallbacks(
            deleteRunnable
        )

        keyboardView = null
        suggestionContainer = null
        keysContainer = null

        super.onDestroy()
    }
}
