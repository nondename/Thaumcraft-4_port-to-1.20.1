# Run with PowerShell 7. Checks the modern resources actually packaged by Gradle.
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$assets = Join-Path $root 'mod/src/modern/resources/assets/thaumcraft'
$languages = @{}
foreach ($locale in @('en_us', 'ru_ru')) {
    $json = Get-Content (Join-Path $assets "lang/$locale.json") -Raw -Encoding utf8
    $document = [System.Text.Json.JsonDocument]::Parse($json)
    try {
        $keys = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
        foreach ($property in $document.RootElement.EnumerateObject()) {
            if (-not $keys.Add($property.Name)) { throw "Duplicate $locale key: $($property.Name)" }
            if ($property.Value.ValueKind -ne [System.Text.Json.JsonValueKind]::String -or
                [string]::IsNullOrWhiteSpace($property.Value.GetString())) { throw "Empty/non-string $locale key: $($property.Name)" }
        }
    } finally { $document.Dispose() }
    $languages[$locale] = $json | ConvertFrom-Json -AsHashtable
}
foreach ($locale in $languages.Keys) {
    foreach ($key in $languages[$locale].Keys) {
        foreach ($other in $languages.Keys) {
            if (-not $languages[$other].ContainsKey($key)) { throw "Missing $other key: $key" }
        }
        # Preserve Java format parameters and the legacy dynamic wand template.
        $pattern = '%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[sdf](?![\p{L}\p{N}])|%CAP|%ROD|%OBJ'
        $actual = @([regex]::Matches($languages[$locale][$key], $pattern) | ForEach-Object Value) -join '|'
        $expected = @([regex]::Matches($languages.en_us[$key], $pattern) | ForEach-Object Value) -join '|'
        if ($actual -cne $expected) { throw "Format parameter mismatch: $locale $key" }
        if ($languages[$locale][$key] -match '\uFFFD') { throw "Broken UTF-8: $locale $key" }
    }
}
$tree = Get-Content (Join-Path $assets 'research/tree.json') -Raw -Encoding utf8 | ConvertFrom-Json
$references = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
foreach ($node in $tree) {
    foreach ($key in @("tc.research_name.$($node.key)", "tc.research_text.$($node.key)",
                      "tc.research_category.$($node.category)") + @($node.pages)) {
        $null = $references.Add($key)
    }
}
# Literal TC keys used in compiled modern Java. Dynamic prefixes are covered by tree checks above.
Get-ChildItem (Join-Path $root 'mod/src/modern/java') -Filter '*.java' -Recurse | ForEach-Object {
    $source = Get-Content $_.FullName -Raw -Encoding utf8
    foreach ($match in [regex]::Matches($source, '(?:translatable|I18n\.get)\("((?:tc\.|item\.thaumcraft\.|block\.thaumcraft\.|container\.thaumcraft\.|itemGroup\.)[^"\s]+)"')) {
        $key = $match.Groups[1].Value
        if (-not $key.EndsWith('.')) { $null = $references.Add($key) }
    }
}
foreach ($key in $references) {
    foreach ($locale in $languages.Keys) {
        if (-not $languages[$locale].ContainsKey($key)) { throw "Untranslated UI/book reference: $locale $key" }
    }
}
Write-Output "Validated $($languages.ru_ru.Count) EN/RU keys and $($references.Count) UI/book references; no duplicates, missing pages or format mismatches."
