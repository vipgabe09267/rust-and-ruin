$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../src/main/resources'))
$utf8=[Text.UTF8Encoding]::new($false)
function Json($name,$value){$path=Join-Path $root $name;[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path))|Out-Null;[IO.File]::WriteAllText($path,($value|ConvertTo-Json -Depth 70)+"`n",$utf8)}
function Sprite($name){
 $source=Join-Path $PSScriptRoot "../assets-source/expansion-2026-11-14/$name.png"
 $image=[Drawing.Bitmap]::FromFile($source)
 try{$out=[Drawing.Bitmap]::new(64,64,[Drawing.Imaging.PixelFormat]::Format32bppArgb);$draw=[Drawing.Graphics]::FromImage($out)
 try{$draw.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor;$draw.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half;$draw.CompositingMode=[Drawing.Drawing2D.CompositingMode]::SourceCopy;$draw.DrawImage($image,[Drawing.Rectangle]::new(0,0,64,64))}finally{$draw.Dispose()}
 try{$out.Save((Join-Path $root "assets/tetanusmod/textures/item/$name.png"),[Drawing.Imaging.ImageFormat]::Png)}finally{$out.Dispose()}}finally{$image.Dispose()}
}
foreach($name in @('tetanus_overload_sword','medical_prescription')){Sprite $name}
Json 'assets/tetanusmod/models/item/tetanus_overload_sword.json' @{parent='minecraft:item/handheld';textures=@{layer0='tetanusmod:item/tetanus_overload_sword'}}
Json 'assets/tetanusmod/models/item/tetanized_end_crystal.json' @{parent='minecraft:item/generated';textures=@{layer0='minecraft:item/end_crystal'}}
# Preserve the vanilla black egg silhouette and UVs; restrained modelled rust chips
# distinguish the trophy without turning the entire shell into an orange iron block.
$eggParts=@()
foreach($layer in @(@(6,15,6,10,16,10),@(5,14,5,11,15,11),@(5,13,5,11,14,11),@(3,11,3,13,13,13),@(2,8,2,14,11,14),@(1,3,1,15,8,15),@(2,1,2,14,3,14),@(3,0,3,13,1,13))){
 $from=@($layer[0],$layer[1],$layer[2]);$to=@($layer[3],$layer[4],$layer[5]);$f=@{}
 foreach($side in @('north','south','east','west')){$f[$side]=@{texture='#all';uv=@($from[0],$from[1],$to[0],$to[1])}}
 foreach($side in @('up','down')){$f[$side]=@{texture='#all';uv=@($from[0],$from[2],$to[0],$to[2])}}
 $eggParts+=@{from=$from;to=$to;faces=$f}
}
# Tiny flush surface facets: no protruding spikes, changed shape or new image texture.
foreach($chip in @(@(4,4,1,1.1,1.6),@(9,5,1,1.8,.6),@(10,7,1,.7,.9),@(6,9,2,.8,1.4),@(9,11,3,.7,1),@(7,14,5,.5,.6))){
 $a=[double]$chip[0];$y=[double]$chip[1];$edge=[double]$chip[2];$w=[double]$chip[3];$h=[double]$chip[4]
 $face=@{north=@{texture='#rust';uv=@(3,4,7,8)}};$eggParts+=@{from=@($a,$y,($edge-.008));to=@(($a+$w),($y+$h),($edge-.008));faces=$face}
 $face=@{east=@{texture='#rust';uv=@(4,3,8,7)}};$eggParts+=@{from=@((16-$edge+.008),$y,$a);to=@((16-$edge+.008),($y+$h),($a+$w));faces=$face}
 $face=@{south=@{texture='#rust';uv=@(3,5,7,9)}};$eggParts+=@{from=@((16-$a-$w),$y,(16-$edge+.008));to=@((16-$a),($y+$h),(16-$edge+.008));faces=$face}
 $face=@{west=@{texture='#rust';uv=@(4,3,8,7)}};$eggParts+=@{from=@(($edge-.008),$y,(16-$a-$w));to=@(($edge-.008),($y+$h),(16-$a));faces=$face}
}
Json 'assets/tetanusmod/models/block/tetanized_dragon_egg.json' @{parent='minecraft:block/block';textures=@{all='minecraft:block/dragon_egg';rust='tetanusmod:block/rusted_iron_block';particle='minecraft:block/dragon_egg'};elements=$eggParts}
Json 'assets/tetanusmod/models/item/tetanized_dragon_egg.json' @{parent='tetanusmod:block/tetanized_dragon_egg'}
Json 'assets/tetanusmod/blockstates/tetanized_dragon_egg.json' @{variants=@{''=@{model='tetanusmod:block/tetanized_dragon_egg'}}}
function Cube($from,$to,$texture){$faces=@{};foreach($side in @('north','south','east','west','up','down')){$faces[$side]=@{texture=$texture}};return @{from=$from;to=$to;faces=$faces}}
$elements=@((Cube @(1,0,1) @(15,3,15) '#frame'),(Cube @(0,12,0) @(16,16,16) '#top'))
foreach($x in @(2,11)){foreach($z in @(2,11)){$elements+=Cube @($x,3,$z) @(($x+3),12,($z+3)) '#frame'}}
$elements+=Cube @(1,11,0) @(15,12,1) '#trim';$elements+=Cube @(1,11,15) @(15,12,16) '#trim'
Json 'assets/tetanusmod/models/block/tetanus_wither_forging_table.json' @{parent='minecraft:block/block';textures=@{frame='minecraft:block/netherite_block';top='minecraft:block/respawn_anchor_top';trim='minecraft:block/cut_copper';particle='minecraft:block/netherite_block'};elements=$elements;display=@{gui=@{rotation=@(30,225,0);translation=@(0,0,0);scale=@(.625,.625,.625)};ground=@{translation=@(0,3,0);scale=@(.25,.25,.25)};fixed=@{rotation=@(0,0,0);scale=@(.5,.5,.5)}}}
Json 'assets/tetanusmod/models/item/tetanus_wither_forging_table.json' @{parent='tetanusmod:block/tetanus_wither_forging_table'}
Json 'assets/tetanusmod/blockstates/tetanus_wither_forging_table.json' @{variants=@{''=@{model='tetanusmod:block/tetanus_wither_forging_table'}}}
Json 'data/tetanusmod/recipes/tetanus_wither_forging_table.json' @{type='minecraft:crafting_shaped';category='misc';pattern=@('NEN','BSB','NAN');key=@{N=@{item='minecraft:netherite_block'};E=@{item='minecraft:ender_eye'};B=@{item='minecraft:blaze_powder'};S=@{item='tetanusmod:tetanized_wither_star'};A=@{item='minecraft:anvil'}};result=@{item='tetanusmod:tetanus_wither_forging_table'}}
Json 'data/tetanusmod/recipes/tetanized_end_crystal.json' @{type='tetanusmod:crystal_forging';ingredients=@(@{item='minecraft:end_crystal'},@{item='tetanusmod:rust'},@{item='minecraft:netherite_ingot'});result=@{item='tetanusmod:tetanized_end_crystal';count=1}}
foreach($name in @('tetanized_dragon_egg','tetanus_wither_forging_table')){Json "data/tetanusmod/loot_tables/blocks/$name.json" @{type='minecraft:block';pools=@(@{rolls=1;entries=@(@{type='minecraft:item';name="tetanusmod:$name"});conditions=@(@{condition='minecraft:survives_explosion'})})}}
function Tag($file,$values){$path=Join-Path $root $file;$old=@();if(Test-Path $path){$old=@((Get-Content $path -Raw -Encoding UTF8|ConvertFrom-Json).values)};Json $file @{replace=$false;values=@($old+$values|Sort-Object -Unique)}}
Tag 'data/minecraft/tags/blocks/mineable/pickaxe.json' @('tetanusmod:tetanus_wither_forging_table')
Tag 'data/minecraft/tags/blocks/needs_diamond_tool.json' @('tetanusmod:tetanus_wither_forging_table')
$languagePath=Join-Path $root 'assets/tetanusmod/lang/en_us.json';$lang=Get-Content $languagePath -Raw -Encoding UTF8|ConvertFrom-Json
$entries=[ordered]@{
 'block.tetanusmod.tetanus_wither_forging_table'='Tetanus Wither Forging Table'
 'block.tetanusmod.tetanized_dragon_egg'='Tetanized Dragon Egg'
 'item.tetanusmod.tetanized_end_crystal'='Tetanized End Crystal'
 'item.tetanusmod.tetanus_overload_sword'='Tetanus Overload Sword'
 'entity.tetanusmod.tetanized_end_crystal'='Tetanized End Crystal'
 'entity.tetanusmod.tetanized_ender_dragon'='Tetanized Ender Dragon'
 'subtitles.tetanusmod.dragon_defeated'='Tetanus. Nice One! (echoes)'
 'entity.tetanusmod.tetanized_wither_minion'='Tetanized Wither Minion'
 'gui.tetanusmod.forge.hint'='Forge one Tetanized End Crystal.'
 'jei.tetanusmod.forging_use'='Place four special crystals around the End exit portal to summon the Tetanized Dragon. Mixed crystals summon a normal dragon.'
 'tooltip.tetanusmod.overload_repair'='Repair at an anvil with Netherite Blocks, or use Mending.'
 'advancements.tetanusmod.dragon_summoned.title'='Tetanus, Again?'
 'advancements.tetanusmod.dragon_summoned.description'='Witness four Tetanized End Crystals summon the infected dragon on the central End island.'
 'advancements.tetanusmod.dragon_defeated.title'='The End of Tetanus?'
 'advancements.tetanusmod.dragon_defeated.description'='Deal at least 20 damage to a Tetanized Dragon and help defeat it.'
 'advancements.tetanusmod.forge_crystal.title'='Forged in Infection'
 'advancements.tetanusmod.forge_crystal.description'='Forge an End Crystal with Rust and a Netherite Ingot at a Tetanus Wither Forging Table.'
 'guide.tetanusmod.bosses.title'='Tetanized bosses'
 'guide.tetanusmod.bosses.body'='Four Tetanized Soul Sand blocks roll a 75% Tetanized Wither chance. A netherite block in the top center raises this to 100%. The boss has 650 HP, drops real XP and one Tetanus book: 50% II, otherwise I. It summons up to two 20-HP minions every 30-60 seconds. Forge four special End Crystals (one normal crystal + Rust + a netherite ingot each), then place all four around the End exit portal to summon the 1100-HP Tetanized Dragon. Mixed crystals summon an ordinary dragon. Each victory drops one shared Overload Sword and placeable infected egg; repeat the ritual for more. Sword: 12 damage, 2.3 speed, 4062 durability, Tetanus II. Repair only using netherite blocks at an anvil or Mending.'
}
foreach($entry in $entries.GetEnumerator()){$lang|Add-Member -NotePropertyName $entry.Key -NotePropertyValue $entry.Value -Force};[IO.File]::WriteAllText($languagePath,($lang|ConvertTo-Json -Depth 70)+"`n",$utf8)
foreach($row in @(@('dragon_summoned','forge_crystal','summon_dragon','tetanized_end_crystal'),@('dragon_defeated','dragon_summoned','defeat_dragon','tetanus_overload_sword'),@('forge_crystal','nice_one','forge_crystal','tetanus_wither_forging_table'))){$id=$row[0];$criteria=@{};$criteria[$row[2]]=@{trigger='minecraft:impossible'};Json "data/tetanusmod/advancements/$id.json" @{parent="tetanusmod:$($row[1])";criteria=$criteria;display=@{icon=@{item="tetanusmod:$($row[3])"};title=@{translate="advancements.tetanusmod.$id.title"};description=@{translate="advancements.tetanusmod.$id.description"};frame=$(if($id-eq'dragon_defeated'){'challenge'}else{'task'});show_toast=$true;announce_to_chat=$true;hidden=$false}}}
$unlockPath=Join-Path $root 'data/tetanusmod/advancements/recipes/unlock.json';$unlock=Get-Content $unlockPath -Raw -Encoding UTF8|ConvertFrom-Json;$unlock.rewards.recipes=@(@($unlock.rewards.recipes)+@('tetanusmod:tetanus_wither_forging_table','tetanusmod:tetanized_end_crystal')|Sort-Object -Unique);[IO.File]::WriteAllText($unlockPath,($unlock|ConvertTo-Json -Depth 70)+"`n",$utf8)
Write-Output 'Generated dragon/forge resources, recipes, sprites and progression.'
