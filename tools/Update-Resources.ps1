$ErrorActionPreference='Stop'
$resourceRoot=Join-Path $PSScriptRoot '../src/main/resources'
$utf8=New-Object Text.UTF8Encoding($false)
function Write-Json($relative,$value) {
    $path=Join-Path $resourceRoot $relative
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path))|Out-Null
    [IO.File]::WriteAllText($path,($value|ConvertTo-Json -Depth 40)+"`n",$utf8)
}
function Ingredient($name) { return @{item=$name} }
function Recipe($name,$pattern,$key,$count=1) {
    Write-Json "data/tetanusmod/recipes/$name.json" @{type='minecraft:crafting_shaped';category='equipment';pattern=$pattern;key=$key;result=@{item="tetanusmod:$name";count=$count}}
}
Recipe 'nail' @('IF','I ') @{I=(Ingredient 'minecraft:iron_ingot');F=(Ingredient 'minecraft:flint')} 4
Recipe 'rebar' @('I  ',' C ','  I') @{I=(Ingredient 'minecraft:iron_ingot');C=(Ingredient 'minecraft:chain')}
Recipe 'metal_pipe' @('ICI','   ') @{I=(Ingredient 'minecraft:iron_ingot');C=(Ingredient 'minecraft:copper_ingot')}
Recipe 'sharpened_rebar' @(' F','RA') @{F=(Ingredient 'minecraft:flint');R=(Ingredient 'tetanusmod:rebar');A=(Ingredient 'minecraft:amethyst_shard')}
Recipe 'caltrops' @(' N ','NSN',' N ') @{N=(Ingredient 'tetanusmod:nail');S=(Ingredient 'minecraft:string')}
Recipe 'wire_brush' @('NNN',' C ',' S ') @{N=(Ingredient 'minecraft:iron_nugget');C=(Ingredient 'minecraft:copper_ingot');S=(Ingredient 'minecraft:stick')}
Recipe 'tetanus_antitoxin' @('HCF',' B ') @{H=(Ingredient 'minecraft:honey_bottle');C=(Ingredient 'minecraft:golden_carrot');F=(Ingredient 'minecraft:fermented_spider_eye');B=(Ingredient 'minecraft:glass_bottle')}
Recipe 'tetanized_arrow' @('RNA') @{R=(Ingredient 'minecraft:rotten_flesh');N=(Ingredient 'tetanusmod:rusted_nail');A=(Ingredient 'minecraft:arrow')}
Write-Json 'data/minecraft/tags/items/arrows.json' @{replace=$false;values=@('tetanusmod:tetanized_arrow')}
Write-Json 'data/tetanusmod/recipes/bent_nail.json' @{type='minecraft:crafting_shapeless';category='equipment';ingredients=@((Ingredient 'tetanusmod:nail'),(Ingredient 'minecraft:andesite'));result=@{item='tetanusmod:bent_nail';count=1}}
foreach ($type in @('smelting','smoking','campfire_cooking')) {
    $time=200; if ($type -eq 'smoking') {$time=100}; if ($type -eq 'campfire_cooking') {$time=600}
    Write-Json "data/tetanusmod/recipes/cooked_infected_meat_$type.json" @{type="minecraft:$type";category='food';ingredient=(Ingredient 'tetanusmod:infected_meat');result='tetanusmod:cooked_infected_meat';experience=0.35;cookingtime=$time}
}
$lang=@{}; (Get-Content (Join-Path $resourceRoot 'assets/tetanusmod/lang/en_us.json') -Raw|ConvertFrom-Json).PSObject.Properties|ForEach-Object {$lang[$_.Name]=$_.Value}
$newNames=@{tetanized_arrow='Tetanized Arrow';infected_meat='Infected Meat';cooked_infected_meat='Cooked Infected Meat';rusted_iron_sword='Rusted Iron Sword';rusted_iron_pickaxe='Rusted Iron Pickaxe';rusted_iron_axe='Rusted Iron Axe';rusted_iron_shovel='Rusted Iron Shovel';rusted_iron_hoe='Rusted Iron Hoe'}
foreach ($name in $newNames.Keys) { $lang["item.tetanusmod.$name"]=$newNames[$name] }
$messages=@{
 'enchantment.tetanusmod.tetanus'='Tetanus';'item.tetanusmod.tetanus_book'='Tetanus I';'entity.tetanusmod.tetanized_wither'='Tetanized Wither';
 'tooltip.tetanusmod.rusty_short'='Inflicts Tetanus. Hold Shift for details.';'tooltip.tetanusmod.clean_short'='Can rust. Hold Shift for details.';
 'advancements.tetanusmod.root.title'='Rust & Ruin';'advancements.tetanusmod.root.description'='Iron has a darker side.';
 'advancements.tetanusmod.infected.title'='A Painful Lesson';'advancements.tetanusmod.infected.description'='Contract Tetanus.';
 'advancements.tetanusmod.rust.title'='Rusty Business';'advancements.tetanusmod.rust.description'='Make a dropped nail rust in water or rain.';
 'jei.tetanusmod.rusting'='Rusting';'jei.tetanusmod.water'='Water: %ss';'jei.tetanusmod.drop'='Drop into water and wait';'jei.tetanusmod.rain'='Exposed rain: %ss';
 'jei.tetanusmod.brush'='Hold rusty items in your other hand, then use the brush. One durability per item. A grindstone also removes rust and enchantments.';
 'jei.tetanusmod.meat'='Drops from tetanized animals. Raw meat causes Tetanus. Cooking removes the infection.'
}
foreach ($key in $messages.Keys) {$lang[$key]=$messages[$key]}
Write-Json 'assets/tetanusmod/lang/en_us.json' $lang
foreach ($name in @($newNames.Keys)+@('caltrops','rusted_caltrops')) {
    $parent='minecraft:item/generated'; if ($name -like 'rusted_iron_*') {$parent='minecraft:item/handheld'}
    Write-Json "assets/tetanusmod/models/item/$name.json" @{parent=$parent;textures=@{layer0="tetanusmod:item/$name"}}
}
function Display($icon,$title,$description,$frame='task') { return @{icon=@{item=$icon};title=@{translate=$title};description=@{translate=$description};frame=$frame;show_toast=$true;announce_to_chat=$true;hidden=$false} }
$root=Display 'tetanusmod:rebar' 'advancements.tetanusmod.root.title' 'advancements.tetanusmod.root.description'
$root.background='minecraft:textures/gui/advancements/backgrounds/stone.png'
Write-Json 'data/tetanusmod/advancements/root.json' @{display=$root;criteria=@{obtain_iron=@{trigger='minecraft:inventory_changed';conditions=@{items=@(@{items=@('minecraft:iron_ingot')})}}}}
Write-Json 'data/tetanusmod/advancements/infected.json' @{parent='tetanusmod:root';display=(Display 'tetanusmod:rusted_nail' 'advancements.tetanusmod.infected.title' 'advancements.tetanusmod.infected.description');criteria=@{get_infected=@{trigger='minecraft:effects_changed';conditions=@{effects=@{'tetanusmod:tetanus'=@{}}}}}}
Write-Json 'data/tetanusmod/advancements/rusty_business.json' @{parent='tetanusmod:root';display=(Display 'tetanusmod:rusted_nail' 'advancements.tetanusmod.rust.title' 'advancements.tetanusmod.rust.description');criteria=@{rust_a_nail=@{trigger='minecraft:impossible'}}}
$recipes=@('nail','rebar','metal_pipe','sharpened_rebar','caltrops','wire_brush','tetanus_antitoxin','tetanized_arrow','bent_nail','cooked_infected_meat_smelting','cooked_infected_meat_smoking','cooked_infected_meat_campfire_cooking')
Write-Json 'data/tetanusmod/advancements/recipes/unlock.json' @{parent='minecraft:recipes/root';criteria=@{has_iron=@{trigger='minecraft:inventory_changed';conditions=@{items=@(@{items=@('minecraft:iron_ingot')})}}};rewards=@{recipes=@($recipes|ForEach-Object {"tetanusmod:$_"})}}
Write-Json 'data/tetanusmod/recipes/tetanized_soul_sand.json' @{type='minecraft:crafting_shapeless';ingredients=@((Ingredient 'tetanusmod:rust'),(Ingredient 'minecraft:soul_sand'),(Ingredient 'minecraft:iron_ingot'));result=@{item='tetanusmod:tetanized_soul_sand';count=1}}
foreach ($name in @('rust','rusty_fishing_hook')) {
    Write-Json "assets/tetanusmod/models/item/$name.json" @{parent='minecraft:item/generated';textures=@{layer0="tetanusmod:item/$name"}}
}
Write-Json 'assets/tetanusmod/blockstates/tetanized_soul_sand.json' @{variants=@{''=@{model='tetanusmod:block/tetanized_soul_sand'}}}
Write-Json 'assets/tetanusmod/models/block/tetanized_soul_sand.json' @{parent='minecraft:block/cube_all';textures=@{all='tetanusmod:block/tetanized_soul_sand'}}
Write-Json 'assets/tetanusmod/models/item/tetanized_soul_sand.json' @{parent='tetanusmod:block/tetanized_soul_sand'}
Write-Json 'data/tetanusmod/loot_tables/blocks/tetanized_soul_sand.json' @{type='minecraft:block';pools=@(@{rolls=1;entries=@(@{type='minecraft:item';name='tetanusmod:tetanized_soul_sand'});conditions=@(@{condition='minecraft:survives_explosion'})})}
foreach ($tag in @('wither_summon_base_blocks','mineable/shovel','soul_speed_blocks','soul_fire_base_blocks')) {
    Write-Json "data/minecraft/tags/blocks/$tag.json" @{replace=$false;values=@('tetanusmod:tetanized_soul_sand')}
}
$moreMessages=@{
 'item.tetanusmod.rust'='Rust';'item.tetanusmod.rusty_fishing_hook'='Rusty Fishing Hook';'block.tetanusmod.tetanized_soul_sand'='Tetanized Soul Sand';
 'effect.tetanusmod.salmonella'='Salmonella';'subtitles.tetanusmod.vomit'='Stomach churns';
 'advancements.tetanusmod.nice_one.title'='Tetanus. Nice One!';'advancements.tetanusmod.nice_one.description'='Defeat a Tetanized Wither.';
 'advancements.tetanusmod.rusty_hook.title'='Hook, Line and Tetanus';'advancements.tetanusmod.rusty_hook.description'='Recover a rusty fishing hook from a waterlogged rod.';
 'commands.tetanusmod.summoned'='Summoned a tetanized %s';'commands.tetanusmod.tetanized'='Tetanized %s mobs';
 'commands.tetanusmod.invalid'='Choose a valid mob type and position.';'commands.tetanusmod.failed'='Could not spawn the mob.';
 'jei.tetanusmod.meat'='Tetanized animals drop only infected meat. Raw meat causes Tetanus and has a 10% chance of Salmonella. Cook it first.';
 'jei.tetanusmod.brush'='Hold rusty items in your other hand, then use the brush. Cleaning returns one Rust; each item costs one durability. A grindstone removes rust and enchantments.'
}
foreach ($key in $moreMessages.Keys) {$lang[$key]=$moreMessages[$key]}
Write-Json 'assets/tetanusmod/lang/en_us.json' $lang
Write-Json 'data/tetanusmod/advancements/nice_one.json' @{parent='tetanusmod:infected';display=(Display 'minecraft:nether_star' 'advancements.tetanusmod.nice_one.title' 'advancements.tetanusmod.nice_one.description' 'challenge');criteria=@{defeat_wither=@{trigger='minecraft:impossible'}}}
Write-Json 'data/tetanusmod/advancements/rusty_hook.json' @{parent='tetanusmod:rusty_business';display=(Display 'tetanusmod:rusty_fishing_hook' 'advancements.tetanusmod.rusty_hook.title' 'advancements.tetanusmod.rusty_hook.description');criteria=@{find_hook=@{trigger='minecraft:inventory_changed';conditions=@{items=@(@{items=@('tetanusmod:rusty_fishing_hook')})}}}}
$recipes+=@('tetanized_soul_sand')
Write-Json 'data/tetanusmod/advancements/recipes/unlock.json' @{parent='minecraft:recipes/root';criteria=@{has_iron=@{trigger='minecraft:inventory_changed';conditions=@{items=@(@{items=@('minecraft:iron_ingot')})}}};rewards=@{recipes=@($recipes|ForEach-Object {"tetanusmod:$_"})}}
$sounds=@{};(Get-Content (Join-Path $resourceRoot 'assets/tetanusmod/sounds.json') -Raw|ConvertFrom-Json).PSObject.Properties|ForEach-Object {$sounds[$_.Name]=$_.Value}
$sounds.vomit=@{subtitle='subtitles.tetanusmod.vomit';sounds=@(@{name='minecraft:entity.player.burp';type='event';pitch=0.7;volume=0.8})}
Write-Json 'assets/tetanusmod/sounds.json' $sounds
Write-Host 'Updated recipes, advancements, item models and localization.'
