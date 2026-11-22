$ErrorActionPreference='Stop'
$resourceRoot=Join-Path $PSScriptRoot '../src/main/resources'
function GenerateClinic($biome){
    $materials=@{}
    $wood=if($biome-eq'desert'){'acacia'}elseif($biome-eq'savanna'){'acacia'}else{'spruce'}
    foreach($part in @('stairs','slab','planks','fence','door')){$materials["minecraft:spruce_$part"]="minecraft:${wood}_$part"}
    if($biome-eq'desert'){
        $materials['minecraft:calcite']='minecraft:sandstone';$materials['minecraft:stone_bricks']='minecraft:cut_sandstone'
        $materials['minecraft:spruce_stairs']='minecraft:smooth_sandstone_stairs';$materials['minecraft:spruce_slab']='minecraft:smooth_sandstone_slab'
        $materials['minecraft:birch_planks']='minecraft:smooth_sandstone';$materials['minecraft:stripped_spruce_log']='minecraft:stripped_jungle_log'
    }elseif($biome-eq'savanna'){
        $materials['minecraft:calcite']='minecraft:terracotta';$materials['minecraft:birch_planks']='minecraft:acacia_planks'
        $materials['minecraft:stripped_spruce_log']='minecraft:stripped_acacia_log';$materials['minecraft:light_blue_stained_glass']='minecraft:orange_stained_glass'
    }elseif($biome-eq'snowy'){
        $materials['minecraft:calcite']='minecraft:snow_block';$materials['minecraft:stone_bricks']='minecraft:polished_andesite';$materials['minecraft:birch_planks']='minecraft:spruce_planks'
    }elseif($biome-eq'taiga'){
        $materials['minecraft:calcite']='minecraft:white_terracotta';$materials['minecraft:birch_planks']='minecraft:spruce_planks';$materials['minecraft:potted_azure_bluet']='minecraft:potted_fern'
    }
$palette=[Collections.Generic.List[object]]::new();$paletteIds=@{};$placed=@{}
function State($name,$properties=@{}){
    if($materials.ContainsKey($name)){$name=$materials[$name]}
    $signature=$name+':'+(($properties.Keys|Sort-Object|ForEach-Object {$_+'='+$properties[$_]})-join',')
    if(!$paletteIds.ContainsKey($signature)){$state=@{Name=$name};if($properties.Count){$state.Properties=$properties};$paletteIds[$signature]=$palette.Count;$palette.Add($state)}
    return [int]$paletteIds[$signature]
}
function Block($x,$y,$z,$name,$properties=@{},$nbt=$null){$block=@{pos=@([int]$x,[int]$y,[int]$z);state=(State $name $properties)};if($nbt){$block.nbt=$nbt};$placed["$x,$y,$z"]=$block}
# Clear headroom only. The two foundation layers outside the footprint must retain native ground.
for($x=0;$x-lt9;$x++){for($z=0;$z-lt13;$z++){for($y=2;$y-lt11;$y++){Block $x $y $z 'minecraft:air'}}}
for($x=1;$x-le7;$x++){for($z=1;$z-le11;$z++){Block $x 0 $z 'minecraft:stone_bricks';Block $x 1 $z 'minecraft:birch_planks'}}
# Limestone infill between timber posts, with a masonry damp course.
for($y=2;$y-le5;$y++){
    for($x=1;$x-le7;$x++){foreach($z in @(3,11)){Block $x $y $z 'minecraft:calcite'}}
    for($z=3;$z-le11;$z++){foreach($x in @(1,7)){Block $x $y $z 'minecraft:calcite'}}
}
foreach($x in @(1,7)){foreach($z in @(3,11)){for($y=1;$y-le5;$y++){Block $x $y $z 'minecraft:stripped_spruce_log' @{axis='y'}}}}
for($z=4;$z-le10;$z++){foreach($x in @(1,7)){Block $x 2 $z 'minecraft:stone_bricks'}}
# Deep window reveals, flower pots and shutter-like trapdoor sills.
foreach($z in @(5,8)){for($dz=0;$dz-le1;$dz++){foreach($x in @(1,7)){Block $x 3 ($z+$dz) 'minecraft:light_blue_stained_glass';Block $x 4 ($z+$dz) 'minecraft:light_blue_stained_glass'}}}
foreach($x in @(2,6)){foreach($z in @(3,11)){Block $x 3 $z 'minecraft:light_blue_stained_glass';Block $x 4 $z 'minecraft:light_blue_stained_glass'}}
foreach($x in @(0,8)){foreach($z in @(5,9)){Block $x 1 $z 'minecraft:spruce_planks';Block $x 2 $z 'minecraft:potted_azure_bluet'}}
# Full gables close the ends of a steep overhanging spruce roof.
for($y=5;$y-le8;$y++){$inset=$y-4;for($x=$inset;$x-le(8-$inset);$x++){foreach($z in @(3,11)){Block $x $y $z 'minecraft:calcite'}}}
foreach($z in @(3,11)){for($y=5;$y-le8;$y++){Block 4 $y $z 'minecraft:stripped_spruce_log' @{axis='y'}}}
for($z=2;$z-le12;$z++){
    for($step=0;$step-le3;$step++){Block $step (5+$step) $z 'minecraft:spruce_stairs' @{facing='east';half='bottom';shape='straight';waterlogged='false'};Block (8-$step) (5+$step) $z 'minecraft:spruce_stairs' @{facing='west';half='bottom';shape='straight';waterlogged='false'}}
    Block 4 9 $z 'minecraft:spruce_slab' @{type='bottom';waterlogged='false'}
}
# Covered porch with beam, lanterns, a modest medical banner, and an actual door frame.
foreach($x in @(1,7)){for($y=2;$y-le4;$y++){Block $x $y 1 'minecraft:spruce_fence' @{north='false';south='false';east='false';west='false';waterlogged='false'}}}
for($x=1;$x-le7;$x++){Block $x 4 1 'minecraft:stripped_spruce_log' @{axis='x'};Block $x 5 0 'minecraft:spruce_stairs' @{facing='south';half='bottom';shape='straight';waterlogged='false'};Block $x 5 1 'minecraft:spruce_planks';Block $x 5 2 'minecraft:spruce_planks'}
Block 4 2 3 'minecraft:spruce_door' @{half='lower';facing='north';hinge='left';open='false';powered='false'}
Block 4 3 3 'minecraft:spruce_door' @{half='upper';facing='north';hinge='left';open='false';powered='false'}
Block 4 4 3 'minecraft:stripped_spruce_log' @{axis='x'}
foreach($x in @(2,6)){Block $x 3 1 'minecraft:lantern' @{hanging='true';waterlogged='false'}}
Block 7 4 0 'minecraft:white_wall_banner' @{facing='north'} @{id='minecraft:banner';Patterns=@(@{Pattern='sc';Color=[int]14})}
$entryMaterial=if($biome-eq'desert'){'minecraft:smooth_sandstone'}else{'minecraft:dirt_path'}
Block 4 1 0 'minecraft:jigsaw' @{orientation='north_up'} @{id='minecraft:jigsaw';name='minecraft:building_entrance';target='minecraft:street';pool='minecraft:empty';joint='rollable';final_state=$entryMaterial}
# Interior: two beds, folded-screen divider, supplies, wash basin, desk and a narrow green runner.
foreach($z in @(6,9)){Block 2 2 $z 'minecraft:white_bed' @{part='foot';facing='east';occupied='false'};Block 3 2 $z 'minecraft:white_bed' @{part='head';facing='east';occupied='false'}}
Block 2 2 7 'minecraft:birch_trapdoor' @{facing='south';half='bottom';open='true';powered='false';waterlogged='false'}
Block 3 2 7 'minecraft:birch_trapdoor' @{facing='south';half='bottom';open='true';powered='false';waterlogged='false'}
for($z=5;$z-le10;$z++){Block 4 2 $z 'minecraft:moss_carpet'}
# The cabinet backs onto the east wall; its doors face west into the interior aisle.
Block 6 2 6 'tetanusmod:medic_cabinet' @{complete='true';half='lower';facing='west';hinge='left';open='false';powered='false'};Block 6 3 6 'tetanusmod:medic_cabinet' @{complete='true';half='upper';facing='west';hinge='left';open='false';powered='false'};Block 6 2 8 'minecraft:water_cauldron' @{level='3'}
Block 6 2 10 'minecraft:chest' @{facing='north';type='single';waterlogged='false'} @{id='minecraft:chest';LootTable='tetanusmod:chests/clinic'}
Block 6 3 10 'minecraft:brewing_stand' @{has_bottle_0='false';has_bottle_1='false';has_bottle_2='false'}
Block 2 2 4 'minecraft:spruce_stairs' @{facing='west';half='top';shape='straight';waterlogged='false'}
Block 2 3 4 'minecraft:potted_poppy';Block 6 2 4 'minecraft:bookshelf'
# Shelf-height desk faces the open aisle; players stand on the interior floor at y2.
Block 6 3 4 'tetanusmod:prescription_writer' @{facing='west'}
Block 2 3 10 'tetanusmod:clinic_notice_board' @{facing='east'}
for($x=2;$x-le6;$x++){Block $x 6 7 'minecraft:stripped_spruce_log' @{axis='x'}}
Block 5 5 7 'minecraft:lantern' @{hanging='true';waterlogged='false'}
# A little masonry chimney breaks up the roof silhouette without an oversized sign.
for($y=6;$y-le9;$y++){Block 6 $y 10 'minecraft:stone_bricks'}
Block 6 10 10 'minecraft:campfire' @{facing='north';lit='true';signal_fire='false';waterlogged='false'}
$entity=@{pos=@([double]4.5,[double]2,[double]5.5);blockPos=@([int]4,[int]2,[int]5);nbt=@{id='minecraft:villager';Health=[single]20;PersistenceRequired=[byte]1;VillagerData=@{profession='tetanusmod:medic';type=('minecraft:'+$(if($biome-eq'snowy'){'snow'}else{$biome}));level=[int]1};Xp=[int]1;Age=[int]0}}
$structure=@{DataVersion=[int]3465;size=@([int]9,[int]11,[int]13);palette=$palette.ToArray();blocks=@($placed.Keys|Sort-Object|ForEach-Object {$placed[$_]});entities=@($entity)}
& (Join-Path $PSScriptRoot 'Write-StructureNbt.ps1') -Structure $structure -Path (Join-Path $resourceRoot "data/tetanusmod/structures/village/${biome}_clinic.nbt")
$loot=@{type='minecraft:chest';pools=@(@{rolls=@{type='minecraft:uniform';min=2;max=4};entries=@(@{type='minecraft:item';name='minecraft:glass_bottle';weight=4;functions=@(@{function='minecraft:set_count';count=@{type='minecraft:uniform';min=1;max=3}})},@{type='minecraft:item';name='minecraft:honey_bottle';weight=2},@{type='minecraft:item';name='minecraft:paper';weight=3},@{type='minecraft:item';name='tetanusmod:tetanus_antitoxin';weight=1})})}
$path=Join-Path $resourceRoot 'data/tetanusmod/loot_tables/chests/clinic.json';[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path))|Out-Null;[IO.File]::WriteAllText($path,($loot|ConvertTo-Json -Depth 30)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output 'Generated the village clinic structure and balanced supply barrel.'
}
foreach($biome in @('plains','desert','savanna','snowy','taiga')){GenerateClinic $biome}
Copy-Item -LiteralPath (Join-Path $resourceRoot 'data/tetanusmod/structures/village/plains_clinic.nbt') -Destination (Join-Path $resourceRoot 'data/tetanusmod/structures/village/clinic.nbt')
