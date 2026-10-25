$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$resourceRoot=Join-Path $projectRoot 'src/main/resources'
$utf8=[Text.UTF8Encoding]::new($false)
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Json($relative,$value){$path=Join-Path $resourceRoot $relative;[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path))|Out-Null;[IO.File]::WriteAllText($path,($value|ConvertTo-Json -Depth 60)+"`n",$utf8)}
function Item($id){@{item=$id}}
function Shaped($id,$pattern,$key,$count=1){Json "data/tetanusmod/recipes/$id.json" @{type='minecraft:crafting_shaped';category='equipment';pattern=$pattern;key=$key;result=@{item="tetanusmod:$id";count=$count}}}
function Shapeless($id,$ingredients,$count=1){Json "data/tetanusmod/recipes/$id.json" @{type='minecraft:crafting_shapeless';category='misc';ingredients=$ingredients;result=@{item="tetanusmod:$id";count=$count}}}
Shaped 'iron_spear' @(' II',' CI','S  ') @{I=(Item 'minecraft:iron_ingot');C=(Item 'minecraft:chain');S=(Item 'minecraft:stick')}
Shaped 'rusted_iron_block' @(' R ','RIR',' R ') @{I=(Item 'minecraft:iron_block');R=(Item 'tetanusmod:rust')}
Shaped 'bear_trap' @('INI','IPI',' C ') @{I=(Item 'minecraft:iron_ingot');N=(Item 'tetanusmod:nail');P=(Item 'minecraft:heavy_weighted_pressure_plate');C=(Item 'minecraft:chain')}
Shaped 'trap_reset_tool' @('I I','ICI',' S ') @{I=(Item 'minecraft:iron_nugget');C=(Item 'minecraft:copper_ingot');S=(Item 'minecraft:stick')}
Shaped 'barbed_wire' @('NNN','CSC','NNN') @{N=(Item 'tetanusmod:nail');C=(Item 'minecraft:chain');S=(Item 'minecraft:string')} 2
Shaped 'medic_cabinet' @('IGI','IBI','IGI') @{I=(Item 'minecraft:iron_ingot');G=(Item 'minecraft:glass_pane');B=(Item 'minecraft:barrel')}
Shapeless 'rabies_vaccine' @((Item 'tetanusmod:tetanus_vaccine'),(Item 'tetanusmod:rabid_meat'),(Item 'minecraft:golden_carrot'))
Json 'data/tetanusmod/recipes/tetanized_arrow.json' @{type='tetanusmod:rust_arrows'}
foreach($method in @('smelting','smoking','campfire_cooking')){
    $ticks=200;if($method-eq'smoking'){$ticks=100};if($method-eq'campfire_cooking'){$ticks=600}
    Json "data/tetanusmod/recipes/cooked_rabid_meat_$method.json" @{type="minecraft:$method";category='food';ingredient=(Item 'tetanusmod:rabid_meat');result='tetanusmod:cooked_rabid_meat';experience=0.35;cookingtime=$ticks}
}
Json 'data/tetanusmod/tags/blocks/tetanized_beacon_base_blocks.json' @{replace=$false;values=@('tetanusmod:rusted_iron_block')}
Json 'data/minecraft/tags/point_of_interest_type/acquirable_job_site.json' @{replace=$false;values=@('tetanusmod:medic','tetanusmod:medic_tent')}
foreach($name in @('tetanus','rabies','rusted_nail','bear_trap','barbed_wire','disorientation')){Json "data/tetanusmod/damage_type/$name.json" @{message_id="tetanusmod.$name";scaling='never';exhaustion=0.1}}
Json 'data/minecraft/tags/damage_type/bypasses_armor.json' @{replace=$false;values=@('tetanusmod:tetanus','tetanusmod:rabies','tetanusmod:disorientation')}
Json 'data/minecraft/tags/damage_type/is_projectile.json' @{replace=$false;values=@('tetanusmod:rusted_nail')}
$names=@{iron_spear='Iron Spear';rusted_iron_spear='Rusted Iron Spear';trap_reset_tool='Trap Reset Tool';rabies_cure='Rabies Cure';rabies_vaccine='Rabies Vaccine';rabid_meat='Rabid Meat';cooked_rabid_meat='Cooked Rabid Meat'}
$blocks=@{rusted_iron_block='Rusted Iron Block';medic_cabinet='Medicine Cabinet';bear_trap='Bear Trap';rusted_bear_trap='Rusted Bear Trap';barbed_wire='Barbed Wire';rusted_barbed_wire='Rusted Barbed Wire';rust='Rust'}
$lang=@{};(Get-Content -LiteralPath (Join-Path $resourceRoot 'assets/tetanusmod/lang/en_us.json') -Encoding UTF8 -Raw|ConvertFrom-Json).PSObject.Properties|ForEach-Object {$lang[$_.Name]=$_.Value}
foreach($id in $names.Keys){$lang["item.tetanusmod.$id"]=$names[$id];$parent='minecraft:item/generated';if($id-like'*spear'){$parent='minecraft:item/handheld'};Json "assets/tetanusmod/models/item/$id.json" @{parent=$parent;textures=@{layer0="tetanusmod:item/$id"}}}
foreach($id in $blocks.Keys){$lang["block.tetanusmod.$id"]=$blocks[$id];if($id-ne'rust'){Json "assets/tetanusmod/models/item/$id.json" @{parent="tetanusmod:block/$id"}}}
$extra=@{
 'effect.tetanusmod.rabies'='Rabies';'effect.tetanusmod.rabies_resistance'='Rabies Resistance';'effect.tetanusmod.disoriented'='Disoriented';
 'entity.minecraft.villager.tetanusmod.medic'='Village Medic';'entity.minecraft.villager.medic'='Village Medic';
 'enchantment.tetanusmod.tetanus_protection'='Tetanus Protection';
 'tooltip.tetanusmod.shield_protection'='Shields only. Protection works while held.';
 'tooltip.tetanusmod.shield_protection_i'='Tetanus I: 65% infection prevention.';
 'tooltip.tetanusmod.shield_protection_ii'='Tetanus II: 40% infection prevention.';
 'item.tetanusmod.tetanus_book_ii'='Tetanus II';
 'tooltip.tetanusmod.gloves'='Right-click to equip in the glove slot. Reduces rusty contact risk.';
 'tooltip.tetanusmod.rabies_cure'='Cures Rabies and restores lost heart capacity. Sold by village medics.';
 'tooltip.tetanusmod.rabies_vaccine'='90% Rabies prevention for 20 minutes. Does not cure an existing infection.';
 'tooltip.tetanusmod.trap_reset_tool'='Use on a closed bear trap to release it and arm it again.';
 'item.tetanusmod.tetanized_spawn_egg'='Tetanized %s Spawn Egg';'item.tetanusmod.rabid_spawn_egg'='Rabid %s Spawn Egg';
 'gui.tetanusmod.primary'='Primary power';'gui.tetanusmod.secondary'='Full pyramid';'gui.tetanusmod.upgrade_iii'='Primary III';
 'gui.tetanusmod.activate'='Activate';'gui.tetanusmod.passive'='Tetanus Resistance';'gui.tetanusmod.passive_active'='while beam is active';'gui.tetanusmod.netherite_short'='Netherite';
 'jei.tetanusmod.batch_smithing'='Rust Arrow Smithing';'jei.tetanusmod.batch_hint'='1 Rust coats up to 64 arrows';'jei.tetanusmod.no_template'='Leave the template slot empty';
 'jei.tetanusmod.meat'='Only permanently tetanized animals drop Infected Meat. A temporary Tetanus infection does not change ordinary animal drops. Cook infected or rabid meat before eating.';
 'tooltip.tetanusmod.arrow'='60s Tetanus alone. An enchanted bow adds full strength; combining them extends it.';
 'item.minecraft.potion.effect.lockjaw'='Potion of Lockjaw Sonata';'item.minecraft.splash_potion.effect.lockjaw'='Splash Potion of Lockjaw Sonata';'item.minecraft.lingering_potion.effect.lockjaw'='Lingering Potion of Lockjaw Sonata';
 'item.minecraft.potion.effect.rabies'='Potion of Rabies';'item.minecraft.splash_potion.effect.rabies'='Splash Potion of Rabies';'item.minecraft.lingering_potion.effect.rabies'='Lingering Potion of Rabies';
 'death.attack.tetanusmod.tetanus'='%1$s lost the fight with a muscle spasm';'death.attack.tetanusmod.tetanus.player'='%1$s seized up while fighting %2$s';
 'death.attack.tetanusmod.rusted_nail'='%1$s found the sharp end of a rusty nail';'death.attack.tetanusmod.rusted_nail.player'='%1$s was nailed by %2$s';'death.attack.tetanusmod.rusted_nail.item'='%1$s was nailed by %2$s using %3$s';
 'death.attack.tetanusmod.returned_nail'='%1$s discovered that rusty nails come back down';
 'death.attack.tetanusmod.bear_trap'='%1$s took one step too many';'death.attack.tetanusmod.bear_trap.player'='%1$s stepped into a bear trap while fighting %2$s';
 'death.attack.tetanusmod.barbed_wire'='%1$s got tangled in the wrong wire';'death.attack.tetanusmod.barbed_wire.player'='%1$s got tangled up while fighting %2$s';
 'death.attack.tetanusmod.disorientation'='%1$s could not keep it together';'death.attack.tetanusmod.disorientation.player'='%1$s lost their bearings while fighting %2$s';
 'death.attack.tetanusmod.rabies'='%1$s succumbed to Rabies';'death.attack.tetanusmod.rabies.player'='%1$s succumbed to Rabies while fighting %2$s';
 'tooltip.tetanusmod.lockjaw_potion'='Blocks eating for 60 seconds. Milk cannot cure it; antitoxin can.';
 'tooltip.tetanusmod.rabies_potion'='Permanent until death or Rabies Cure. Loses heart capacity each minute, down to two hearts.';
 'tooltip.tetanusmod.rabies_potion_sick'='At two hearts: nausea for 10 seconds each minute. Milk cannot cure Rabies.'
}
foreach($key in $extra.Keys){$lang[$key]=$extra[$key]}
# Update guide chapters that changed, preserving the existing ten-chapter screen and dynamic placeholders.
$lang['guide.tetanusmod.diseases.body']="Tetanus: random %s-%s seconds per ordinary infection. Muscle stiffness and spasms develop over time.\n\nLockjaw Sonata blocks eating during the final 2-4 minutes. A standalone Lockjaw potion lasts one minute. Milk cures neither; antitoxin cures both.\n\nSalmonella causes vomiting, nausea, hunger loss and mild nonlethal damage. Strange Meat requires picking up infected meat; Why did I eat that? requires Salmonella.\n\nRabies lasts until death or Rabies Cure. Each minute removes one heart of capacity, down to two hearts; nausea then lasts ten seconds every minute. Milk and Tetanus Antitoxin do not cure Rabies."
$lang['guide.tetanusmod.prevention.body']="Tetanus Vaccine: %s%% lower infection risk for %s. It prevents new Tetanus without curing existing illness or Salmonella.\n\nGloves: right-click to equip in the dedicated glove slot above the offhand slot. %s%% lower contact risk; 192 durability. Shift-click to equip or remove. Gloves do not protect against infected food or potions. Combined vaccine/glove protection: %s%%.\n\nTetanus Protection: a shield enchantment. A held enchanted shield reduces risk from Tetanus I by 65 percent and Tetanus II by 40 percent.\n\nRabies Vaccine: 90 percent prevention for twenty minutes; sold by medics and craftable. Rabies Cure is sold by medics."
$lang['guide.tetanusmod.weapons.body']="Iron Spear: three iron ingots, a chain and a stick. Seven base damage, slower than an iron sword. Right-click thrusts up to 4.5 blocks; walls obstruct it. A rusted spear inflicts Tetanus.\n\nTetanus I applies to melee weapons, bows, crossbows and tridents. Tetanus II also causes disorientation, erratic movement, missed mob attacks and two points of periodic nonlethal damage.\n\nTetanized arrows alone cause one minute of Tetanus. Enchanted bows cause full-strength infection; using both extends it by 25 percent. Infinity still consumes infected arrows.\n\nOld rusted iron tools keep working in saves but are retired from new acquisition and this creative tab."
$lang['guide.tetanusmod.crafting.body']="Iron Spear: row 1 empty/iron/iron; row 2 empty/chain/iron; row 3 stick/empty/empty.\n\nRusted Iron Block: iron block surrounded by four Rust in a cross. The Tetanized Beacon accepts only this base block.\n\nBear Trap: iron/nail/iron; iron/heavy pressure plate/iron; chain below center. Reset a closed trap with a Trap Reset Tool; you have three seconds to move clear before it rearms. Rusty traps inflict Tetanus.\n\nBarbed Wire: three nails across top and bottom; chain/string/chain across middle.\n\nMedicine Cabinet: iron ingots down both sides, glass panes at top and bottom center, barrel in the center. An unemployed villager can become a medic using it."
$lang['guide.tetanusmod.brewing.body']="Water + Cow Manure makes Cow Manure Potion, causing five seconds of nausea.\n\nManure Potion + Rust makes Tetanus.\nManure Potion + Infected Meat makes Salmonella.\nManure Potion + Rabid Meat makes Rabies.\nTetanus Potion + Pufferfish makes Lockjaw Sonata.\n\nGunpowder makes splash potions; dragon breath makes lingering versions.\n\nSmithing, not brewing: leave template empty; arrows in the base slot, Rust in addition. One Rust coats the entire arrow stack, up to 64."
$lang['guide.tetanusmod.creatures.body']="Rusty contact can permanently mutate animals (%s%%) or other ordinary mobs (%s%%). Only permanent tetanized animals replace their ordinary drops with Infected Meat. Temporary Tetanus alone never changes animal loot.\n\nNatural tetanized spawns (Easy/Normal/Hard): animals %s/%s/%s percent; other mobs %s/%s/%s percent. Spawners use %s/%s/%s percent. Withers keep their separate ritual.\n\nSome mammals and undead can be rabid: thirty extra health points, leather-equivalent armor and five-point melee attacks. Rabid animals alone drop Rabid Meat.\n\nIron golems: %s%% permanent conversion chance from rusty hits. Using Rust directly has a separate eighteen-percent roll.\n\nUse creative variant spawn eggs for specific creatures, or operator commands for testing."
$lang['guide.tetanusmod.salvage.body']="Supported dropped metal rusts after five consecutive seconds touching water or fifteen seconds in exposed rain. Roofs protect against rain. Names, enchantments and damage survive conversion.\n\nA wire brush cleans equipment, costing durability per item and returning one Rust per action. Rust is placeable on floors and walls.\n\nFishing rods left in water return two sticks and two string, with a fifteen percent hook chance.\n\nA Rusty Hookline pulls targets up to twelve blocks away with a two-second cooldown.\n\nOnly permanent tetanized animals drop infected meat; only rabid animals drop rabid meat. Cooking either makes it safe."
$lang['guide.tetanusmod.bosses.body']="Four Tetanized Soul Sand in the usual Wither T: five percent mutation chance. A netherite upper-center block raises the default chance to thirty percent.\n\nA Tetanized Wither has six hundred health, normal and Tetanized Wither Stars, guaranteed Tetanus I, a fifty percent additional Tetanus II book chance, and bonus loot.\n\nCraft its special star into a Tetanized Beacon. Build the pyramid entirely from Rusted Iron Blocks. Payments remain netherite ingots.\n\nPrimary powers start at II; full pyramids offer Regeneration II or a primary upgrade to III. An active beam always grants Tetanus Resistance. Its placement and full activation have separate mod advancements."
foreach($key in @($lang.Keys)){if($key-like'guide.*'){$lang[$key]=$lang[$key].Replace('\n',"`n")}}
# Models/loot for new blocks.
foreach($id in @('rusted_iron_block','medic_cabinet')){Json "assets/tetanusmod/blockstates/$id.json" @{variants=@{''=@{model="tetanusmod:block/$id"}}};Json "assets/tetanusmod/models/block/$id.json" @{parent='minecraft:block/cube_all';textures=@{all="tetanusmod:block/$id"}}}
function Faces($texture){$faces=@{};foreach($side in @('up','down','north','south','east','west')){$faces[$side]=@{texture=$texture}};return $faces}
foreach($id in @('bear_trap','rusted_bear_trap','barbed_wire','rusted_barbed_wire')){
    $rusty=$id.StartsWith('rusted_');$texture=if($rusty){'rusted_metal'}else{'metal'}
    $variants=@{}
    $states=if($id-like'*bear_trap'){@($false,$true)}else{@($false)}
    foreach($closed in $states){
        $name=$id;if($closed){$name+='_closed'};$elements=@();$boxes=@()
        if($id-like'*bear_trap'){
            $boxes+=,@(2,0,2,14,1,14);$boxes+=,@(6,1,6,10,2,10)
            if(!$closed){$boxes+=,@(1,1,1,3,2,15);$boxes+=,@(13,1,1,15,2,15);$boxes+=,@(3,1,1,13,2,3);$boxes+=,@(3,1,13,13,2,15);foreach($v in @(3,6,9,12)){$boxes+=,@($v,2,3,($v+1),4,4);$boxes+=,@($v,2,12,($v+1),4,13)}}
            else{$boxes+=,@(5,1,2,7,7,14);$boxes+=,@(9,1,2,11,7,14);foreach($v in @(3,6,9,12)){$boxes+=,@(7,5,$v,9,7,($v+1))}}
        }else{
            foreach($z in @(3,8,13)){$boxes+=,@(1,3,$z,15,4,($z+1));foreach($x in @(3,7,11)){$boxes+=,@($x,2,($z-1),($x+1),6,($z+2))}}
            $boxes+=,@(1,0,1,3,8,3);$boxes+=,@(13,0,13,15,8,15)
        }
        foreach($b in $boxes){$elements+=@{from=@($b[0],$b[1],$b[2]);to=@($b[3],$b[4],$b[5]);faces=(Faces '#metal')}}
        Json "assets/tetanusmod/models/block/$name.json" @{parent='minecraft:block/block';textures=@{metal="tetanusmod:block/$texture";particle="tetanusmod:block/$texture"};elements=$elements}
        if($id-like'*bear_trap'){$variants['closed='+$closed.ToString().ToLowerInvariant()]=@{model="tetanusmod:block/$name"}}
    }
    if($id-like'*barbed_wire'){$variants=@{'waterlogged=false'=@{model="tetanusmod:block/$id"};'waterlogged=true'=@{model="tetanusmod:block/$id"}}}
    Json "assets/tetanusmod/blockstates/$id.json" @{variants=$variants}
}
$pileElements=@();foreach($b in @(@(3,0,3,13,1,13),@(5,1,5,12,2,11),@(7,2,6,10,3,9))){$pileElements+=@{from=@($b[0],$b[1],$b[2]);to=@($b[3],$b[4],$b[5]);faces=(Faces '#rust')}}
Json 'assets/tetanusmod/models/block/rust.json' @{textures=@{rust='tetanusmod:block/rusted_metal';particle='tetanusmod:block/rusted_metal'};elements=$pileElements}
Json 'assets/tetanusmod/blockstates/rust.json' @{variants=@{'facing=up'=@{model='tetanusmod:block/rust'};'facing=down'=@{model='tetanusmod:block/rust';x=180};'facing=north'=@{model='tetanusmod:block/rust';x=90};'facing=south'=@{model='tetanusmod:block/rust';x=90;y=180};'facing=east'=@{model='tetanusmod:block/rust';x=90;y=90};'facing=west'=@{model='tetanusmod:block/rust';x=90;y=270}}}
foreach($id in $blocks.Keys){Json "data/tetanusmod/loot_tables/blocks/$id.json" @{type='minecraft:block';pools=@(@{rolls=1;entries=@(@{type='minecraft:item';name="tetanusmod:$id"});conditions=@(@{condition='minecraft:survives_explosion'})})}}
Json 'data/minecraft/tags/blocks/mineable/pickaxe.json' @{replace=$false;values=@('tetanusmod:caltrops','tetanusmod:rusted_caltrops','tetanusmod:barbed_wire','tetanusmod:rusted_barbed_wire','tetanusmod:bear_trap','tetanusmod:rusted_bear_trap','tetanusmod:rusted_iron_block','tetanusmod:tetanized_beacon')}
Json 'data/minecraft/tags/blocks/mineable/axe.json' @{replace=$false;values=@()}
$steelTag=Get-Content (Join-Path $resourceRoot 'data/minecraft/tags/blocks/mineable/pickaxe.json') -Raw -Encoding UTF8|ConvertFrom-Json
Json 'data/minecraft/tags/blocks/mineable/pickaxe.json' @{replace=$false;values=@(@($steelTag.values)+@('tetanusmod:medic_cabinet','tetanusmod:field_medic_cabinet')|Select-Object -Unique)}
# All variant egg models inherit the native tinted egg template.
$mc=Get-ChildItem (Join-Path $projectRoot '.gradle/loom-cache/minecraftMaven') -Recurse -Filter '*build.10-v2.jar'|Select-Object -First 1
$archive=[IO.Compression.ZipFile]::OpenRead($mc.FullName)
try{$eggTypes=@($archive.Entries|Where-Object FullName -match '^assets/minecraft/models/item/[a-z_]+_spawn_egg.json$'|ForEach-Object {$_.Name.Replace('_spawn_egg.json','')}|Where-Object {$_-ne'template'})+@('giant','illusioner','wither','ender_dragon')}finally{$archive.Dispose()}
$rabidTypes=@('wolf','fox','cat','bat','rabbit','cow','pig','sheep','goat','horse','donkey','mule','llama','trader_llama','camel','panda','polar_bear','hoglin','zoglin','zombie','husk','drowned')
foreach($type in ($eggTypes|Sort-Object -Unique)){Json "assets/tetanusmod/models/item/tetanized_${type}_spawn_egg.json" @{parent='minecraft:item/template_spawn_egg'}}
foreach($type in $rabidTypes){Json "assets/tetanusmod/models/item/rabid_${type}_spawn_egg.json" @{parent='minecraft:item/template_spawn_egg'}}
# Advancements use custom IDs/criteria. No vanilla beacon criterion is reused.
$adv=@(
 @('why_did_i_do_that','Why did I do that?','Throw a rusty nail into the air and die to its return.','tetanusmod:rusted_nail','returned_nail','challenge','infected'),
 @('beacon_placed','A Beacon of Bad Ideas','Place a Tetanized Beacon.','tetanusmod:tetanized_beacon','place_beacon','task','nice_one'),
 @('beacon_full','Rust Never Sleeps','Activate a Tetanized Beacon on a complete rusty iron pyramid.','tetanusmod:rusted_iron_block','activate_full','challenge','beacon_placed'),
 @('shield_protection','Not Today, maybe tomorrow','Prevent a Tetanus attack with a Tetanus Protection shield.','minecraft:shield','block_tetanus','goal','infected'),
 @('glove_protection','Hands Off','Prevent an infection with your equipped gloves.','tetanusmod:protective_gloves','prevent_tetanus','task','infected'),
 @('rabies','Foaming at the Mouth','Contract Rabies.','tetanusmod:rabid_meat','contract_rabies','task','root'),
 @('rabies_cured','Back to Yourself','Use Rabies Cure while infected.','tetanusmod:rabies_cure','cure_rabies','goal','rabies'),
 @('antitoxin','A Dose of Common Sense','Cure Tetanus or Lockjaw Sonata with antitoxin.','tetanusmod:tetanus_antitoxin','cure_tetanus','task','infected'),
 @('trap_reset','Mind Your Fingers','Reset a closed bear trap.','tetanusmod:trap_reset_tool','reset_trap','task','rusty_business'),
 @('spear','Keep Your Distance','Land a spear thrust.','tetanusmod:iron_spear','use_spear','task','root'),
 @('variant_egg','Bad Egg','Spawn a tetanized or rabid creature with its variant egg.','minecraft:zombie_spawn_egg','spawn_variant','task','tetanized_mob'),
 @('vaccinated','Faucci Ouchie','Take a Tetanus or Rabies vaccine.','tetanusmod:tetanus_vaccine','drink_vaccine','task','root'),
 @('rust_arrows','A Pointed Upgrade','Coat an arrow batch with Rust at a smithing table.','tetanusmod:tetanized_arrow','smith_arrows','task','rusty_business')
)
foreach($a in $adv){$id=$a[0];$lang["advancements.tetanusmod.$id.title"]=$a[1];$lang["advancements.tetanusmod.$id.description"]=$a[2];Json "data/tetanusmod/advancements/$id.json" @{parent="tetanusmod:$($a[6])";display=@{icon=@{item=$a[3]};title=@{translate="advancements.tetanusmod.$id.title"};description=@{translate="advancements.tetanusmod.$id.description"};frame=$a[5];show_toast=$true;announce_to_chat=$true;hidden=$false};criteria=@{$a[4]=@{trigger='minecraft:impossible'}}}}
$lang['advancements.tetanusmod.iron_has_a_darker_side.title']='Iron Has a Darker Side'
$lang['advancements.tetanusmod.iron_has_a_darker_side.description']='Obtain an iron ingot and discover what rust can do.'
Json 'data/tetanusmod/advancements/iron_has_a_darker_side.json' @{parent='tetanusmod:root';display=@{icon=@{item='minecraft:iron_ingot'};title=@{translate='advancements.tetanusmod.iron_has_a_darker_side.title'};description=@{translate='advancements.tetanusmod.iron_has_a_darker_side.description'};frame='task';show_toast=$true;announce_to_chat=$true;hidden=$false};criteria=@{obtain_iron=@{trigger='minecraft:inventory_changed';conditions=@{items=@(@{items=@('minecraft:iron_ingot')})}}}}
$lang['advancements.tetanusmod.medic.title']="Doctor's Orders";$lang['advancements.tetanusmod.medic.description']='Trade with a village medic.'
Json 'data/tetanusmod/advancements/medic.json' @{parent='tetanusmod:root';display=@{icon=@{item='tetanusmod:medic_cabinet'};title=@{translate='advancements.tetanusmod.medic.title'};description=@{translate='advancements.tetanusmod.medic.description'};frame='task';show_toast=$true;announce_to_chat=$true;hidden=$false};criteria=@{trade=@{trigger='minecraft:villager_trade';conditions=@{villager=@{type='minecraft:villager';nbt='{VillagerData:{profession:"tetanusmod:medic"}}'}}}}}
$lang['advancements.tetanusmod.clinic.title']='The Doctor Is In';$lang['advancements.tetanusmod.clinic.description']='Place a Medicine Cabinet to offer a villager a medical career.'
Json 'data/tetanusmod/advancements/clinic.json' @{parent='tetanusmod:medic';display=@{icon=@{item='tetanusmod:medic_cabinet'};title=@{translate='advancements.tetanusmod.clinic.title'};description=@{translate='advancements.tetanusmod.clinic.description'};frame='task';show_toast=$true;announce_to_chat=$true;hidden=$false};criteria=@{place=@{trigger='minecraft:placed_block';conditions=@{location=@(@{condition='minecraft:block_state_property';block='tetanusmod:medic_cabinet'})}}}}
& (Join-Path $PSScriptRoot 'Generate-Maintenance.ps1') -Language $lang
$orderedLang=[ordered]@{}
foreach($key in ($lang.Keys|Sort-Object)){$orderedLang[$key]=$lang[$key]}
Json 'assets/tetanusmod/lang/en_us.json' $orderedLang
$recipes=@(Get-ChildItem -LiteralPath (Join-Path $resourceRoot 'data/tetanusmod/recipes') -Filter '*.json'|ForEach-Object {'tetanusmod:'+$_.BaseName})
Json 'data/tetanusmod/advancements/recipes/unlock.json' @{parent='minecraft:recipes/root';criteria=@{has_iron=@{trigger='minecraft:inventory_changed';conditions=@{items=@(@{items=@('minecraft:iron_ingot')})}}};rewards=@{recipes=$recipes}}
& (Join-Path $PSScriptRoot 'Draw-Expansion-Art.ps1')
& (Join-Path $PSScriptRoot 'Generate-Clinic.ps1')
Write-Output 'Generated expansion recipes, models, variant eggs, disease text, custom advancements, artwork and clinic.'
& (Join-Path $PSScriptRoot 'Generate-Refresh.ps1')
