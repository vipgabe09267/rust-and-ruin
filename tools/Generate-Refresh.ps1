$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$resourceRoot=Join-Path $projectRoot 'src/main/resources'
$utf8=[Text.UTF8Encoding]::new($false)
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Json($relative,$value){$p=Join-Path $resourceRoot $relative;[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($p))|Out-Null;[IO.File]::WriteAllText($p,($value|ConvertTo-Json -Depth 60)+"`n",$utf8)}
# Native structure grids make generation repeatable, saved, and discoverable with /locate.
Json 'data/tetanusmod/tags/worldgen/biome/has_medic_clinic.json' @{replace=$false;values=@('#minecraft:has_structure/village_plains','#minecraft:has_structure/village_desert','#minecraft:has_structure/village_savanna','#minecraft:has_structure/village_snowy','#minecraft:has_structure/village_taiga')}
Json 'data/tetanusmod/tags/worldgen/biome/has_medic_tent.json' @{replace=$false;values=@('minecraft:lush_caves','minecraft:dripstone_caves')}
Json 'data/tetanusmod/worldgen/structure/medic_clinic.json' @{type='tetanusmod:medic_clinic';biomes='#tetanusmod:has_medic_clinic';step='surface_structures';spawn_overrides=@{};terrain_adaptation='beard_thin'}
Json 'data/tetanusmod/worldgen/structure/medic_tent.json' @{type='tetanusmod:medic_tent';biomes='#tetanusmod:has_medic_tent';step='underground_structures';spawn_overrides=@{};terrain_adaptation='none'}
Json 'data/tetanusmod/worldgen/structure_set/medic_clinics.json' @{structures=@(@{structure='tetanusmod:medic_clinic';weight=1});placement=@{type='minecraft:random_spread';spacing=34;separation=8;salt=10387312;frequency=0.55;frequency_reduction_method='default'}}
Json 'data/tetanusmod/worldgen/structure_set/medic_tents.json' @{structures=@(@{structure='tetanusmod:medic_tent';weight=1});placement=@{type='minecraft:random_spread';spacing=160;separation=128;salt=813260217;frequency=0.15;frequency_reduction_method='default'}}
Json 'data/tetanusmod/tags/worldgen/structure/medics.json' @{replace=$false;values=@('tetanusmod:medic_clinic','tetanusmod:medic_tent')}
function Loot($target,$tent){
    $entries=@();foreach($spec in @(@('minecraft:apple',6,2,5),@('minecraft:bread',5,2,4),@('minecraft:golden_apple',1,1,1),@('minecraft:torch',4,4,12),@('minecraft:iron_ingot',2,1,3),@('minecraft:emerald',2,1,2),@('minecraft:paper',4,2,6))){$entries+=@{type='minecraft:item';name=$spec[0];weight=$spec[1];functions=@(@{function='minecraft:set_count';count=@{type='minecraft:uniform';min=$spec[2];max=$spec[3]}})}}
    Json $target @{type='minecraft:chest';pools=@(@{rolls=@{type='minecraft:uniform';min=3;max=5};entries=$entries})}
}
Loot 'data/tetanusmod/loot_tables/chests/clinic.json' $false
Loot 'data/tetanusmod/loot_tables/chests/medic_tent.json' $true
# Thirteen-block cave camp: cloth tent, bed, medicine cabinet, chest, and native DATA marker for a guard.
$palette=[Collections.Generic.List[object]]::new();$indices=@{};$blocks=@{}
function Block($x,$y,$z,$name,$props=@{},$nbt=$null){
    $key=$name+':'+(($props.Keys|Sort-Object|ForEach-Object {$_+'='+$props[$_]})-join',')
    if(!$indices.ContainsKey($key)){$state=@{Name=$name};if($props.Count){$state.Properties=$props};$indices[$key]=$palette.Count;$palette.Add($state)}
    $b=@{pos=@([int]$x,[int]$y,[int]$z);state=[int]$indices[$key]};if($nbt){$b.nbt=$nbt};$blocks["$x,$y,$z"]=$b
}
for($x=0;$x-lt13;$x++){for($z=0;$z-lt13;$z++){for($y=0;$y-lt8;$y++){Block $x $y $z 'minecraft:air'};Block $x 0 $z $(if(($x+$z)%4-eq0){'minecraft:mossy_cobblestone'}else{'minecraft:stone'})}}
for($z=3;$z-le10;$z++){for($i=0;$i-le3;$i++){Block (2+$i) (3+$i) $z 'minecraft:white_wool';Block (9-$i) (3+$i) $z 'minecraft:white_wool'}}
foreach($x in @(2,9)){foreach($z in @(3,10)){for($y=1;$y-le3;$y++){Block $x $y $z 'minecraft:oak_fence' @{north='false';south='false';east='false';west='false';waterlogged='false'}}}}
for($x=3;$x-le8;$x++){for($y=1;$y-le3;$y++){Block $x $y 10 'minecraft:white_wool'}}
Block 5 1 8 'minecraft:white_bed' @{part='foot';facing='north';occupied='false'};Block 5 1 7 'minecraft:white_bed' @{part='head';facing='north';occupied='false'}
Block 2 1 7 'tetanusmod:medic_cabinet' @{complete='true';facing='east';half='lower';hinge='left';open='false';powered='false'};Block 2 2 7 'tetanusmod:medic_cabinet' @{complete='true';facing='east';half='upper';hinge='left';open='false';powered='false'}
Block 8 1 8 'minecraft:chest' @{facing='north';type='single';waterlogged='false'} @{id='minecraft:chest';LootTable='tetanusmod:chests/medic_tent'}
Block 5 5 5 'minecraft:lantern' @{hanging='true';waterlogged='false'};Block 8 1 5 'minecraft:water_cauldron' @{level='3'}
Block 10 1 8 'minecraft:structure_block' @{mode='data'} @{id='minecraft:structure_block';mode='DATA';metadata='guard'}
$medic=@{pos=@([double]5.5,[double]1,[double]4.5);blockPos=@([int]5,[int]1,[int]4);nbt=@{id='minecraft:villager';Health=[single]20;PersistenceRequired=[byte]1;VillagerData=@{profession='tetanusmod:medic';type='minecraft:plains';level=[int]1};Xp=[int]1;Age=[int]0}}
& (Join-Path $PSScriptRoot 'Write-StructureNbt.ps1') -Structure @{DataVersion=[int]3465;size=@([int]13,[int]8,[int]13);palette=$palette.ToArray();blocks=@($blocks.Keys|Sort-Object|ForEach-Object {$blocks[$_]});entities=@($medic)} -Path (Join-Path $resourceRoot 'data/tetanusmod/structures/medic_tent.nbt')
# Cabinet shelves and vials are actual model geometry, not a painted cube.
function Faces($texture){$out=@{};foreach($side in @('up','down','north','south','east','west')){$out[$side]=@{texture=$texture}};return $out}
function Box($bounds,$texture){return @{from=@($bounds[0],$bounds[1],$bounds[2]);to=@($bounds[3],$bounds[4],$bounds[5]);faces=(Faces $texture)}}
$variants=@{}
foreach($half in @('lower','upper')){foreach($open in @($false,$true)){
    $name='medic_cabinet_'+$half+$(if($open){'_open'}else{''});$elements=@()
    foreach($b in @(@(1,0,13,15,16,15),@(1,0,2,3,16,13),@(13,0,2,15,16,13),@(3,0,2,13,2,13),@(3,14,2,13,16,13),@(3,7,3,13,8,13))){$elements+=Box $b '#wood'}
    foreach($x in @(4,8,11)){foreach($y in @(2,8)){$elements+=Box @($x,$y,7,($x+2),($y+4),10) '#glass';$elements+=Box @(($x+0.3),($y+0.3),7.3,($x+1.7),($y+2.8),9.7) $(if(($x+$y)%2){'#green'}else{'#red'});$elements+=Box @($x,($y+4),7,($x+2),($y+4.7),10) '#cork'}}
    if($open){$elements+=Box @(1,1,1,2,15,12) '#trim';$elements+=Box @(1,2,2,2,14,11) '#glass'}
    else{$elements+=Box @(2,1,1,14,2,3) '#trim';$elements+=Box @(2,14,1,14,15,3) '#trim';$elements+=Box @(2,2,1,3,14,3) '#trim';$elements+=Box @(13,2,1,14,14,3) '#trim';$elements+=Box @(3,2,2,13,14,2.2) '#glass';$elements+=Box @(11,6,0.4,12,9,1) '#metal'}
    Json "assets/tetanusmod/models/block/$name.json" @{parent='minecraft:block/block';textures=@{wood='minecraft:block/birch_planks';trim='minecraft:block/stripped_spruce_log';glass='minecraft:block/glass';green='minecraft:block/green_concrete';red='minecraft:block/purple_concrete';cork='minecraft:block/oak_planks';metal='minecraft:block/iron_block';particle='minecraft:block/birch_planks'};elements=$elements}
    foreach($facing in @('north','east','south','west')){foreach($hinge in @('left','right')){foreach($powered in @('false','true')){$rotation=@{north=0;east=90;south=180;west=270}[$facing];$variants["facing=$facing,half=$half,hinge=$hinge,open=$($open.ToString().ToLowerInvariant()),powered=$powered"]=@{model="tetanusmod:block/$name";y=$rotation}}}}
}}
Json 'assets/tetanusmod/blockstates/medic_cabinet.json' @{variants=$variants}
Json 'assets/tetanusmod/models/item/medic_cabinet.json' @{parent='tetanusmod:block/medic_cabinet_lower';display=@{gui=@{rotation=@(30,225,0);translation=@(0,0,0);scale=@(0.75,0.75,0.75)}}}
Json 'data/tetanusmod/loot_tables/blocks/medic_cabinet.json' @{type='minecraft:block';pools=@(@{rolls=1;entries=@(@{type='minecraft:item';name='tetanusmod:medic_cabinet'});conditions=@(@{condition='minecraft:survives_explosion'},@{condition='minecraft:block_state_property';block='tetanusmod:medic_cabinet';properties=@{half='lower'}})})}
# Retain the old advancement ID for saved progress, without a Creative-only visible node.
Json 'data/tetanusmod/advancements/variant_egg.json' @{criteria=@{spawn_variant=@{trigger='minecraft:impossible'}}}
# Import generated replacements at stable game sizes, preserving all alpha and giving edges room.
foreach($name in @('veterinary_kit','iron_spear','rusted_iron_spear','trap_reset_tool')){
    $source=[Drawing.Bitmap]::FromFile((Join-Path $projectRoot "assets-source/art/refresh/$name.png"));$result=[Drawing.Bitmap]::new(64,64);$g=[Drawing.Graphics]::FromImage($result)
    try{$scale=[Math]::Min(58.0/$source.Width,58.0/$source.Height);$w=[int][Math]::Round($source.Width*$scale);$h=[int][Math]::Round($source.Height*$scale);$g.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor;$g.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half;$g.DrawImage($source,[Drawing.Rectangle]::new([int]((64-$w)/2),[int]((64-$h)/2),$w,$h));$result.Save((Join-Path $resourceRoot "assets/tetanusmod/textures/item/$name.png"),[Drawing.Imaging.ImageFormat]::Png)}finally{$g.Dispose();$result.Dispose();$source.Dispose()}
}
# Preserve the exact vanilla arrow alpha/UV layout, changing metal/wood coloration only.
$jar=Get-ChildItem (Join-Path $projectRoot '.gradle/loom-cache/minecraftMaven') -Recurse -Filter 'minecraft-merged-*.jar'|Select-Object -First 1
$zip=[IO.Compression.ZipFile]::OpenRead($jar.FullName)
try{$stream=$zip.GetEntry('assets/minecraft/textures/entity/projectiles/arrow.png').Open();$arrow=[Drawing.Bitmap]::new($stream);try{for($y=0;$y-lt$arrow.Height;$y++){for($x=0;$x-lt$arrow.Width;$x++){$c=$arrow.GetPixel($x,$y);if($c.A-eq0){continue};$arrow.SetPixel($x,$y,[Drawing.Color]::FromArgb($c.A,[Math]::Min(255,[int]($c.R*0.72+60)),[int]($c.G*0.65+16),[int]($c.B*0.50+8)))}};$arrow.Save((Join-Path $resourceRoot 'assets/tetanusmod/textures/entity/tetanized_arrow.png'),[Drawing.Imaging.ImageFormat]::Png)}finally{$arrow.Dispose();$stream.Dispose()}}finally{$zip.Dispose()}
# Uniform palettes preserve the medic's established UV map and readable cross.
$base=[Drawing.Bitmap]::FromFile((Join-Path $resourceRoot 'assets/tetanusmod/textures/entity/villager/profession/medic.png'))
try{foreach($biome in @('plains','desert','savanna','snow','taiga','swamp','jungle')){$image=[Drawing.Bitmap]::new($base);try{for($y=0;$y-lt64;$y++){for($x=0;$x-lt64;$x++){$c=$image.GetPixel($x,$y);if($c.A-eq0){continue};$tint=switch($biome){desert{@(236,211,159)}savanna{@(220,193,145)}snow{@(178,204,220)}taiga{@(184,203,176)}swamp{@(161,190,163)}jungle{@(208,214,161)}default{@(229,234,223)}};if($c.R-gt140-and$c.G-gt140-and$c.B-gt130){$image.SetPixel($x,$y,[Drawing.Color]::FromArgb($c.A,[int]($c.R*$tint[0]/229),[Math]::Min(255,[int]($c.G*$tint[1]/234)),[int]($c.B*$tint[2]/223)))}}};$image.Save((Join-Path $resourceRoot "assets/tetanusmod/textures/entity/villager/profession/medic_$biome.png"),[Drawing.Imaging.ImageFormat]::Png)}finally{$image.Dispose()}}}finally{$base.Dispose()}
# Client glove model uses its own leather material and a vanilla-style empty-slot symbol.
$leather=[Drawing.Bitmap]::new(32,32);try{for($y=0;$y-lt32;$y++){for($x=0;$x-lt32;$x++){$v=($x*5+$y*3)%11;$leather.SetPixel($x,$y,[Drawing.Color]::FromArgb(255,115+$v,75+$v,42+$v))}};$leather.Save((Join-Path $resourceRoot 'assets/tetanusmod/textures/entity/gloves.png'),[Drawing.Imaging.ImageFormat]::Png)}finally{$leather.Dispose()}
$icon=[Drawing.Bitmap]::new(16,16);try{for($y=5;$y-le12;$y++){for($x=5;$x-le10;$x++){$icon.SetPixel($x,$y,[Drawing.Color]::FromArgb(110,200,200,200))}};foreach($x in @(5,7,9)){$icon.SetPixel($x,3,[Drawing.Color]::FromArgb(110,200,200,200));$icon.SetPixel($x,4,[Drawing.Color]::FromArgb(110,200,200,200))};$icon.SetPixel(4,8,[Drawing.Color]::FromArgb(110,200,200,200));$icon.Save((Join-Path $resourceRoot 'assets/tetanusmod/textures/gui/empty_glove_slot.png'),[Drawing.Imaging.ImageFormat]::Png)}finally{$icon.Dispose()}
$lang=@{};(Get-Content (Join-Path $resourceRoot 'assets/tetanusmod/lang/en_us.json') -Raw -Encoding UTF8|ConvertFrom-Json).PSObject.Properties|ForEach-Object {$lang[$_.Name]=$_.Value}
$lang['itemGroup.tetanusmod.spawn_eggs']='Rust & Ruins Spawn Eggs'
$lang['message.tetanusmod.golem_failed']='The rust did not take hold. Each attempt has an 18% chance.'
$lang['message.tetanusmod.golem_success']='Tetanized Iron Golem: 200 health. Rusty Guardian!'
$lang['commands.tetanusmod.effect_applied']='Applied %s to %s targets.'
$lang['tooltip.tetanusmod.gloves']='192 contact exposures. Supports Unbreaking and Mending while equipped.'
$lang['jei.tetanusmod.meat']='Only permanent tetanized animals drop Infected Meat. Raw infected meat causes Salmonella, not Tetanus. Raw Rabid Meat can cause Rabies. Cooking either makes it safe.'
$lang['guide.tetanusmod.diseases.body']="Tetanus: random %s-%s seconds. Rusty metal injuries cause stiffness, spasms and movement disturbances.`n`nLockjaw Sonata blocks eating during the final 2-4 minutes; a standalone potion lasts one minute. Antitoxin cures both.`n`nRaw Infected Meat causes Salmonella: vomiting, nausea, hunger loss and mild nonlethal damage. Cook it first.`n`nRaw Rabid Meat can cause Rabies, with immediate nausea. Rabies lasts until death or Rabies Cure. Each minute removes one heart of capacity, stopping at two hearts; it never deals direct damage. Milk does not cure Rabies."
$lang['guide.tetanusmod.salvage.body']="Supported dropped metal rusts in water after five seconds, or fifteen seconds of exposed rain. Names, enchantments and durability survive conversion.`n`nOnly permanently tetanized animals drop Infected Meat. Raw infected meat causes Salmonella; raw Rabid Meat can cause Rabies. Cook both before eating.`n`nA wire brush cleans metal and returns one Rust. Use honeycomb to protect clean metal.`n`nA Veterinary Kit treats a mutated animal and produces healing particles and a sound."
$lang['guide.tetanusmod.commands.body']="Operator level 2:`n/rustandruin summon minecraft:spider ~ ~ ~`n/rustandruin tetanize @e[type=minecraft:zombie,distance=..10]`n/rustandruin locate medic`n/rustandruin locate tent`n/rustandruin locate workshop`n`nNative locate: /locate structure tetanusmod:medic_clinic or tetanusmod:medic_tent.`n`n/effect give @s tetanusmod:tetanus 300`n/effect give @s tetanusmod:rabies infinite`nShort disease names also work, such as /effect give @s rabies. Operator commands bypass vaccine prevention.`n`nPopulation inspection reads loaded chunks only."
$lang['guide.tetanusmod.prevention.body']="Vaccine: %s%% prevention for %s. Equipped gloves add %s%% rusty-contact prevention; together: %s%%.`n`nGloves have 192 durability and support Unbreaking and Mending, including repair from XP while in the glove slot. Their appearance is visible on your hands.`n`nTetanus Protection is a shield enchantment: 65 percent prevention against I, 40 percent against II. Blocking an infectious weapon with it awards Not Today.`n`nMedic clinics roll 55 percent at eligible village locations in newly generated terrain. Cave tents roll 15 percent at widely spaced eligible cave candidates, with no second tent within 2,000 blocks."
$ordered=[ordered]@{};foreach($key in ($lang.Keys|Sort-Object)){$ordered[$key]=$lang[$key]};Json 'assets/tetanusmod/lang/en_us.json' $ordered
Write-Output 'Generated clinic/tent worldgen, two-block cabinet, replacement sprites, arrow UVs, medic uniforms, glove materials and updated guide.'

