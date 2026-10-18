$ErrorActionPreference='Stop'
$resourceRoot=Join-Path $PSScriptRoot '../src/main/resources'
$utf8=[Text.UTF8Encoding]::new($false)
function Json($relative,$value) {
    $path=Join-Path $resourceRoot $relative
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path))|Out-Null
    [IO.File]::WriteAllText($path,($value|ConvertTo-Json -Depth 40)+"`n",$utf8)
}
function Item($id){return @{item=$id}}
Json 'data/tetanusmod/recipes/caltrops.json' @{type='minecraft:crafting_shapeless';category='equipment';ingredients=@((Item 'tetanusmod:nail'),(Item 'tetanusmod:nail'),(Item 'tetanusmod:nail'),(Item 'tetanusmod:nail'),(Item 'minecraft:iron_nugget'));result=@{item='tetanusmod:caltrops';count=4}}
Json 'data/tetanusmod/recipes/rusty_hookline.json' @{type='minecraft:crafting_shapeless';category='equipment';ingredients=@((Item 'tetanusmod:rusty_fishing_hook'),(Item 'minecraft:chain'),(Item 'minecraft:string'),(Item 'minecraft:stick'));result=@{item='tetanusmod:rusty_hookline';count=1}}
Json 'data/tetanusmod/recipes/tetanized_beacon.json' @{type='minecraft:crafting_shaped';category='misc';pattern=@('GGG','GSG','OOO');key=@{G=(Item 'minecraft:glass');S=(Item 'tetanusmod:tetanized_wither_star');O=(Item 'minecraft:obsidian')};result=@{item='tetanusmod:tetanized_beacon';count=1}}
foreach($name in @('cow_manure','tetanized_wither_star','rusty_hookline')) {
    Json "assets/tetanusmod/models/item/$name.json" @{parent='minecraft:item/generated';textures=@{layer0="tetanusmod:item/$name"}}
}
Json 'assets/tetanusmod/blockstates/tetanized_beacon.json' @{variants=@{''=@{model='tetanusmod:block/tetanized_beacon'}}}
Json 'assets/tetanusmod/models/block/tetanized_beacon.json' @{parent='minecraft:block/beacon';textures=@{glass='tetanusmod:block/cracked_glass';beacon='tetanusmod:block/beacon_core';obsidian='minecraft:block/obsidian'}}
Json 'assets/tetanusmod/models/item/tetanized_beacon.json' @{parent='tetanusmod:block/tetanized_beacon'}
Json 'data/tetanusmod/loot_tables/blocks/tetanized_beacon.json' @{type='minecraft:block';pools=@(@{rolls=1;entries=@(@{type='minecraft:item';name='tetanusmod:tetanized_beacon'});conditions=@(@{condition='minecraft:survives_explosion'})})}
Json 'data/minecraft/tags/blocks/mineable/pickaxe.json' @{replace=$false;values=@('tetanusmod:caltrops','tetanusmod:rusted_caltrops','tetanusmod:tetanized_beacon')}
$lang=@{};(Get-Content -Encoding UTF8 (Join-Path $resourceRoot 'assets/tetanusmod/lang/en_us.json') -Raw|ConvertFrom-Json).PSObject.Properties|ForEach-Object{$lang[$_.Name]=$_.Value}
$more=@{
'advancements.tetanusmod.lockjaw_sonata.title'='Lockjaw Sonata';'advancements.tetanusmod.lockjaw_sonata.description'='Experience the late-stage lockjaw symptom of Tetanus.';
'effect.tetanusmod.lockjaw'='Lockjaw Sonata';'message.tetanusmod.lockjaw'='Lockjaw Sonata: you cannot eat.';
'item.tetanusmod.cow_manure'='Cow Manure';'item.tetanusmod.tetanized_wither_star'='Tetanized Wither Star';'item.tetanusmod.rusty_hookline'='Rusty Hookline';
'block.tetanusmod.tetanized_beacon'='Tetanized Beacon';
'advancements.tetanusmod.rusty_guardian.title'='Rusty Guardian';'advancements.tetanusmod.rusty_guardian.description'='Tetanize an iron golem with Rust. It now has 200 health.';
'advancements.tetanusmod.strange_meat.title'='Strange Meat';'advancements.tetanusmod.strange_meat.description'='Contract Salmonella.';
'advancements.tetanusmod.tetanized_mob.title'='Tetanized Mob';'advancements.tetanusmod.tetanized_mob.description'='Permanently tetanize a mob with a rusty projectile or hookline.';
'gui.tetanusmod.primary'='Primary II';'gui.tetanusmod.secondary'='Secondary II / III';'gui.tetanusmod.netherite_only'='Netherite only';
'message.tetanusmod.manure_wait'='This cow needs time. Try again in a minute.';
'item.minecraft.potion.effect.cow_manure'='Cow Manure Potion';'item.minecraft.potion.effect.tetanus'='Potion of Tetanus';'item.minecraft.potion.effect.salmonella'='Potion of Salmonella';
'item.minecraft.splash_potion.effect.cow_manure'='Splash Cow Manure Potion';'item.minecraft.splash_potion.effect.tetanus'='Splash Potion of Tetanus';'item.minecraft.splash_potion.effect.salmonella'='Splash Potion of Salmonella';
'item.minecraft.lingering_potion.effect.cow_manure'='Lingering Cow Manure Potion';'item.minecraft.lingering_potion.effect.tetanus'='Lingering Potion of Tetanus';'item.minecraft.lingering_potion.effect.salmonella'='Lingering Potion of Salmonella';
'tooltip.tetanusmod.hook'='Craft with chain, string and a stick to make a Rusty Hookline.';
'tooltip.tetanusmod.hookline'='Pulls and infects a creature up to 12 blocks away. 2s cooldown.';
'tooltip.tetanusmod.manure'='Collect with a glass bottle on a cow. Brew with a water bottle.';
'tooltip.tetanusmod.star'='Crafts a Tetanized Beacon. Wire-brush it into a Nether Star.';
'jei.tetanusmod.drop'='Drop in water, including the surface';
'jei.tetanusmod.meat'='Tetanized animals drop only infected meat. Raw meat causes Tetanus and has a 10% chance of Salmonella. Cook it first.'
}
foreach($key in $more.Keys){$lang[$key]=$more[$key]}
Json 'assets/tetanusmod/lang/en_us.json' $lang
function Display($icon,$id){return @{icon=@{item=$icon};title=@{translate="advancements.tetanusmod.$id.title"};description=@{translate="advancements.tetanusmod.$id.description"};frame='task';show_toast=$true;announce_to_chat=$true;hidden=$false}}
Json 'data/tetanusmod/advancements/strange_meat.json' @{parent='tetanusmod:infected';display=(Display 'tetanusmod:infected_meat' 'strange_meat');criteria=@{get_salmonella=@{trigger='minecraft:effects_changed';conditions=@{effects=@{'tetanusmod:salmonella'=@{}}}}}}
Json 'data/tetanusmod/advancements/tetanized_mob.json' @{parent='tetanusmod:infected';display=(Display 'tetanusmod:rusted_nail' 'tetanized_mob');criteria=@{convert_mob=@{trigger='minecraft:impossible'}}}
$unlock=Get-Content -Encoding UTF8 (Join-Path $resourceRoot 'data/tetanusmod/advancements/recipes/unlock.json') -Raw|ConvertFrom-Json
$unlock.rewards.recipes+=@('tetanusmod:rusty_hookline','tetanusmod:tetanized_beacon')
$unlock.rewards.recipes=@($unlock.rewards.recipes|Select-Object -Unique)
Json 'data/tetanusmod/advancements/recipes/unlock.json' $unlock
Write-Host 'Updated progression recipes, brewing names, beacon data and advancements.'
Json 'data/tetanusmod/advancements/rusty_guardian.json' @{parent='tetanusmod:tetanized_mob';display=(Display 'minecraft:iron_ingot' 'rusty_guardian');criteria=@{rust_golem=@{trigger='minecraft:impossible'}}}

Json 'data/tetanusmod/advancements/lockjaw_sonata.json' @{parent='tetanusmod:infected';display=(Display 'minecraft:bone' 'lockjaw_sonata');criteria=@{get_lockjaw=@{trigger='minecraft:effects_changed';conditions=@{effects=@{'tetanusmod:lockjaw'=@{}}}}}}

Json 'data/tetanusmod/tags/blocks/tetanized_beacon_base_blocks.json' @{replace=$false;values=@('minecraft:gold_block','minecraft:netherite_block','minecraft:copper_block','minecraft:exposed_copper','minecraft:weathered_copper','minecraft:oxidized_copper','minecraft:waxed_copper_block','minecraft:waxed_exposed_copper','minecraft:waxed_weathered_copper','minecraft:waxed_oxidized_copper')}
