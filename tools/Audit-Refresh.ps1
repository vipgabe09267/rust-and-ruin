param([string]$Jar='build/libs/rust-and-ruin-0.1.0-beta.jar',[string]$Report)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.Drawing
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$path=if([IO.Path]::IsPathRooted($Jar)){$Jar}else{Join-Path $root $Jar}
$zip=[IO.Compression.ZipFile]::OpenRead($path)
function Json($name){
    $entry=$zip.GetEntry($name);if(!$entry){throw "Missing refresh resource: $name"}
    $reader=[IO.StreamReader]::new($entry.Open());try{return ($reader.ReadToEnd()|ConvertFrom-Json)}finally{$reader.Dispose()}
}
try{
    foreach($id in @('medic_clinic','medic_tent')){
        $structure=Json "data/tetanusmod/worldgen/structure/$id.json"
        if($structure.type-ne"tetanusmod:$id"){throw "Incorrect structure type: $id"}
    }
    $clinic=Json 'data/tetanusmod/worldgen/structure_set/medic_clinics.json'
    $tent=Json 'data/tetanusmod/worldgen/structure_set/medic_tents.json'
    if($clinic.placement.frequency-ne0.55-or$clinic.placement.spacing-ne34-or$clinic.placement.separation-ne8-or$clinic.placement.salt-ne10387312){throw 'Clinic no longer matches the 55% vanilla village grid'}
    if($tent.placement.frequency-ne0.15-or$tent.placement.spacing-ne160-or$tent.placement.separation-ne128){throw 'Tent frequency or minimum spacing changed'}
    foreach($type in @('plains','desert','savanna','snowy','taiga')){if(!$zip.GetEntry("data/tetanusmod/structures/village/${type}_clinic.nbt")){throw "Missing themed clinic: $type"}}
    if(!$zip.GetEntry('data/tetanusmod/structures/medic_tent.nbt')){throw 'Missing camp template'}
    foreach($biome in @('plains','desert','savanna','snow','taiga','swamp','jungle')){
        if(!$zip.GetEntry("assets/tetanusmod/textures/entity/villager/profession/medic_$biome.png")){throw "Missing medic uniform: $biome"}
    }
    foreach($loot in @('clinic','medic_tent')){
        $table=Json "data/tetanusmod/loot_tables/chests/$loot.json"
        if($table.type-ne'minecraft:chest'-or!($table.pools[0].entries|Where-Object name -eq 'minecraft:golden_apple')){throw "Missing generic supply loot: $loot"}
    }
    $retired=Json 'data/tetanusmod/advancements/variant_egg.json'
    if($retired.display-or!$retired.criteria.spawn_variant){throw 'Creative-only advancement was not retired compatibly'}
    $cabinet=Json 'assets/tetanusmod/blockstates/medic_cabinet.json'
    if(@($cabinet.variants.PSObject.Properties).Count-ne64){throw 'Cabinet blockstates incomplete'}
    foreach($half in @('lower','upper')){foreach($suffix in @('','_open')){
        $model=Json "assets/tetanusmod/models/block/medic_cabinet_${half}$suffix.json"
        if($model.elements.Count-lt20){throw 'Cabinet shelves or vials missing'}
    }}
    foreach($sprite in @('veterinary_kit','iron_spear','rusted_iron_spear','trap_reset_tool')){
        $entry=$zip.GetEntry("assets/tetanusmod/textures/item/$sprite.png");if(!$entry){throw "Missing sprite: $sprite"}
        $stream=$entry.Open();try{
            $image=[Drawing.Bitmap]::new($stream);try{
                if($image.Width-ne64-or$image.Height-ne64){throw "Wrong sprite dimensions: $sprite"}
                for($i=0;$i-lt64;$i++){if($image.GetPixel($i,0).A-or$image.GetPixel($i,63).A-or$image.GetPixel(0,$i).A-or$image.GetPixel(63,$i).A){throw "Sprite touches edge: $sprite"}}
            }finally{$image.Dispose()}
        }finally{$stream.Dispose()}
    }
    $result=[pscustomobject]@{Result='PASS';Jar=$path;SHA256=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash;Checks='Native clinic/camp grids, 5 clinic templates, camp, 7 uniforms, generic loot, retired advancement, 64 cabinet variants, 4 transparent sprites'}
    $json=$result|ConvertTo-Json
    if($Report){$target=[IO.Path]::GetFullPath($Report);[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($target))|Out-Null;[IO.File]::WriteAllText($target,$json+"`n",[Text.UTF8Encoding]::new($false))}
    $json
}finally{$zip.Dispose()}
