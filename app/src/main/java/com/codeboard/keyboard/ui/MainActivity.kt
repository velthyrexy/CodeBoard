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
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnEnable = findViewById<Button>(R.id.btn_enable_keyboard)
        val btnSelect = findViewById<Button>(R.id.btn_select_keyboard)
        val spinnerLanguage = findViewById<Spinner>(R.id.spinner_language)

        btnEnable.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        btnSelect.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        val languages = arrayOf("Luau", "Python", "JavaScript", "C++", "Java", "HTML/CSS")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)
        spinnerLanguage.adapter = adapter

        val sharedPrefs = getSharedPreferences("codeboard_prefs", Context.MODE_PRIVATE)
        val savedLang = sharedPrefs.getString("selected_language", "Luau")
        val savedPos = languages.indexOf(savedLang)
        if (savedPos >= 0) {
            spinnerLanguage.setSelection(savedPos)
        }

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLang = languages[position]
                sharedPrefs.edit().putString("selected_language", selectedLang).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}
