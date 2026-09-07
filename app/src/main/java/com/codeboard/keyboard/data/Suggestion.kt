package com.codeboard.keyboard.data

data class Suggestion(
    val text: String,
    val type: String,
    val popularity: Int,
    val isSnippet: Boolean = false
)
