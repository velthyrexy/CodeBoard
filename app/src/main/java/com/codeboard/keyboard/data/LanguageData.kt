package com.codeboard.keyboard.data

object LuauData {
    val items = listOf(
        // Luau Keywords
        Suggestion("local", "keyword", 100),
        Suggestion("function", "keyword", 99),
        Suggestion("return", "keyword", 98),
        Suggestion("if", "keyword", 95),
        Suggestion("then", "keyword", 94),
        Suggestion("else", "keyword", 93),
        Suggestion("elseif", "keyword", 92),
        Suggestion("for", "keyword", 91),
        Suggestion("while", "keyword", 90),
        Suggestion("repeat", "keyword", 89),
        Suggestion("until", "keyword", 88),

        // Roblox Services
        Suggestion("game:GetService()", "snippet", 100),
        Suggestion("ProximityPromptService", "service", 95),
        Suggestion("TweenService", "service", 95),
        Suggestion("RunService", "service", 90),
        Suggestion("UserInputService", "service", 90),
        Suggestion("HttpService", "service", 85),
        Suggestion("ReplicatedStorage", "service", 95),
        Suggestion("ServerStorage", "service", 90),

        // Roblox Classes
        Suggestion("ProximityPrompt", "class", 95),
        Suggestion("PromptTriggered", "event", 90),
        Suggestion("Instance.new()", "snippet", 98),
        Suggestion("Instance", "class", 90),
        Suggestion("Part", "class", 85),
        Suggestion("Model", "class", 85),
        Suggestion("Folder", "class", 80),

        // Common Functions
        Suggestion("WaitForChild()", "function", 97),
        Suggestion("FindFirstChild()", "function", 96),
        Suggestion("GetChildren()", "function", 95),
        Suggestion("GetDescendants()", "function", 94),
        Suggestion("print()", "function", 95),
        Suggestion("warn()", "function", 90),
        Suggestion("task.wait()", "function", 94),
        Suggestion("task.spawn()", "function", 90),

        // Common Variable Terms
        Suggestion("prompt", "variable", 80)
    ).sortedByDescending { it.popularity }
}
