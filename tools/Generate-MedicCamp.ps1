$ErrorActionPreference='Stop'
$resourceRoot=Join-Path $PSScriptRoot '../src/main/resources'
$palette=[Collections.Generic.List[object]]::new();$indices=@{};$blocks=@{};$fences=@{}
function Block($x,$y,$z,$name,$props=@{},$nbt=$null){
 $key=$name+':'+(($props.Keys|Sort-Object|ForEach-Object {$_+'='+$props[$_]})-join',')
 if(!$indices.ContainsKey($key)){$state=@{Name=$name};if($props.Count){$state.Properties=$props};$indices[$key]=$palette.Count;$palette.Add($state)}
 $entry=@{pos=@([int]$x,[int]$y,[int]$z);state=[int]$indices[$key]};if($nbt){$entry.nbt=$nbt};$blocks["$x,$y,$z"]=$entry
}
function Fence($x,$z){$fences["$x,$z"]=@([int]$x,[int]$z)}
# Fixed13x13x8 footprint; north approach x4..8 remains aligned with the cave fit.
for($x=0;$x-lt13;$x++){for($z=0;$z-lt13;$z++){for($y=1;$y-lt8;$y++){Block $x $y $z 'minecraft:air'};Block $x 0 $z $(if(($x*3+$z)%7-eq0){'minecraft:mossy_stone_bricks'}elseif(($x+$z)%3-eq0){'minecraft:polished_andesite'}else{'minecraft:stone_bricks'})}}
# A solid lower course plus connected fence rails protects villagers from reach
# through the sides. Two closed gate lines form a proper entry vestibule.
for($x=0;$x-le12;$x++){foreach($z in @(0,12)){if($z-eq0-and$x-ge5-and$x-le7){continue};Block $x 1 $z 'minecraft:stone_bricks';Fence $x $z}}
for($z=1;$z-lt12;$z++){foreach($x in @(0,12)){Block $x 1 $z 'minecraft:stone_bricks';Fence $x $z}}
foreach($x in @(1,2,3,4,8,9,10,11)){Block $x 1 2 'minecraft:stone_bricks';Fence $x 2}
foreach($z in @(0,2)){foreach($x in @(5,6,7)){Block $x 1 $z 'minecraft:oak_fence_gate' @{facing='north';in_wall='false';open='false';powered='false'}}}
foreach($key in ($fences.Keys|Sort-Object)){
 $x=$fences[$key][0];$z=$fences[$key][1];$props=@{waterlogged='false'}
 foreach($direction in @(@('north',0,-1),@('south',0,1),@('east',1,0),@('west',-1,0))){$nx=$x+$direction[1];$nz=$z+$direction[2];$props[$direction[0]]=$fences.ContainsKey("$nx,$nz").ToString().ToLowerInvariant()}
 Block $x 2 $z 'minecraft:oak_fence' $props
}
foreach($pair in @(@(0,0),@(12,0),@(0,12),@(12,12),@(4,0),@(8,0))){for($y=1;$y-le3;$y++){Block $pair[0] $y $pair[1] 'minecraft:mossy_stone_bricks'};Block $pair[0] 4 $pair[1] 'minecraft:lantern' @{hanging='false';waterlogged='false'}}
# Tall folded canvas canopy: at least three clear blocks above the floor.
for($z=5;$z-le11;$z++){for($x=1;$x-le8;$x++){$rise=[Math]::Min(2,[Math]::Min($x-1,8-$x));Block $x (4+$rise) $z $(if(($x+$z)%9-eq0){'minecraft:light_gray_wool'}else{'minecraft:white_wool'})}}
for($y=1;$y-le3;$y++){for($x=1;$x-le8;$x++){Block $x $y 11 'minecraft:white_wool'};for($z=6;$z-le10;$z++){Block 1 $y $z 'minecraft:white_wool'}}
foreach($x in @(1,8)){foreach($z in @(5,11)){for($y=1;$y-le3;$y++){Block $x $y $z 'minecraft:stripped_oak_log' @{axis='y'}}}}
foreach($x in @(3,5)){Block $x 1 9 'minecraft:white_bed' @{part='foot';facing='south';occupied='false'};Block $x 1 10 'minecraft:white_bed' @{part='head';facing='south';occupied='false'}}
Block 2 1 7 'tetanusmod:field_medic_cabinet' @{complete='true';facing='east';half='lower';hinge='left';open='false';powered='false'}
Block 2 2 7 'tetanusmod:field_medic_cabinet' @{complete='true';facing='east';half='upper';hinge='left';open='false';powered='false'}
# A low wooden desk keeps the writer above the floor and faces the clear center aisle.
Block 2 1 5 'minecraft:oak_planks'
Block 2 2 5 'tetanusmod:prescription_writer' @{facing='east'}
Block 2 2 8 'tetanusmod:clinic_notice_board' @{facing='east'}
Block 8 1 10 'minecraft:chest' @{facing='north';type='single';waterlogged='false'} @{id='minecraft:chest';LootTable='tetanusmod:chests/medic_tent'}
Block 8 1 8 'minecraft:water_cauldron' @{level='3'}
Block 6 5 7 'minecraft:lantern' @{hanging='true';waterlogged='false'}
for($z=5;$z-le10;$z++){foreach($x in @(4,6)){if(!($x-eq6-and$z-eq6)){Block $x 1 $z 'minecraft:brown_carpet'}}}
# Small hydrated, lit farm and accessible farmer workstation.
for($x=9;$x-le11;$x++){for($z=3;$z-le5;$z++){if($x-eq10-and$z-eq4){Block $x 1 $z 'minecraft:water' @{level='0'}}else{Block $x 1 $z 'minecraft:farmland' @{moisture='7'};Block $x 2 $z 'minecraft:wheat' @{age='7'}}}}
Block 8 1 4 'minecraft:composter' @{level='0'}
foreach($z in @(3,5)){Block 8 1 $z 'minecraft:stone_brick_stairs' @{facing='east';half='bottom';shape='straight';waterlogged='false'}}
for($x=9;$x-le11;$x++){Block $x 4 4 'minecraft:oak_slab' @{type='bottom';waterlogged='false'}}
Block 10 3 4 'minecraft:lantern' @{hanging='true';waterlogged='false'}
# The defender stands in the open courtyard, with full width/head clearance.
Block 10 1 8 'minecraft:structure_block' @{mode='data'} @{id='minecraft:structure_block';mode='DATA';metadata='guard'}
$medic=@{pos=@([double]5.5,[double]1,[double]6.5);blockPos=@([int]5,[int]1,[int]6);nbt=@{id='minecraft:villager';Health=[single]20;PersistenceRequired=[byte]1;RustRuinFieldMedic=[byte]1;Tags=@('rustruin_camp_medic');VillagerData=@{profession='tetanusmod:medic';type='minecraft:plains';level=[int]1};Xp=[int]1;Age=[int]0}}
$support=@{pos=@([double]7.5,[double]1,[double]4.5);blockPos=@([int]7,[int]1,[int]4);nbt=@{id='minecraft:villager';Health=[single]20;PersistenceRequired=[byte]1;Tags=@('rustruin_camp_support');VillagerData=@{profession='minecraft:farmer';type='minecraft:plains';level=[int]1};Xp=[int]1;Age=[int]0;Inventory=@(@{Slot=[byte]0;id='minecraft:wheat_seeds';Count=[byte]16},@{Slot=[byte]1;id='minecraft:bread';Count=[byte]3})}}
$structure=@{DataVersion=[int]3465;size=@([int]13,[int]8,[int]13);palette=$palette.ToArray();blocks=@($blocks.Keys|Sort-Object|ForEach-Object {$blocks[$_]});entities=@($medic,$support)}
& (Join-Path $PSScriptRoot 'Write-StructureNbt.ps1') -Structure $structure -Path (Join-Path $resourceRoot 'data/tetanusmod/structures/medic_tent.nbt')
Write-Output 'Generated fenced medic camp: two beds, field medic, support farmer, hydrated farm and clear guardian courtyard.'
