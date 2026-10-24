$ErrorActionPreference='Stop'
$resourceRoot=Join-Path $PSScriptRoot '../src/main/resources'
$utf8=[Text.UTF8Encoding]::new($false)
function Json($relative,$value){$path=Join-Path $resourceRoot $relative;[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path))|Out-Null;[IO.File]::WriteAllText($path,($value|ConvertTo-Json -Depth 40)+"`n",$utf8)}
$palette=[Collections.Generic.List[object]]::new();$indices=@{};$blocks=@{}
function Block($x,$y,$z,$name,$properties=@{},$nbt=$null){
    $key=$name+':'+(($properties.Keys|Sort-Object|ForEach-Object {$_+'='+$properties[$_]})-join',')
    if(!$indices.ContainsKey($key)){$state=@{Name=$name};if($properties.Count){$state.Properties=$properties};$indices[$key]=$palette.Count;$palette.Add($state)}
    $block=@{pos=@([int]$x,[int]$y,[int]$z);state=[int]$indices[$key]};if($nbt){$block.nbt=$nbt};$blocks["$x,$y,$z"]=$block
}
# A small ruined smithy, with a covered forge and a collapsed store room.
for($x=0;$x-lt11;$x++){for($z=0;$z-lt13;$z++){for($y=0;$y-lt8;$y++){Block $x $y $z 'minecraft:air'}}}
for($x=1;$x-le9;$x++){for($z=1;$z-le11;$z++){
    $stone=if(($x+$z)%5-eq0){'minecraft:mossy_cobblestone'}else{'minecraft:cobblestone'}
    Block $x 0 $z $stone
}}
for($y=1;$y-le3;$y++){
    for($z=3;$z-le11;$z++){foreach($x in @(1,9)){if(($x+$y+$z)%7-ne0){Block $x $y $z 'minecraft:cracked_stone_bricks'}}}
    for($x=1;$x-le9;$x++){if(($x+$y)%6-ne0){Block $x $y 11 'minecraft:mossy_stone_bricks'}}
}
foreach($x in @(1,9)){foreach($z in @(2,6,11)){for($y=1;$y-le4;$y++){Block $x $y $z 'minecraft:stripped_dark_oak_log' @{axis='y'}}}}
for($z=1;$z-le12;$z++){for($step=0;$step-le4;$step++){
    if(($z+2*$step)%9-ne0){Block $step (3+$step) $z 'minecraft:weathered_cut_copper_stairs' @{facing='east';half='bottom';shape='straight';waterlogged='false'}}
    if($z-lt8-or($z+$step)%3-eq0){Block (10-$step) (3+$step) $z 'minecraft:oxidized_cut_copper_stairs' @{facing='west';half='bottom';shape='straight';waterlogged='false'}}
};if($z-lt8){Block 5 7 $z 'minecraft:weathered_cut_copper_slab' @{type='bottom';waterlogged='false'}}}
for($x=2;$x-le8;$x++){Block $x 4 3 'minecraft:stripped_dark_oak_log' @{axis='x'}}
Block 2 1 4 'minecraft:smithing_table';Block 2 1 6 'minecraft:chipped_anvil' @{facing='east'}
Block 2 1 8 'minecraft:blast_furnace' @{facing='east';lit='false'}
for($y=2;$y-le6;$y++){Block 2 $y 8 'minecraft:bricks'}
Block 2 1 10 'minecraft:barrel' @{facing='up';open='false'} @{id='minecraft:barrel';LootTable='tetanusmod:chests/workshop'}
Block 8 1 5 'minecraft:crafting_table';Block 8 1 7 'minecraft:chain' @{axis='y';waterlogged='false'}
Block 7 1 9 'tetanusmod:rusted_bear_trap' @{closed='false'}
Block 5 1 10 'tetanusmod:rusted_caltrops'
Block 8 1 10 'minecraft:gravel';Block 8 2 10 'minecraft:cobweb';Block 7 4 10 'minecraft:cobweb'
Block 4 1 5 'minecraft:campfire' @{facing='north';lit='false';signal_fire='false';waterlogged='false'}
Block 5 3 3 'minecraft:lantern' @{hanging='true';waterlogged='false'}
$structure=@{DataVersion=[int]3465;size=@([int]11,[int]8,[int]13);palette=$palette.ToArray();blocks=@($blocks.Keys|Sort-Object|ForEach-Object {$blocks[$_]});entities=@()}
& (Join-Path $PSScriptRoot 'Write-StructureNbt.ps1') -Structure $structure -Path (Join-Path $resourceRoot 'data/tetanusmod/structures/workshop.nbt')
Json 'data/tetanusmod/worldgen/template_pool/workshop.json' @{name='tetanusmod:workshop';fallback='minecraft:empty';elements=@(@{weight=1;element=@{element_type='minecraft:single_pool_element';location='tetanusmod:workshop';processors='minecraft:empty';projection='rigid'}})}
Json 'data/tetanusmod/tags/worldgen/biome/has_workshop.json' @{replace=$false;values=@('minecraft:plains','minecraft:sunflower_plains','minecraft:forest','minecraft:birch_forest','minecraft:taiga','minecraft:meadow','minecraft:savanna','minecraft:dark_forest')}
Json 'data/tetanusmod/worldgen/structure/workshop.json' @{type='minecraft:jigsaw';biomes='#tetanusmod:has_workshop';step='surface_structures';spawn_overrides=@{};terrain_adaptation='beard_thin';start_pool='tetanusmod:workshop';size=1;start_height=@{absolute=0};project_start_to_heightmap='WORLD_SURFACE_WG';max_distance_from_center=80;use_expansion_hack=$false}
Json 'data/tetanusmod/worldgen/structure_set/workshops.json' @{structures=@(@{structure='tetanusmod:workshop';weight=1});placement=@{type='minecraft:random_spread';spacing=32;separation=16;salt=137642891}}
$entries=@()
foreach($entry in @(@('tetanusmod:rust',5,2,5),@('minecraft:copper_ingot',4,1,3),@('minecraft:string',4,2,5),@('minecraft:coal',4,1,4),@('tetanusmod:rusted_nail',3,2,5),@('tetanusmod:tetanus_antitoxin',1,1,1),@('tetanusmod:veterinary_kit',1,1,1))){
    $entries+=@{type='minecraft:item';name=$entry[0];weight=$entry[1];functions=@(@{function='minecraft:set_count';count=@{type='minecraft:uniform';min=$entry[2];max=$entry[3]}})}
}
$entries+=@{type='minecraft:item';name='tetanusmod:rusted_rebar';weight=2;functions=@(@{function='minecraft:set_damage';damage=@{type='minecraft:uniform';min=0.2;max=0.6}})}
Json 'data/tetanusmod/loot_tables/chests/workshop.json' @{type='minecraft:chest';pools=@(@{rolls=@{type='minecraft:uniform';min=3;max=5};entries=$entries})}
Write-Output 'Generated abandoned workshop, loot, biome tag and natural structure placement.'