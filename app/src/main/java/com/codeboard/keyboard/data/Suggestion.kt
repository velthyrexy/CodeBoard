package com.codeboard.keyboard.data

data class Suggestion(
    val text: String,
    val type: String = "keyword",
    val popularity: Int = 0
)
