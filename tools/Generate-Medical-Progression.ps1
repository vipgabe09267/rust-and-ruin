$ErrorActionPreference='Stop'
$resourceRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../src/main/resources'))
$utf8=[Text.UTF8Encoding]::new($false)
function Json($relative,$value){$path=Join-Path $resourceRoot $relative;[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path))|Out-Null;[IO.File]::WriteAllText($path,($value|ConvertTo-Json -Depth 70)+"`n",$utf8)}
function ReadJson($relative){Get-Content -LiteralPath (Join-Path $resourceRoot $relative) -Raw -Encoding UTF8|ConvertFrom-Json}
function AddValue($relative,$value){$path=Join-Path $resourceRoot $relative;$data=if(Test-Path -LiteralPath $path){ReadJson $relative}else{@{replace=$false;values=@()}};if($data.values-notcontains$value){$data.values=@($data.values)+@($value)};Json $relative $data}

Json 'data/tetanusmod/recipes/prescription_writer.json' @{type='minecraft:crafting_shaped';category='equipment';pattern=@('IFI','PBP','ICI');key=@{I=@{item='minecraft:iron_ingot'};F=@{item='minecraft:feather'};P=@{tag='minecraft:planks'};B=@{item='minecraft:book'};C=@{item='minecraft:copper_ingot'}};result=@{item='tetanusmod:prescription_writer';count=1}}
Json 'data/tetanusmod/recipes/medical_prescription.json' @{type='tetanusmod:prescription_writing';ingredients=@(@{item='minecraft:paper'},@{item='minecraft:ink_sac'});result=@{item='tetanusmod:medical_prescription';count=1}}
Json 'assets/tetanusmod/models/item/medical_prescription.json' @{parent='minecraft:item/generated';textures=@{layer0='tetanusmod:item/medical_prescription'}}
Json 'assets/tetanusmod/models/item/prescription_writer.json' @{parent='tetanusmod:block/prescription_writer'}
$faces=@{};foreach($side in @('north','south','east','west','up','down')){$faces[$side]=@{texture='#steel';uv=@(0,0,16,16)}}
function Box($from,$to,$texture){$f=@{};foreach($side in @('north','south','east','west','up','down')){$f[$side]=@{texture=$texture;uv=@(0,0,16,16)}};return @{from=$from;to=$to;faces=$f}}
$elements=@()
foreach($x in @(1,13)){foreach($z in @(1,13)){$elements+=Box @($x,0,$z) @(($x+2),9,($z+2)) '#steel'}}
$elements+=Box @(0,9,0) @(16,11,16) '#steel'
$elements+=Box @(3,11,7) @(13,14,14) '#recess'
$elements+=Box @(3,11,3) @(13,12,7) '#steel'
$elements+=Box @(4,13,10) @(12,15,10.25) '#paper'
$elements+=Box @(12,11,3) @(14,13.5,5) '#ink'
$elements+=Box @(7,9.5,-0.02) @(9,10.8,0.02) '#badge'
foreach($row in 0..2){foreach($col in 0..5){$x=3.6+$col*1.35;$z=3.5+$row*1.0;$elements+=Box @($x,12,$z) @(($x+0.85),12.35,($z+0.65)) '#keys'}}
Json 'assets/tetanusmod/models/block/prescription_writer.json' @{parent='minecraft:block/block';textures=@{particle='tetanusmod:block/cabinet_steel';steel='tetanusmod:block/cabinet_steel';recess='tetanusmod:block/cabinet_recess';paper='tetanusmod:item/medical_prescription';ink='tetanusmod:block/cabinet_vial_teal';badge='tetanusmod:block/cabinet_badge';keys='tetanusmod:block/cabinet_cap'};elements=$elements;display=@{gui=@{rotation=@(30,225,0);translation=@(0,0,0);scale=@(0.6,0.6,0.6)}}}
Json 'assets/tetanusmod/blockstates/prescription_writer.json' @{variants=@{'facing=north'=@{model='tetanusmod:block/prescription_writer'};'facing=east'=@{model='tetanusmod:block/prescription_writer';y=90};'facing=south'=@{model='tetanusmod:block/prescription_writer';y=180};'facing=west'=@{model='tetanusmod:block/prescription_writer';y=270}}}
Json 'data/tetanusmod/loot_tables/blocks/prescription_writer.json' @{type='minecraft:block';pools=@(@{rolls=1;conditions=@(@{condition='minecraft:survives_explosion'});entries=@(@{type='minecraft:item';name='tetanusmod:prescription_writer'})})}
AddValue 'data/minecraft/tags/blocks/mineable/pickaxe.json' 'tetanusmod:prescription_writer'
AddValue 'data/minecraft/tags/blocks/needs_stone_tool.json' 'tetanusmod:prescription_writer'
$unlock=ReadJson 'data/tetanusmod/advancements/recipes/unlock.json';$unlock.rewards.recipes=@(@($unlock.rewards.recipes)+@('tetanusmod:prescription_writer','tetanusmod:medical_prescription')|Sort-Object -Unique);Json 'data/tetanusmod/advancements/recipes/unlock.json' $unlock

# IDs and existing criteria remain stable. Custom item icons identify the actual mod progression.
$icons=@{iron_has_a_darker_side='rusted_iron_block';rusty_business='rusted_nail';spear='iron_spear';rust_arrows='tetanized_arrow';trap_reset='trap_reset_tool';rusty_hook='rusty_fishing_hook';infected='rusted_metal_pipe';lockjaw_sonata='medical_prescription';antitoxin='tetanus_antitoxin';vaccinated='tetanus_vaccine';glove_protection='protective_gloves';strange_meat='infected_meat';why_did_i_eat_that='infected_meat';rabies='rabid_meat';rabies_cured='rabies_cure';why_did_i_do_that='rusted_nail';tetanized_mob='tetanized_zombie_spawn_egg';rusty_guardian='rusted_iron_block';nice_one='tetanized_wither_star';beacon_placed='tetanized_beacon';beacon_full='rusted_iron_block';clinic='medic_cabinet';medic='medical_prescription';supply_run='medical_prescription';clean_bill_of_health='veterinary_kit'}
foreach($id in $icons.Keys){$path="data/tetanusmod/advancements/$id.json";$data=ReadJson $path;if($null-ne$data.display){$data.display.icon=@{item="tetanusmod:$($icons[$id])"};Json $path $data}}
$cure=ReadJson 'data/tetanusmod/advancements/antitoxin.json';$cure.parent='tetanusmod:infected'
$cure.criteria.cure_tetanus=@{trigger='minecraft:consume_item';conditions=@{item=@{items=@('tetanusmod:tetanus_antitoxin')};player=@{effects=@{'tetanusmod:tetanus'=@{}}}}}
$cure.criteria|Add-Member -MemberType NoteProperty -Name cure_lockjaw -Value @{trigger='minecraft:consume_item';conditions=@{item=@{items=@('tetanusmod:tetanus_antitoxin')};player=@{effects=@{'tetanusmod:lockjaw'=@{}}}}} -Force
$cure|Add-Member -MemberType NoteProperty -Name requirements -Value @(,@('cure_tetanus','cure_lockjaw')) -Force
Json 'data/tetanusmod/advancements/antitoxin.json' $cure

$langPath='assets/tetanusmod/lang/en_us.json';$lang=ReadJson $langPath
$strings=@{
 'item.tetanusmod.medical_prescription'='Medical Prescription';
 'block.tetanusmod.prescription_writer'='Prescription Writer';
 'tooltip.tetanusmod.medical_prescription'='Print at a Writer; trade with emeralds.';
 'tooltip.tetanusmod.rabid_meat_origin'='Dropped by rabid animals; see Field Guide.';
 'gui.tetanusmod.writer.paper'='Paper';'gui.tetanusmod.writer.ink'='Ink';'gui.tetanusmod.writer.result'='Result';
 'gui.tetanusmod.writer.hint'='1 paper + 1 ink sac';
 'jei.tetanusmod.prescription_use'='A Prescription Writer consumes one paper and one ink sac to make one Medical Prescription. Medic treatment offers require a prescription as well as emeralds.';
 'gui.tetanusmod.medic_supply_hint'='Sneak + right-click a medic to see today''s supply request.';
 'message.tetanusmod.supply_request'='Today this medic needs %s x %s. You are holding %s. Hold the full amount in your main hand, then sneak and right-click this medic to deliver it.';
 'message.tetanusmod.supplies_done'='You already completed this medic''s delivery today. A new request is available next world day.';
 'message.tetanusmod.supplies_delivered'='Supply delivery complete! This medic now offers you better prices.';
 'advancements.tetanusmod.supply_run.description'='Sneak and right-click a medic to read today''s request, then repeat while holding the requested supplies.';
 'advancements.tetanusmod.antitoxin.description'='Drink Tetanus Antitoxin while infected with Tetanus or Lockjaw Sonata.';
}
foreach($key in $strings.Keys){$lang|Add-Member -MemberType NoteProperty -Name $key -Value $strings[$key] -Force}
$crafting="Prescription Writer: iron/feather/iron; planks/book/planks; iron/copper/iron. New village clinics and cave medic camps include a writer; Nurses and Nurse Practitioners may also sell one per delivery. Place it, add one paper and one ink sac, and take one Medical Prescription. Treatment trades require emeralds plus a prescription. Medics may sell two emergency prescriptions per village delivery, or one in a cave camp. Nurse treatments are antitoxin or Tetanus Vaccine; Rabies Cure starts at Nurse Practitioner, and Rabies Vaccine and Veterinary Kits start at Medical Doctor. Higher ranks have lower base treatment prices. Native discounts apply; stock still waits for its normal delivery."
if(!$lang.'guide.tetanusmod.crafting.body'.Contains('Prescription Writer:')){$lang.'guide.tetanusmod.crafting.body'+="`n`n"+$crafting}
$delivery="Medic supply requests: sneak and right-click any adult medic. The chat tells you the exact item and count. Hold that full amount in your main hand, then sneak and right-click the same medic again. One delivery per medic per world day earns better prices and Special Delivery. These requests are separate from prescription writing."
if(!$lang.'guide.tetanusmod.crafting.body'.Contains('Medic supply requests:')){$lang.'guide.tetanusmod.crafting.body'+="`n`n"+$delivery}
$rabies="Finding rabid variants: explore newly spawned creatures in their usual habitats and look for purple-tinted, unusually aggressive mammals or zombie relatives. Wolves, foxes, rabbits, bats, farm mammals and horses are eligible, as are zombies, husks and drowned. Eligible natural/spawner spawns that do not become Tetanized roll a separate Rabid chance: Easy/Normal/Hard %s/%s/%s percent. Rusty weapons make Tetanized variants, not Rabid variants; giving a mob the Rabies effect does not change it into a permanent Rabid variant. For direct testing, use a Rabid spawn egg from the mod's spawn-egg creative tab."
if(!$lang.'guide.tetanusmod.creatures.body'.Contains('Finding rabid variants:')){$lang.'guide.tetanusmod.creatures.body'+="`n`n"+$rabies}
Json $langPath $lang
Write-Output 'Generated Prescription Writer progression, readable supply instructions, cure criteria and custom advancement icons.'
