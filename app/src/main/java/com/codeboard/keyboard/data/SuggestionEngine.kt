package com.codeboard.keyboard.data

data class Suggestion(val text: String)

class SuggestionEngine {

    private val keywordsByLanguage = mapOf(
        "Luau" to listOf(
            // Anahtar Kelimeler & Tipler
            "local", "function", "end", "then", "if", "else", "elseif", "return", 
            "for", "while", "do", "pairs", "ipairs", "nil", "true", "false", "self", 
            "repeat", "until", "break", "continue", "typeof", "type", "export", "in", 
            "not", "and", "or", "pcall", "xpcall", "setmetatable", "getmetatable",
            // Roblox API & Veri Tipleri
            "Instance.new", "Vector3.new", "Vector2.new", "CFrame.new", "UDim2.new", 
            "Color3.fromRGB", "Color3.new", "BrickColor.new", "RaycastParams.new",
            "task.wait", "task.spawn", "task.defer", "task.delay", "task.cancel",
            "game:GetService", "Workspace", "Players", "ReplicatedStorage", 
            "ServerScriptService", "ServerStorage", "TweenService", "UserInputService", 
            "RunService", "HttpService", "DataStoreService", "SoundService",
            // Math & Table & String Modülleri
            "math.abs", "math.floor", "math.ceil", "math.clamp", "math.rad", "math.deg", 
            "math.random", "math.huge", "math.min", "math.max", "math.sin", "math.cos",
            "table.insert", "table.remove", "table.find", "table.sort", "table.clear", 
            "table.clone", "table.concat", "string.sub", "string.format", "string.find", 
            "string.match", "string.gsub", "string.len", "string.lower", "string.upper", 
            "print", "warn", "error"
        ),

        "Python" to listOf(
            // Anahtar Kelimeler
            "def", "class", "import", "from", "return", "if", "elif", "else", "for", 
            "in", "while", "try", "except", "finally", "with", "as", "lambda", "yield", 
            "raise", "assert", "pass", "break", "continue", "global", "nonlocal", 
            "async", "await", "is", "not", "and", "or", "True", "False", "None",
            // Dahili Fonksiyonlar & Veri Yapıları
            "print", "len", "range", "str", "int", "float", "list", "dict", "set", 
            "tuple", "bool", "enumerate", "zip", "map", "filter", "sorted", "sum", 
            "min", "max", "abs", "round", "type", "isinstance", "hasattr", "getattr", 
            "setattr", "open", "input", "super", "property", "staticmethod", "classmethod",
            "append", "extend", "pop", "remove", "keys", "values", "items", "get"
        ),

        "JavaScript" to listOf(
            // Anahtar Kelimeler & Sözdizimi
            "const", "let", "var", "function", "return", "if", "else", "for", "while", 
            "do", "switch", "case", "break", "continue", "try", "catch", "finally", 
            "throw", "class", "extends", "super", "this", "import", "export", "default", 
            "async", "await", "yield", "typeof", "instanceof", "new", "delete", "void", 
            "in", "of", "null", "undefined", "true", "false",
            // DOM & Browser APIs
            "console.log", "console.error", "console.warn", "document.getElementById", 
            "document.querySelector", "document.querySelectorAll", "addEventListener", 
            "removeEventListener", "fetch", "setTimeout", "setInterval", "clearTimeout",
            // Built-in Nesneler & Metodlar
            "Promise.resolve", "Promise.reject", "Promise.all", "JSON.stringify", 
            "JSON.parse", "Object.keys", "Object.values", "Object.entries", "Array.from", 
            "Math.floor", "Math.random", "Math.max", "Math.min", "Math.ceil",
            "push", "pop", "shift", "unshift", "splice", "slice", "map", "filter", 
            "reduce", "forEach", "find", "includes"
        ),

        "TypeScript" to listOf(
            // TS Tipleri & Sözdizimi
            "interface", "type", "enum", "implements", "declare", "namespace", "readonly", 
            "keyof", "typeof", "never", "unknown", "any", "void", "string", "number", 
            "boolean", "symbol", "bigint", "as", "satisfies", "is", "public", "private", 
            "protected", "abstract", "override",
            // Utility Types
            "Record", "Partial", "Required", "Pick", "Omit", "Readonly", "ReturnType", 
            "InstanceType", "Promise", "Array", "Parameters",
            // JS Standartları
            "const", "let", "function", "return", "async", "await", "import", "export", 
            "console.log", "JSON.stringify", "JSON.parse"
        ),

        "C++" to listOf(
            // Türler & Anahtar Kelimeler
            "int", "char", "float", "double", "bool", "void", "auto", "const", "constexpr", 
            "static", "inline", "virtual", "override", "class", "struct", "enum", "namespace", 
            "using", "public", "private", "protected", "template", "typename", "if", "else", 
            "for", "while", "do", "switch", "case", "break", "continue", "return", "try", 
            "catch", "throw", "new", "delete", "nullptr", "true", "false",
            // STL & Preprocessor
            "#include", "#define", "#ifdef", "#ifndef", "#endif", "std::cout", "std::cin", 
            "std::endl", "std::vector", "std::string", "std::map", "std::unordered_map", 
            "std::set", "std::pair", "std::unique_ptr", "std::shared_ptr", "std::make_shared", 
            "std::make_unique", "std::move", "std::make_pair", "std::sort", "std::find", 
            "push_back", "emplace_back", "size", "clear", "begin", "end"
        ),

        "Java" to listOf(
            // Sözdizimi & Türler
            "public", "private", "protected", "class", "interface", "extends", "implements", 
            "static", "final", "abstract", "void", "int", "long", "double", "float", 
            "boolean", "char", "String", "if", "else", "for", "while", "do", "switch", 
            "case", "break", "continue", "return", "try", "catch", "finally", "throw", 
            "throws", "new", "this", "super", "null", "true", "false", "instanceof",
            // Standart Kütüphane & Metodlar
            "System.out.println", "System.err.println", "ArrayList", "HashMap", "HashSet", 
            "List", "Map", "Set", "Optional", "Objects", "Integer.parseInt", "String.format", 
            "Stream", "Collectors.toList()", "length()", "substring()", "equals()", "hashCode()"
        ),

        "C#" to listOf(
            // Sözdizimi & Türler
            "public", "private", "protected", "internal", "class", "struct", "interface", 
            "enum", "void", "int", "string", "bool", "double", "float", "var", "object", 
            "if", "else", "switch", "case", "for", "foreach", "while", "do", "return", 
            "break", "continue", "try", "catch", "finally", "throw", "using", "namespace", 
            "async", "await", "get", "set", "null", "true", "false", "is", "as", "override", 
            "virtual", "sealed", "partial",
            // System & LINQ
            "Console.WriteLine", "Console.ReadLine", "List", "Dictionary", "Task.Run", 
            "Enumerable", "Convert.ToInt32", "String.IsNullOrEmpty", "Math.Max", "Math.Min", 
            "Where", "Select", "FirstOrDefault", "ToList", "ToArray"
        ),

        "Rust" to listOf(
            // Anahtar Kelimeler & Sözdizimi
            "fn", "let", "mut", "const", "static", "struct", "enum", "impl", "trait", 
            "pub", "use", "mod", "match", "if", "else", "loop", "while", "for", "in", 
            "return", "break", "continue", "type", "async", "await", "where", "self", 
            "Self", "super", "crate", "ref", "unsafe", "move",
            // Standart Tipler, Makrolar & Metodlar
            "println!", "eprintln!", "format!", "vec!", "panic!", "Option", "Result", 
            "Some", "None", "Ok", "Err", "String", "Vec", "HashMap", "Box", "Rc", 
            "Arc", "Mutex", "unwrap()", "expect()", "clone()", "as_str()", "collect()"
        ),

        "Go" to listOf(
            // Anahtar Kelimeler & Dahili Fonksiyonlar
            "package", "import", "func", "var", "const", "type", "struct", "interface", 
            "if", "else", "switch", "case", "default", "for", "range", "return", "break", 
            "continue", "defer", "go", "select", "chan", "map", "nil", "true", "false", 
            "make", "len", "cap", "append", "copy", "delete", "panic", "recover",
            // Standart Paketteler
            "fmt.Println", "fmt.Printf", "fmt.Sprintf", "http.Get", "http.Post", 
            "json.Marshal", "json.Unmarshal", "time.Sleep", "time.Now"
        ),

        "HTML/CSS" to listOf(
            // HTML Etiketleri
            "<!DOCTYPE html>", "html", "head", "body", "div", "span", "h1", "h2", "h3", 
            "p", "a", "img", "button", "input", "form", "label", "select", "option", 
            "table", "tr", "td", "th", "ul", "ol", "li", "header", "footer", "nav", 
            "section", "article", "script", "style", "link", "meta",
            // CSS Özellikleri & Seçiciler
            "display", "flex", "grid", "position", "margin", "padding", "width", "height", 
            "color", "background-color", "border", "border-radius", "font-family", 
            "font-size", "font-weight", "text-align", "justify-content", "align-items", 
            "flex-direction", "box-shadow", "opacity", "z-index", "overflow", "transition", 
            "transform", "@media", ":hover", ":focus", ":active", "!important"
        )
    )

    fun getLanguages(): List<String> = keywordsByLanguage.keys.toList()

    fun getSuggestions(language: String, prefix: String, limit: Int = 15): List<Suggestion> {
        val keywords = keywordsByLanguage[language] ?: keywordsByLanguage["Luau"] ?: emptyList()
        if (prefix.isBlank()) {
            return keywords.take(limit).map { Suggestion(it) }
        }
        return keywords.filter { it.startsWith(prefix, ignoreCase = true) }
            .take(limit)
            .map { Suggestion(it) }
    }
}
