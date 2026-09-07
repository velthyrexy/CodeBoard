package com.codeboard.keyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.codeboard.keyboard.data.SuggestionEngine

class MainActivity : AppCompatActivity() {

    private val suggestionEngine = SuggestionEngine()
    private var isReturningFromSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnEnableKeyboard: Button = findViewById(R.id.btn_enable_keyboard)
        val btnSelectKeyboard: Button = findViewById(R.id.btn_select_keyboard)
        val spinnerLanguage: Spinner = findViewById(R.id.spinner_language)

        // Populate Language Selection Spinner
        val languages = suggestionEngine.getLanguages()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        spinnerLanguage.adapter = adapter

        val sharedPrefs = getSharedPreferences("codeboard_prefs", Context.MODE_PRIVATE)
        val currentLang = sharedPrefs.getString("selected_language", "Luau")
        val defaultPosition = languages.indexOf(currentLang)
        if (defaultPosition >= 0) {
            spinnerLanguage.setSelection(defaultPosition)
        }

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLang = languages[position]
                sharedPrefs.edit().putString("selected_language", selectedLang).apply()
                Toast.makeText(this@MainActivity, "Default Language: $selectedLang", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Enable Keyboard Button
        btnEnableKeyboard.setOnClickListener {
            isReturningFromSettings = true
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        // Select Keyboard Button
        btnSelectKeyboard.setOnClickListener {
            showInputMethodPicker()
        }
    }

    override fun onResume() {
        super.onResume()
        // Automatically show input method picker when returning from settings
        if (isReturningFromSettings) {
            isReturningFromSettings = false
            showInputMethodPicker()
        }
    }

    private fun showInputMethodPicker() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }
}
