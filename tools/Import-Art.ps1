$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$textureRoot=Join-Path $projectRoot 'src/main/resources/assets/tetanusmod/textures'
$atlasPath=Join-Path $projectRoot 'assets-source/art/inventory-atlas.png'
if (-not (Test-Path -LiteralPath $atlasPath)) { throw 'The inventory art atlas is missing.' }
$atlas=[Drawing.Bitmap]::FromFile($atlasPath)
function Save-Sprite($source,$rectangle,$relative,$width,$height) {
    $path=Join-Path $textureRoot $relative
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path)) | Out-Null
    $bitmap=New-Object Drawing.Bitmap($width,$height)
    $graphics=[Drawing.Graphics]::FromImage($bitmap)
    if ($relative -like 'block/*') { $graphics.Clear([Drawing.Color]::FromArgb(255,71,44,32)) }
    $graphics.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
    $graphics.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
    $graphics.DrawImage($source,([Drawing.Rectangle]::new(0,0,$width,$height)),$rectangle,[Drawing.GraphicsUnit]::Pixel)
    $graphics.Dispose()
    $bitmap.Save($path,[Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}
$names=@('nail','rusted_nail','rebar','rusted_rebar','sharpened_rebar','rusted_sharpened_rebar',
    'bent_nail','rusted_bent_nail','metal_pipe','rusted_metal_pipe','wire_brush','tetanus_antitoxin',
    'tetanized_arrow','infected_meat','cooked_infected_meat','caltrops','rusted_caltrops','rusted_iron_sword',
    'rusted_iron_pickaxe','rusted_iron_axe','rusted_iron_shovel','rusted_iron_hoe','tetanus','tetanus_book')
$cellWidth=[int]($atlas.Width/6); $cellHeight=[int]($atlas.Height/4)
for ($i=0;$i -lt $names.Count;$i++) {
    $rectangle=[Drawing.Rectangle]::new([int](($i%6)*$cellWidth),[int](([Math]::Floor($i/6))*$cellHeight),$cellWidth,$cellHeight)
    $relative="item/$($names[$i]).png"
    if ($names[$i] -eq 'tetanus') { $relative='mob_effect/tetanus.png' }
    Save-Sprite $atlas $rectangle $relative 64 64
}
$atlas.Dispose()
$skin=[Drawing.Bitmap]::FromFile((Join-Path $projectRoot 'assets-source/art/tetanized-material.png'))
$all=[Drawing.Rectangle]::new(0,0,$skin.Width,$skin.Height)
Save-Sprite $skin $all 'entity/tetanized_skin.png' 64 64
Save-Sprite $skin $all 'entity/tetanized_creeper.png' 64 32
$skin.Dispose()
# Arrow UV follows the vanilla persistent-projectile renderer's 32x32 layout.
$arrow=New-Object Drawing.Bitmap(32,32)
for ($x=0;$x -lt 32;$x++) { for ($y=0;$y -lt 16;$y++) {
    $color=[Drawing.Color]::FromArgb(255,105,65,37)
    if ($x%5 -eq 0) { $color=[Drawing.Color]::FromArgb(255,175,111,54) }
    if ($y%4 -eq 0) { $color=[Drawing.Color]::FromArgb(255,61,42,29) }
    $arrow.SetPixel($x,$y,$color)
} }
$arrow.Save((Join-Path $textureRoot 'entity/tetanized_arrow.png'),[Drawing.Imaging.ImageFormat]::Png); $arrow.Dispose()
# HUD hearts are small code-native sprites with crisp silhouettes and a worn copper highlight.
foreach ($half in @($false,$true)) {
    $heart=New-Object Drawing.Bitmap(9,9)
    $mask=@('000000000','011001100','111111110','111111110','011111100','001111000','000110000','000000000','000000000')
    for ($y=0;$y -lt 9;$y++) { for ($x=0;$x -lt 9;$x++) {
        if ($mask[$y][$x] -ne '1' -or ($half -and $x -ge 4)) { continue }
        $hex='#9e542d'; if ($x+$y -lt 5) { $hex='#dfa25e' }; if ($y -gt 4) { $hex='#653922' }
        $heart.SetPixel($x,$y,[Drawing.ColorTranslator]::FromHtml($hex))
    } }
    [IO.Directory]::CreateDirectory((Join-Path $textureRoot 'gui')) | Out-Null
    $name='rust_heart.png'; if ($half) { $name='rust_heart_half.png' }
    $heart.Save((Join-Path $textureRoot "gui/$name"),[Drawing.Imaging.ImageFormat]::Png); $heart.Dispose()
}
$salvagePath=Join-Path $projectRoot 'assets-source/art/salvage-atlas.png'
if (Test-Path -LiteralPath $salvagePath) {
    $salvage=[Drawing.Bitmap]::FromFile($salvagePath)
    $cell=[int]($salvage.Width/2)
    Save-Sprite $salvage ([Drawing.Rectangle]::new(0,0,$cell,$cell)) 'item/rust.png' 64 64
    Save-Sprite $salvage ([Drawing.Rectangle]::new($cell,0,$cell,$cell)) 'item/rusty_fishing_hook.png' 64 64
    Save-Sprite $salvage ([Drawing.Rectangle]::new($cell,$cell,$cell,$cell)) 'mob_effect/salmonella.png' 64 64
    # Crop the flat tile to its opaque edges so the placed cube has no transparent seams.
    Save-Sprite $salvage ([Drawing.Rectangle]::new(68,646,529,533)) 'block/tetanized_soul_sand.png' 64 64
    $salvage.Dispose()
}
Write-Host 'Imported inventory art, salvage art, skins, arrow texture and rusty hearts.'
