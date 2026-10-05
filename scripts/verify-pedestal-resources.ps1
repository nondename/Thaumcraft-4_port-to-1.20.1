param([string]$Repository = (Split-Path $PSScriptRoot -Parent))
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Join-Path $Repository 'mod/src/modern/resources/assets'
$model = Get-Content (Join-Path $root 'thaumcraft/models/block/arcane_pedestal.json') -Raw | ConvertFrom-Json -AsHashtable
$item = Get-Content (Join-Path $root 'thaumcraft/models/item/arcane_pedestal.json') -Raw | ConvertFrom-Json -AsHashtable
if ($item.parent -ne 'thaumcraft:block/arcane_pedestal' -or $item.ContainsKey('elements')) { throw 'Inventory must inherit the repaired world geometry' }
if ($model.render_type -ne 'minecraft:solid') { throw 'Pedestal must use the solid render layer' }
if ($model.elements.Count -ne 3) { throw 'Expected three pedestal tiers' }
$atlas = Get-Content (Join-Path $root 'minecraft/atlases/blocks.json') -Raw | ConvertFrom-Json -AsHashtable
foreach ($texture in $model.textures.Values | Select-Object -Unique) {
    if (-not ($atlas.sources | Where-Object { $_.resource -eq $texture })) { throw "Texture not explicitly stitched: $texture" }
    $filename = $texture.Replace('thaumcraft:', '')
    $original = Join-Path $Repository ('mod/thaumcraft_src/assets/thaumcraft/textures/blocks/' + (Split-Path $filename -Leaf) + '.png')
    $modern = Join-Path $root ("thaumcraft/textures/$filename.png")
    if ((Get-FileHash $original).Hash -ne (Get-FileHash $modern).Hash) { throw "Changed original pixels: $texture" }
}
$faces = 0
foreach ($element in $model.elements) {
    if ($element.faces.Count -ne 6) { throw 'Every tier must have six closed faces' }
    foreach ($face in $element.faces.Values) {
        if ($face.uv.Count -ne 4) { throw 'Explicit UV bounds required' }
        $texture = $model.textures[$face.texture.Substring(1)].Replace('thaumcraft:', '')
        $bitmap = [Drawing.Bitmap]::new((Join-Path $root "thaumcraft/textures/$texture.png"))
        try {
            for ($y = [int]$face.uv[1]; $y -lt [int]$face.uv[3]; $y++) {
                for ($x = [int]$face.uv[0]; $x -lt [int]$face.uv[2]; $x++) {
                    if ($bitmap.GetPixel($x,$y).A -ne 255) { throw "UV samples transparent pixel at $x,$y in $texture" }
                }
            }
        } finally { $bitmap.Dispose() }
        $faces++
    }
}
$particle = $model.textures.particle.Replace('thaumcraft:', '')
$bitmap = [Drawing.Bitmap]::new((Join-Path $root "thaumcraft/textures/$particle.png"))
try {
    for ($y=0; $y -lt $bitmap.Height; $y++) { for ($x=0; $x -lt $bitmap.Width; $x++) {
        if ($bitmap.GetPixel($x,$y).A -ne 255) { throw 'Particle sprite must be opaque over its entire area' }
    } }
} finally { $bitmap.Dispose() }
Write-Output "Validated $faces opaque pedestal faces, shared inventory geometry, original PNG hashes and stitched opaque particle sprite."
