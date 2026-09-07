package com.codeboard.keyboard

import android.content.Context
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CodeBoardInputMethodService : InputMethodService() {

    private lateinit var keyboardView: LinearLayout
    private lateinit var keyContainer: LinearLayout
    private lateinit var aiChatContainer: LinearLayout
    private lateinit var suggestionBar: LinearLayout
    
    private lateinit var etApiKey: EditText
    private lateinit var etPrompt: EditText
    private lateinit var tvAiResponse: TextView
    private lateinit var btnSendAi: Button
    private lateinit var btnInsertText: Button
    private lateinit var btnBackToKeyboard: Button

    private lateinit var sharedPreferences: SharedPreferences
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    private var isSymbolMode = false
    private var isShifted = false
    private var currentWord = StringBuilder()

    private val qwertyRows = arrayOf(
        arrayOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        arrayOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        arrayOf("Shift", "z", "x", "c", "v", "b", "n", "m", "DEL"),
        arrayOf("123", ",", "SPACE", ".", "AI")
    )

    private val symbolRows = arrayOf(
        arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        arrayOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/"),
        arrayOf("*", "\"", "'", ":", ";", "!", "?", "ABC", "DEL"),
        arrayOf("ABC", ",", "SPACE", ".", "AI")
    )

    private val dictionary = listOf(
        "kotlin", "android", "keyboard", "developer", "google", "gemini", 
        "artificial", "intelligence", "code", "programming", "software", 
        "merhaba", "nasılsın", "teşekkürler", "bilgisayar", "yazılım", "yapay", "zeka"
    )

    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as LinearLayout
        sharedPreferences = getSharedPreferences("CodeBoardPrefs", Context.MODE_PRIVATE)

        keyContainer = keyboardView.findViewById(R.id.keyContainer)
        aiChatContainer = keyboardView.findViewById(R.id.aiChatContainer)
        suggestionBar = keyboardView.findViewById(R.id.suggestionBar)
        
        etApiKey = keyboardView.findViewById(R.id.etApiKey)
        etPrompt = keyboardView.findViewById(R.id.etPrompt)
        tvAiResponse = keyboardView.findViewById(R.id.tvAiResponse)
        btnSendAi = keyboardView.findViewById(R.id.btnSendAi)
        btnInsertText = keyboardView.findViewById(R.id.btnInsertText)
        btnBackToKeyboard = keyboardView.findViewById(R.id.btnBackToKeyboard)

        etApiKey.setText(sharedPreferences.getString("GEMINI_API_KEY", ""))

        etApiKey.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val key = etApiKey.text.toString().trim()
                sharedPreferences.edit().putString("GEMINI_API_KEY", key).apply()
            }
        }

        btnBackToKeyboard.setOnClickListener {
            aiChatContainer.visibility = View.GONE
            keyContainer.visibility = View.VISIBLE
            suggestionBar.visibility = View.VISIBLE
        }

        btnSendAi.setOnClickListener {
            val prompt = etPrompt.text.toString().trim()
            val apiKey = etApiKey.text.toString().trim()

            if (apiKey.isEmpty()) {
                Toast.makeText(this, "Lütfen önce Gemini API Key girin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (prompt.isEmpty()) {
                Toast.makeText(this, "Lütfen bir şeyler yazın!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            tvAiResponse.text = "Yapay zeka düşünüyor..."
            callGeminiApi(apiKey, prompt)
        }

        btnInsertText.setOnClickListener {
            val responseText = tvAiResponse.text.toString()
            if (responseText.isNotEmpty() && responseText != "Yapay zeka düşünüyor...") {
                currentInputConnection?.commitText(responseText, 1)
                aiChatContainer.visibility = View.GONE
                keyContainer.visibility = View.VISIBLE
                suggestionBar.visibility = View.VISIBLE
                etPrompt.setText("")
                tvAiResponse.text = ""
            }
        }

        renderKeyboardLayout()
        updateSuggestions("")
        return keyboardView
    }

    private fun renderKeyboardLayout() {
        keyContainer.removeAllViews()
        val rows = if (isSymbolMode) symbolRows else qwertyRows

        for (rowKeys in rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }

            for (key in rowKeys) {
                val button = Button(this).apply {
                    text = if (key.length == 1 && !isSymbolMode && isShifted) key.uppercase() else key
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        if (key == "SPACE") 4f else if (key == "Shift" || key == "DEL" || key == "123" || key == "ABC" || key == "AI") 1.5f else 1f
                    ).apply {
                        setMargins(2, 2, 2, 2)
                    }
                    setOnClickListener { handleKeyPress(key) }
                }
                rowLayout.addView(button)
            }
            keyContainer.addView(rowLayout)
        }
    }

    private fun handleKeyPress(key: String) {
        val ic = currentInputConnection ?: return

        when (key) {
            "Shift" -> {
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
            "DEL" -> {
                val selectedText = ic.getSelectedText(0)
                if (!selectedText.isNullOrEmpty()) {
                    ic.commitText("", 1)
                } else {
                    ic.deleteSurroundingText(1, 0)
                }
                updateCurrentWordAndSuggest()
            }
            "SPACE" -> {
                ic.commitText(" ", 1)
                currentWord.clear()
                updateSuggestions("")
            }
            "AI" -> {
                keyContainer.visibility = View.GONE
                suggestionBar.visibility = View.GONE
                aiChatContainer.visibility = View.VISIBLE
            }
            else -> {
                val textToCommit = if (!isSymbolMode && isShifted) key.uppercase() else key
                ic.commitText(textToCommit, 1)
                
                if (isShifted) {
                    isShifted = false
                    renderKeyboardLayout()
                }
                updateCurrentWordAndSuggest()
            }
        }
    }

    private fun updateCurrentWordAndSuggest() {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(25, 0) ?: ""
        val lastSpace = textBefore.lastIndexOf(' ')
        currentWord = if (lastSpace != -1) {
            StringBuilder(textBefore.subSequence(lastSpace + 1, textBefore.length))
        } else {
            StringBuilder(textBefore)
        }
        updateSuggestions(currentWord.toString())
    }

    private fun updateSuggestions(prefix: String) {
        suggestionBar.removeAllViews()
        
        val matches = if (prefix.isEmpty()) {
            listOf("merhaba", "selam", "harika", "tamam")
        } else {
            dictionary.filter { it.startsWith(prefix.lowercase()) }.take(4)
        }

        for (word in matches) {
            val tv = TextView(this).apply {
                text = word
                setPadding(24, 12, 24, 12)
                textSize = 14f
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                setOnClickListener {
                    val ic = currentInputConnection
                    if (ic != null && currentWord.isNotEmpty()) {
                        ic.deleteSurroundingText(currentWord.length, 0)
                        ic.commitText("$word ", 1)
                        currentWord.clear()
                        updateSuggestions("")
                    }
                }
            }
            suggestionBar.addView(tv)
        }
    }

    private fun callGeminiApi(apiKey: String, prompt: String) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                }

                connection.outputStream.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                
                val responseCode = connection.responseCode
                val stream = if (responseCode == 200) connection.inputStream else connection.errorStream
                val responseString = stream.bufferedReader().use { it.readText() }

                withContext(Dispatchers.Main) {
                    if (responseCode == 200) {
                        val jsonResponse = JSONObject(responseString)
                        val candidates = jsonResponse.getJSONArray("candidates")
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        val text = parts.getJSONObject(0).getString("text")

                        tvAiResponse.text = text.trim()
                    } else {
                        tvAiResponse.text = "Hata ($responseCode): $responseString"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvAiResponse.text = "Bağlantı Hatası: ${e.localizedMessage}"
                }
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        aiChatContainer.visibility = View.GONE
        keyContainer.visibility = View.VISIBLE
        suggestionBar.visibility = View.VISIBLE
        updateSuggestions("")
    }
}
