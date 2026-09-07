package com.codeboard.keyboard.data

class SuggestionEngine {

    fun getSuggestions(prefix: String, maxCount: Int = 4): List<Suggestion> {
        if (prefix.isBlank()) {
            return LuauData.items.take(maxCount)
        }

        val lowerPrefix = prefix.lowercase()

        // 1. Prefix match
        val startsWithList = LuauData.items.filter {
            it.text.lowercase().startsWith(lowerPrefix)
        }

        // 2. Contains match if less than 4 items
        val combinedList = if (startsWithList.size < maxCount) {
            val containsList = LuauData.items.filter {
                it.text.lowercase().contains(lowerPrefix) && !it.text.lowercase().startsWith(lowerPrefix)
            }
            (startsWithList + containsList).distinctBy { it.text }
        } else {
            startsWithList
        }

        // 3. Fallback popular items if still less than 4
        val finalList = if (combinedList.size < maxCount) {
            (combinedList + LuauData.items).distinctBy { it.text }
        } else {
            combinedList
        }

        return finalList.take(maxCount)
    }
}
