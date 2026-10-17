$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$resourceRoot = Join-Path $PSScriptRoot '../src/main/resources'
$utf8 = New-Object System.Text.UTF8Encoding($false)
function Write-Json($relative, $value) {
    $path = Join-Path $resourceRoot $relative
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path)) | Out-Null
    [IO.File]::WriteAllText($path, ($value | ConvertTo-Json -Depth 40) + "`n", $utf8)
}
function Pixel($bitmap, $x, $y, $hex) {
    if ($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16) {
        $bitmap.SetPixel($x, $y, [Drawing.ColorTranslator]::FromHtml($hex))
    }
}
function Texture($name, $kind, $rusty) {
    $bitmap = New-Object Drawing.Bitmap(16,16)
    $dark = '#30373d'; $mid = '#747f84'; $light = '#c3ced2'; $shade = '#465058'
    if ($rusty) { $mid = '#a1572e'; $light = '#d58b47'; $shade = '#653b2c' }
    if ($kind -in 'nail','bent_nail') {
        for ($i=0; $i -lt 10; $i++) {
            $x = 4 + $i; $y = 12 - $i
            if ($kind -eq 'bent_nail' -and $i -gt 5) { $x = 9; $y = 12 - $i }
            Pixel $bitmap $x $y $dark; Pixel $bitmap ($x+1) $y $mid; Pixel $bitmap $x ($y-1) $light
        }
        for ($i=0; $i -lt 5; $i++) { Pixel $bitmap (10+$i) (1+$i) $dark; Pixel $bitmap (10+$i) (2+$i) $mid }
    } elseif ($kind -in 'rebar','sharpened_rebar','metal_pipe') {
        for ($i=0; $i -lt 12; $i++) {
            $x=2+$i; $y=14-$i
            Pixel $bitmap $x $y $dark; Pixel $bitmap $x ($y-1) $mid
            Pixel $bitmap ($x+1) $y $shade; Pixel $bitmap ($x-1) ($y-1) $dark
            if ($i % 3 -eq 0 -and $kind -ne 'metal_pipe') { Pixel $bitmap ($x-1) ($y-2) $light; Pixel $bitmap ($x+1) ($y+1) $mid }
            else { Pixel $bitmap $x ($y-2) $light }
            if ($kind -eq 'metal_pipe') { Pixel $bitmap ($x+1) ($y-1) $mid; Pixel $bitmap ($x+2) $y $dark }
        }
        if ($kind -eq 'sharpened_rebar') { Pixel $bitmap 14 1 '#dbe2e3'; Pixel $bitmap 15 0 '#edf0ee'; Pixel $bitmap 14 2 $light }
        if ($kind -eq 'metal_pipe') { Pixel $bitmap 13 1 $dark; Pixel $bitmap 14 2 '#10171b'; Pixel $bitmap 15 3 $dark }
    } elseif ($kind -eq 'wire_brush') {
        for ($i=0; $i -lt 8; $i++) { Pixel $bitmap (2+$i) (14-$i) '#38291f'; Pixel $bitmap (3+$i) (14-$i) '#93633b'; Pixel $bitmap (3+$i) (13-$i) '#c38e54' }
        for ($x=8; $x -le 13; $x++) { for ($y=3; $y -le 7; $y++) { Pixel $bitmap $x $y $dark; if (($x+$y)%2 -eq 0) { Pixel $bitmap $x $y $light } } }
        for ($i=0; $i -lt 5; $i++) { Pixel $bitmap (9+$i) 2 $light; Pixel $bitmap (9+$i) 1 $mid }
    } elseif ($kind -eq 'tetanus_antitoxin') {
        for ($x=4; $x -le 11; $x++) { for ($y=5; $y -le 14; $y++) { Pixel $bitmap $x $y '#355c64' } }
        for ($x=5; $x -le 10; $x++) { for ($y=7; $y -le 13; $y++) { Pixel $bitmap $x $y '#75b99b' } }
        for ($x=6; $x -le 9; $x++) { Pixel $bitmap $x 4 '#a4cdd0'; Pixel $bitmap $x 3 '#86b3bb'; Pixel $bitmap $x 2 '#bb9147' }
        for ($y=6; $y -le 11; $y++) { Pixel $bitmap 5 $y '#d4f0e1' }
        Pixel $bitmap 8 8 '#f0ecd1'; Pixel $bitmap 8 9 '#f0ecd1'; Pixel $bitmap 8 10 '#f0ecd1'; Pixel $bitmap 7 9 '#f0ecd1'; Pixel $bitmap 9 9 '#f0ecd1'
    } elseif ($kind -eq 'metal') {
        for ($x=0; $x -lt 16; $x++) { for ($y=0; $y -lt 16; $y++) {
            $color = $mid
            if (($x*7+$y*11)%9 -lt 3) { $color=$shade }
            if (($x*3+$y*5)%13 -eq 0) { $color=$light }
            Pixel $bitmap $x $y $color
        } }
    } elseif ($kind -eq 'effect') {
        for ($x=3; $x -le 12; $x++) { for ($y=4; $y -le 12; $y++) { Pixel $bitmap $x $y '#62372d' } }
        for ($x=4; $x -le 11; $x++) { for ($y=5; $y -le 11; $y++) { Pixel $bitmap $x $y '#ba7647' } }
        for ($x=4; $x -le 11; $x+=2) { Pixel $bitmap $x 3 '#e0ae68'; Pixel $bitmap $x 4 '#e0ae68' }
        foreach ($point in @(@(9,5),@(8,6),@(7,7),@(8,7),@(9,7),@(8,8),@(7,9),@(6,10))) { Pixel $bitmap $point[0] $point[1] '#ffe9a3' }
    }
    # Deterministic flecks break up the rust surface while preserving the silhouette.
    if ($rusty) { for ($x=0; $x -lt 16; $x++) { for ($y=0; $y -lt 16; $y++) {
        if ($bitmap.GetPixel($x,$y).A -gt 0 -and ($x*7+$y*3)%11 -eq 0) { Pixel $bitmap $x $y '#d17b37' }
    } } }
    $path = Join-Path $resourceRoot "assets/tetanusmod/textures/$name.png"
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path)) | Out-Null
    $bitmap.Save($path, [Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

$names = [ordered]@{
    nail='Nail'; rusted_nail='Rusted Nail'; rebar='Rebar'; rusted_rebar='Rusted Rebar';
    sharpened_rebar='Sharpened Rebar'; rusted_sharpened_rebar='Rusted Sharpened Rebar';
    bent_nail='Bent Nail'; rusted_bent_nail='Rusted Bent Nail'; metal_pipe='Metal Pipe'; rusted_metal_pipe='Rusted Metal Pipe';
    wire_brush='Wire Brush'; tetanus_antitoxin='Tetanus Antitoxin'; caltrops='Caltrops'; rusted_caltrops='Rusted Caltrops'
}
$lang = [ordered]@{
    'itemGroup.tetanusmod'='Rust & Ruin'; 'effect.tetanusmod.tetanus'='Tetanus';
    'subtitles.tetanusmod.spasm'='Muscles spasm';
    'subtitles.tetanusmod.tetanus_effect'='Tetanus infection';
    'tooltip.tetanusmod.damage'='%s base damage (health points)';
    'tooltip.tetanusmod.speed'='%s attacks per second';
    'tooltip.tetanusmod.durability'='Durability: %s / %s';
    'tooltip.tetanusmod.infection'='Inflicts Tetanus: random %s-%s seconds';
    'tooltip.tetanusmod.clean'='Does not cause Tetanus';
    'tooltip.tetanusmod.water'='Dropped in water: rusts after %s seconds';
    'tooltip.tetanusmod.rain'='Exposed to rain: rusts after %s seconds';
    'tooltip.tetanusmod.brush'='Use with rusty items in your other hand (1 durability/item)';
    'tooltip.tetanusmod.antitoxin'='Drink to cure Tetanus; keeps other effects';
    'tooltip.tetanusmod.trap'='Place: 1 damage per creature, once per second'
}
foreach ($name in $names.Keys) {
    if ($name -like '*caltrops') { $lang["block.tetanusmod.$name"] = $names[$name]; continue }
    $lang["item.tetanusmod.$name"] = $names[$name]
    $kind = $name -replace '^rusted_',''
    Texture "item/$name" $kind ($name -like 'rusted_*')
    $parent = 'minecraft:item/generated'
    if ($kind -in 'rebar','sharpened_rebar','metal_pipe','wire_brush') { $parent = 'minecraft:item/handheld' }
    Write-Json "assets/tetanusmod/models/item/$name.json" @{parent=$parent; textures=@{layer0="tetanusmod:item/$name"}}
}
Texture 'block/metal' 'metal' $false
Texture 'block/rusted_metal' 'metal' $true
Texture 'mob_effect/tetanus' 'effect' $false
foreach ($name in @('caltrops','rusted_caltrops')) {
    $tex='metal'; if ($name -like 'rusted_*') { $tex='rusted_metal' }
    $elements = @()
    foreach ($bounds in @(@(2,0,7,14,1,9),@(7,0,2,9,1,14),@(7,0,7,9,5,9),@(3,0,3,5,3,5),@(11,0,11,13,3,13))) {
        $faces = @{}
        foreach ($face in @('north','south','east','west','up','down')) { $faces[$face] = @{texture='#metal'} }
        $elements += @{from=@($bounds[0],$bounds[1],$bounds[2]); to=@($bounds[3],$bounds[4],$bounds[5]); faces=$faces}
    }
    Write-Json "assets/tetanusmod/models/block/$name.json" @{parent='minecraft:block/block'; textures=@{metal="tetanusmod:block/$tex"; particle="tetanusmod:block/$tex"}; elements=$elements}
    Write-Json "assets/tetanusmod/models/item/$name.json" @{parent="tetanusmod:block/$name"}
    Write-Json "assets/tetanusmod/blockstates/$name.json" @{variants=@{'waterlogged=false'=@{model="tetanusmod:block/$name"}; 'waterlogged=true'=@{model="tetanusmod:block/$name"}}}
    Write-Json "data/tetanusmod/loot_tables/blocks/$name.json" @{type='minecraft:block'; pools=@(@{rolls=1; entries=@(@{type='minecraft:item'; name="tetanusmod:$name"}); conditions=@(@{condition='minecraft:survives_explosion'})})}
}
Write-Json 'assets/tetanusmod/lang/en_us.json' $lang
Write-Json 'assets/tetanusmod/sounds.json' @{
    spasm=@{subtitle='subtitles.tetanusmod.spasm'; sounds=@(@{name='minecraft:entity.player.hurt'; type='event'; volume=0.45; pitch=0.8})};
    tetanus_effect=@{subtitle='subtitles.tetanusmod.tetanus_effect'; sounds=@(@{name='tetanusmod:tetanus_effect'; stream=$false})}
}

function Shaped($name,$pattern,$key,$count=1) {
    Write-Json "data/tetanusmod/recipes/$name.json" @{type='minecraft:crafting_shaped'; category='equipment'; pattern=$pattern; key=$key; result=@{item="tetanusmod:$name"; count=$count}}
}
function Shapeless($name,$ingredients) {
    Write-Json "data/tetanusmod/recipes/$name.json" @{type='minecraft:crafting_shapeless'; category='equipment'; ingredients=$ingredients; result=@{item="tetanusmod:$name"; count=1}}
}
$iron = @{item='minecraft:iron_ingot'}
Shaped 'nail' @('I','I') @{I=$iron} 4
Shaped 'rebar' @('I  ',' I ','  I') @{I=$iron}
Shaped 'metal_pipe' @('I','I','I') @{I=$iron}
Shaped 'caltrops' @(' N ','N N',' N ') @{N=@{item='tetanusmod:nail'}}
Shaped 'wire_brush' @('III',' S ',' S ') @{I=$iron; S=@{item='minecraft:stick'}}
Shaped 'tetanus_antitoxin' @('HC',' B') @{H=@{item='minecraft:honey_bottle'}; C=@{item='minecraft:golden_carrot'}; B=@{item='minecraft:glass_bottle'}}
Shapeless 'sharpened_rebar' @(@{item='tetanusmod:rebar'},$iron)
Shapeless 'bent_nail' @(@{item='tetanusmod:nail'},@{item='minecraft:stone'})
# Unlock all mod recipes when the player first acquires iron.
$recipeNames=@('nail','rebar','metal_pipe','caltrops','wire_brush','tetanus_antitoxin','sharpened_rebar','bent_nail')
Write-Json 'data/tetanusmod/advancements/recipes/unlock.json' @{
    parent='minecraft:recipes/root'; criteria=@{has_iron=@{trigger='minecraft:inventory_changed'; conditions=@{items=@(@{items=@('minecraft:iron_ingot')})}}};
    rewards=@{recipes=@($recipeNames | ForEach-Object { "tetanusmod:$_" })}
}
Write-Json 'data/minecraft/tags/blocks/mineable/pickaxe.json' @{replace=$false; values=@('tetanusmod:caltrops','tetanusmod:rusted_caltrops')}
Write-Host 'Generated item textures, status icon, trap models, localization, recipes, loot and sounds.'
& (Join-Path $PSScriptRoot 'Update-Resources.ps1')
& (Join-Path $PSScriptRoot 'Import-Art.ps1')
