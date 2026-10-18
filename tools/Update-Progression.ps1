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
'advancements.tetanusmod.why_did_i_eat_that.title'='Why did I eat that?';'advancements.tetanusmod.why_did_i_eat_that.description'='Contract Salmonella. That was a decision.';
'advancements.tetanusmod.strange_meat.title'='Strange Meat';'advancements.tetanusmod.strange_meat.description'='Collect infected meat from an infected animal.';
'advancements.tetanusmod.tetanized_mob.title'='Tetanized Mob';'advancements.tetanusmod.tetanized_mob.description'='Permanently tetanize a mob through a rusty attack.';
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
'tooltip.tetanusmod.potion_duration'='Tetanus: random %s-%s';
'tooltip.tetanusmod.potion_stiffness'='Muscle stiffness, spasms and movement disturbances.';
'tooltip.tetanusmod.potion_lockjaw'='Lockjaw Sonata: cannot eat for the final 2-4 min.';
'tooltip.tetanusmod.potion_cure'='Milk cannot cure it. Drink Tetanus Antitoxin.';
'tooltip.tetanusmod.potion_salmonella'='Vomiting and nausea every 10 seconds.';
'tooltip.tetanusmod.potion_sickness'='Loses hunger and takes mild, nonlethal damage.';
'jei.tetanusmod.salmonella'='Raw infected meat has a 10% chance of Salmonella: vomiting, nausea, hunger loss and mild nonlethal damage. Why did I eat that? unlocks only when Salmonella is contracted.';
'jei.tetanusmod.tetanus'='Tetanus lasts 5-10 minutes. Lockjaw Sonata lasts the final 2-4 minutes. Milk cannot cure it; drink Tetanus Antitoxin.';
'tooltip.tetanusmod.antitoxin'='Cures Tetanus and Lockjaw Sonata. Milk cannot cure them.';
'jei.tetanusmod.meat'='Animals with Tetanus drop only infected meat. Raw meat causes Tetanus and has a 10% chance of Salmonella. Cook it first.'
}

foreach($key in $more.Keys){$lang[$key]=$more[$key]}
Json 'assets/tetanusmod/lang/en_us.json' $lang
function Display($icon,$id){return @{icon=@{item=$icon};title=@{translate="advancements.tetanusmod.$id.title"};description=@{translate="advancements.tetanusmod.$id.description"};frame='task';show_toast=$true;announce_to_chat=$true;hidden=$false}}
Json 'data/tetanusmod/advancements/strange_meat.json' @{parent='tetanusmod:infected';display=(Display 'tetanusmod:infected_meat' 'strange_meat');criteria=@{obtain_infected_meat=@{trigger='minecraft:inventory_changed';conditions=@{items=@(@{items=@('tetanusmod:infected_meat')})}}}}
Json 'data/tetanusmod/advancements/why_did_i_eat_that.json' @{parent='tetanusmod:strange_meat';display=(Display 'tetanusmod:infected_meat' 'why_did_i_eat_that');criteria=@{get_salmonella=@{trigger='minecraft:effects_changed';conditions=@{effects=@{'tetanusmod:salmonella'=@{}}}}}}
Json 'data/tetanusmod/advancements/tetanized_mob.json' @{parent='tetanusmod:infected';display=(Display 'tetanusmod:rusted_nail' 'tetanized_mob');criteria=@{convert_mob=@{trigger='minecraft:impossible'}}}
$unlock=Get-Content -Encoding UTF8 (Join-Path $resourceRoot 'data/tetanusmod/advancements/recipes/unlock.json') -Raw|ConvertFrom-Json
$unlock.rewards.recipes+=@('tetanusmod:rusty_hookline','tetanusmod:tetanized_beacon')
$unlock.rewards.recipes=@($unlock.rewards.recipes|Select-Object -Unique)
Json 'data/tetanusmod/advancements/recipes/unlock.json' $unlock
Write-Host 'Updated progression recipes, brewing names, beacon data and advancements.'
Json 'data/tetanusmod/advancements/rusty_guardian.json' @{parent='tetanusmod:tetanized_mob';display=(Display 'minecraft:iron_ingot' 'rusty_guardian');criteria=@{rust_golem=@{trigger='minecraft:impossible'}}}

Json 'data/tetanusmod/advancements/lockjaw_sonata.json' @{parent='tetanusmod:infected';display=(Display 'minecraft:bone' 'lockjaw_sonata');criteria=@{get_lockjaw=@{trigger='minecraft:effects_changed';conditions=@{effects=@{'tetanusmod:lockjaw'=@{}}}}}}

Json 'data/tetanusmod/tags/blocks/tetanized_beacon_base_blocks.json' @{replace=$false;values=@('minecraft:gold_block','minecraft:netherite_block','minecraft:copper_block','minecraft:exposed_copper','minecraft:weathered_copper','minecraft:oxidized_copper','minecraft:waxed_copper_block','minecraft:waxed_exposed_copper','minecraft:waxed_weathered_copper','minecraft:waxed_oxidized_copper')}

foreach($id in @('tetanus_vaccine','protective_gloves','field_guide')) {
    Json "assets/tetanusmod/models/item/$id.json" @{parent='minecraft:item/generated';textures=@{layer0="tetanusmod:item/$id"}}
}
Json 'data/tetanusmod/recipes/tetanus_vaccine.json' @{type='minecraft:crafting_shapeless';category='misc';ingredients=@((Item 'tetanusmod:tetanus_antitoxin'),(Item 'minecraft:golden_apple'),(Item 'minecraft:glowstone_dust'),(Item 'minecraft:honey_bottle'));result=@{item='tetanusmod:tetanus_vaccine';count=1}}
Json 'data/tetanusmod/recipes/protective_gloves.json' @{type='minecraft:crafting_shaped';category='equipment';pattern=@('L L','INI',' S ');key=@{L=(Item 'minecraft:leather');I=(Item 'minecraft:iron_nugget');N=(Item 'minecraft:copper_ingot');S=(Item 'minecraft:string')};result=@{item='tetanusmod:protective_gloves';count=1}}
Json 'data/tetanusmod/recipes/field_guide.json' @{type='minecraft:crafting_shapeless';category='misc';ingredients=@((Item 'minecraft:book'),(Item 'tetanusmod:rust'),(Item 'minecraft:feather'));result=@{item='tetanusmod:field_guide';count=1}}
$unlock=Get-Content -Encoding UTF8 (Join-Path $resourceRoot 'data/tetanusmod/advancements/recipes/unlock.json') -Raw|ConvertFrom-Json
$unlock.rewards.recipes+=@('tetanusmod:tetanus_vaccine','tetanusmod:protective_gloves','tetanusmod:field_guide')
$unlock.rewards.recipes=@($unlock.rewards.recipes|Select-Object -Unique)
Json 'data/tetanusmod/advancements/recipes/unlock.json' $unlock
$extra=@{
'item.tetanusmod.tetanus_vaccine'='Tetanus Vaccine';'item.tetanusmod.protective_gloves'='Protective Gloves';'item.tetanusmod.field_guide'='Rust & Ruin Field Guide';
'effect.tetanusmod.tetanus_resistance'='Tetanus Resistance';
'tooltip.tetanusmod.vaccine'='Temporary prevention, not a cure. Drink to vaccinate.';
'tooltip.tetanusmod.gloves'='Equip in offhand. Reduces rusty contact risk; wears with exposure.';
'tooltip.tetanusmod.field_guide'='Right-click to read recipes, diseases, creatures and progression.';
'guide.tetanusmod.title'='Rust & Ruin - Field Guide';'guide.tetanusmod.scroll'='Line %s / %s - scroll to read';
'guide.tetanusmod.overview.title'='Overview';'guide.tetanusmod.overview.body'="Rust changes what iron can do. Build tools, let them weather, then choose whether to clean them or use the infection.\n\nThis guide is read-only and never consumed. Choose a chapter on the left. Use the mouse wheel or arrow buttons to scroll.\n\nThe server controls duration and protection chances. Diseases, Prevention and Creatures show its current settings. Weapon statistics and ritual examples use defaults.\n\nJEI and REI are optional. All recipes also work without a viewer.";
'guide.tetanusmod.diseases.title'='Diseases';'guide.tetanusmod.diseases.body'="Tetanus: random %s-%s seconds per infection. Symptoms develop into muscle stiffness, involuntary spasms and movement disturbances.\n\nLockjaw Sonata blocks eating continuously during the final random 2-4 minutes. Drinking remains possible. Milk cannot cure Tetanus or Lockjaw. Drink Tetanus Antitoxin, or wait for natural recovery.\n\nSalmonella: raw infected meat has a 10%% chance. Vomiting every 10 seconds causes nausea, hunger loss and mild nonlethal damage. Cook infected meat first.\n\nStrange Meat: pick up infected meat. Why did I eat that?: contract Salmonella. Eating without Salmonella does not award it.";
'guide.tetanusmod.prevention.title'='Prevention';'guide.tetanusmod.prevention.body'="Vaccine: %s%% lower Tetanus risk for %s. Drink a Tetanus Vaccine to gain Tetanus Resistance. It does not cure an existing infection or prevent Salmonella. Milk removes this beneficial protection.\n\nGloves: equip Protective Gloves in the offhand for %s%% lower rusty-contact risk. One durability per exposure; 192 durability. Main-hand or inventory gloves offer no protection. Gloves do not protect against infected food or disease potions.\n\nTogether: %s%% lower contact risk. Neither prevents ordinary weapon damage.\n\nVaccine recipe: Antitoxin + golden apple + glowstone dust + honey bottle, shapeless. Both bottles return.\n\nGloves recipe: leather in top corners; iron nugget, copper ingot, iron nugget across middle; string below center.";
'guide.tetanusmod.weapons.title'='Weapons';'guide.tetanusmod.weapons.body'="4 Nails: two vertical iron ingots, flint to the right of the upper ingot. One heart per ordinary throw; entity hits consume the nail.\n\nRebar: iron top left, chain center, iron bottom right. Half a heart on a fully charged ordinary unarmored hit.\n\nSharpened Rebar: flint upper right, rebar lower left, amethyst shard lower right.\n\nBent Nail: nail + andesite, shapeless.\n\nMetal Pipe: iron, copper, iron across a row.\n\n4 Caltrops: four nails + iron nugget, shapeless. Place as a trap.\n\nTetanized Arrow: rotten flesh, rusted nail, arrow across one row. Normal bow/crossbow ammunition; Infinity still consumes it. Tetanus I works on melee weapons.";
'guide.tetanusmod.crafting.title'='Crafting';'guide.tetanusmod.crafting.body'="Wire Brush: three iron nuggets across top, copper ingot center, stick below. Hold rusty equipment in the opposite hand and use the brush. Cleaning returns one Rust per action and charges durability per cleaned item.\n\nAntitoxin: honey bottle, golden carrot, fermented spider eye across top; glass bottle below the carrot. Drink to cure Tetanus and Lockjaw. Returns a bottle.\n\nField Guide: book + Rust + feather, shapeless.\n\nRusty Hookline: rusty fishing hook + chain + string + stick, shapeless.\n\nTetanized Soul Sand: Rust + soul sand + iron ingot, shapeless.\n\nTetanized Beacon: normal beacon recipe, substituting a Tetanized Wither Star.";
'guide.tetanusmod.brewing.title'='Brewing';'guide.tetanusmod.brewing.body'="Use a glass bottle on a cow to collect Cow Manure. One-minute collection cooldown per cow.\n\nWater Bottle + Cow Manure = Cow Manure Potion.\nCow Manure Potion + Rust = Tetanus.\nCow Manure Potion + Infected Meat = Salmonella.\nAdd gunpowder for splash versions. Dragon breath makes lingering versions.\n\nUse a normal brewing stand with blaze powder. One ingredient handles up to three bottles.\n\nTetanus bottles use the duration range shown in Diseases. Salmonella uses normal splash-distance scaling. Potion tooltips describe the actual symptoms.";
'guide.tetanusmod.creatures.title'='Creatures';'guide.tetanusmod.creatures.body'="Rusty attacks can permanently mutate animals (%s%%) or other ordinary mobs (%s%%). Prevention can stop an exposure before it rolls mutation. Withers use a separate ritual.\n\nTemporary Tetanus is an illness; permanent mutation also changes the skin and attributes. All vanilla mob types support mutation, including spiders, fish and villagers.\n\nAnimals become slower. Any infected animal, temporary or permanent, drops only 1-3 infected meat.\n\nHostile variants gain health and damage where supported, retaining normal drops plus modest salvage.\n\nUse Rust on an iron golem: 18%% chance, 200 maximum health on success. Survival attempts consume one Rust.\n\nPermanent mutation survives saves and ordinary mob conversions. Antitoxin does not remove permanent mutation.";
'guide.tetanusmod.salvage.title'='Salvage';'guide.tetanusmod.salvage.body'="Dropped metal rusts after five consecutive seconds touching water, including floating at its surface, or 15 seconds exposed to rain. A roof protects against rain. Names, enchantments and durability survive conversion.\n\nFive iron tools have rusted variants: sword, pickaxe, axe, shovel and hoe. Rusted tools have a 20 percent infection chance on melee hits.\n\nDropped fishing rods break after five seconds in water: two sticks, two string, and a 15 percent chance of a Rusty Fishing Hook.\n\nA hookline reaches 12 blocks, pulls and infects a target, and has durability plus a two-second cooldown. Solid blocks obstruct it.\n\nMutant salvage: Rust plus iron nuggets, copper, flint, string, coal or bone.\n\nA grindstone removes rust and ordinary enchantments. A brush preserves enchantments.";
'guide.tetanusmod.bosses.title'='Boss & Beacon';'guide.tetanusmod.bosses.body'="Build the normal Wither T using four Tetanized Soul Sand blocks for a 5 percent mutation chance. Replace the upper center block with netherite for 30 percent by default. All blocks are consumed; a failed roll summons an ordinary Wither.\n\nA Tetanized Wither has 600 health, a special boss bar, its normal star, a Tetanized Wither Star, Tetanus I and bonus loot. Defeating it awards Tetanus. Nice One!\n\nBrush the special star into an ordinary star, or use it in the normal beacon recipe.\n\nThe Tetanized Beacon accepts gold, full copper (including waxed/oxidized) and netherite base blocks. Payments must be netherite ingots.\n\nPrimary powers start at II; a complete pyramid offers Regeneration II or a primary upgrade to III. Clear sky access and normal beacon cycling still apply.";
'guide.tetanusmod.commands.title'='Commands';'guide.tetanusmod.commands.body'="Operator permission level 2:\n/rustandruin summon minecraft:spider ~ ~ ~\n/rustandruin tetanize @e[type=minecraft:zombie,distance=..10]\n\nCommands bypass natural mutation chances and do not target players.\n\nSounds:\n/playsound tetanusmod:tetanus_effect master @a\n/playsound tetanusmod:spasm master @a\n/playsound tetanusmod:vomit master @a\n\nBoth client and server need Rust & Ruin and Fabric API. No recipe viewer is required.\n\nConfigure the server in config/tetanusmod.json while stopped. Existing customized values are preserved.";
}
foreach($key in $extra.Keys){$lang[$key]=$extra[$key].Replace(([string][char]92+[char]92+"n"),"`n").Replace(([string][char]92+"n"),"`n")}
Json 'assets/tetanusmod/lang/en_us.json' $lang