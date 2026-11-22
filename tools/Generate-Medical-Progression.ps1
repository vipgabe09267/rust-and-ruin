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
$unlock=ReadJson 'data/tetanusmod/advancements/recipes/unlock.json';$unlock.rewards.recipes=@(@($unlock.rewards.recipes)+@('tetanusmod:prescription_writer','tetanusmod:medical_prescription','tetanusmod:tetanus_tnt','tetanusmod:clinic_notice_board','tetanusmod:rust_magnet','tetanusmod:field_splint')|Sort-Object -Unique);Json 'data/tetanusmod/advancements/recipes/unlock.json' $unlock

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
 'message.tetanusmod.boss.rustquake'='Rustquake incoming. Move out of the orange ring.';
 'message.tetanusmod.boss.skull_barrage'='Skull barrage incoming. Move away from the orange marker.';
 'message.tetanusmod.boss.rustfire'='Rustfire incoming. Leave the purple ring.';
 'message.tetanusmod.boss.wingstorm'='Wingstorm incoming. Leave the wide purple ring or take cover.';
 'guide.tetanusmod.boss_abilities'="Tetanized bosses deal double the damage on boss-owned hits, including skulls, blasts, bites, wings, and dragon breath. Wither skulls apply Wither III on Normal and Hard.`n`nThe Wither marks orange rings before a Rustquake or a three-skull barrage. The dragon marks purple rings before a seven-second Rustfire pool or a Wingstorm that knocks players outward. Leave the marked area when you hear a warning. At half health, both bosses enrage and use their abilities more often.";
 'block.tetanusmod.clinic_notice_board'='Clinic Notice Board';
 'item.tetanusmod.rust_magnet'='Rust Magnet';
 'item.tetanusmod.field_splint'='Field Splint';
 'effect.tetanusmod.splinted'='Splinted';
 'tooltip.tetanusmod.rust_magnet'='Pull loose metal salvage within %s blocks. A successful pull costs durability.';
 'tooltip.tetanusmod.field_splint'='Use during Tetanus to reduce spasms for %s seconds.';
 'message.tetanusmod.magnet_empty'='No reachable metal salvage.';
 'message.tetanusmod.splint_healthy'='A Field Splint helps when you have Tetanus.';
 'message.tetanusmod.splint_active'='Your Field Splint is still active.';
 'message.tetanusmod.medic_case.delivery_amount'='Deliver %s x %s.';
 'gui.tetanusmod.notice_board.village'='Village clinic cases';
 'gui.tetanusmod.notice_board.field'='Cave camp cases';
 'gui.tetanusmod.notice_board.select'='Choose a case. Hover over it to see the objectives.';
 'gui.tetanusmod.notice_board.active'='%s is active. Use /mediccase.';
 'gui.tetanusmod.notice_board.completed'='Today''s case is complete. Check back tomorrow.';
 'guide.tetanusmod.utilities'="Use a Clinic Notice Board to choose a case. Hover over a case to preview its objectives. Bring supplies or report fieldwork to the appropriate kind of medic.`n`nRight-click with a Rust Magnet to pull loose Rust, nails, and metal salvage up to seven blocks toward you. Solid blocks stop it. Each successful pull costs one durability.`n`nUse a Field Splint during Tetanus to reduce spasms for 90 seconds. Use antitoxin to cure the infection.";
 'item.tetanusmod.medical_prescription'='Medical Prescription';
 'block.tetanusmod.prescription_writer'='Prescription Writer';
 'tooltip.tetanusmod.medical_prescription'='Write at a Prescription Writer and pay with emeralds for treatment.';
 'tooltip.tetanusmod.rabid_meat_origin'='Dropped by rabid animals; see Field Guide.';
 'gui.tetanusmod.writer.paper'='Paper';'gui.tetanusmod.writer.ink'='Ink';'gui.tetanusmod.writer.result'='Result';
 'gui.tetanusmod.writer.hint'='1 paper + 1 ink sac';
 'jei.tetanusmod.prescription_use'='A Prescription Writer consumes one paper and one ink sac to make one Medical Prescription. Medic treatment offers require a prescription as well as emeralds.';
 'block.tetanusmod.tetanus_tnt'='Tetanus TNT';
 'entity.tetanusmod.tetanus_tnt'='Primed Tetanus TNT';
 'gui.tetanusmod.medic_supply_hint'='Sneak + right-click to start or continue a case file.';
 'advancements.tetanusmod.supply_run.description'='Sneak and right-click a medic, then complete both steps of a case file.';
 'message.tetanusmod.medic_case.clinic_restock'='Clinic Restock';
 'message.tetanusmod.medic_case.workshop_inspection'='Workshop Inspection';
 'message.tetanusmod.medic_case.herd_checkup'='Herd Checkup';
 'message.tetanusmod.medic_case.camp_rations'='Camp Rations';
 'message.tetanusmod.medic_case.salvage_inspection'='Salvage Inspection';
 'message.tetanusmod.medic_case.cave_patrol'='Cave Patrol';
 'message.tetanusmod.medic_case.objective'='%s (step %s of %s). %s';
 'message.tetanusmod.medic_case.deliver'='Bring %s x %s. Hold the supplies in your main hand and sneak + right-click the medic. Partial deliveries count.';
 'message.tetanusmod.medic_case.work.brush'='Clean %s rusty stack with a Wire Brush.';
 'message.tetanusmod.medic_case.work.veterinary'='Treat %s permanently tetanized or rabid animal with a Veterinary Kit.';
 'message.tetanusmod.medic_case.work.cave_patrol'='Defeat %s hostile mobs below Y=32 in the Overworld.';
 'message.tetanusmod.medic_case.report'='%s is ready to turn in. Sneak + right-click a %s to report back.';
 'message.tetanusmod.medic_case.wrong_medic'='%s belongs to a %s. Check progress with /mediccase.';
 'message.tetanusmod.medic_case.complete'='%s complete! You earned supplies, emeralds, and better prices with this medic.';
 'message.tetanusmod.medic_case.none'='You have no active case file. Sneak + right-click a medic to take one.';
 'message.tetanusmod.medic_case.done'='You finished a case today. Check back the next in-game day.';
 'message.tetanusmod.medic_case.abandoned'='Case abandoned. Sneak + right-click a medic to take a case.';
 'advancements.tetanusmod.antitoxin.description'='Drink Tetanus Antitoxin while infected with Tetanus or Lockjaw Sonata.';
}
foreach($key in $strings.Keys){$lang|Add-Member -MemberType NoteProperty -Name $key -Value $strings[$key] -Force}
foreach($key in @('message.tetanusmod.supply_request','message.tetanusmod.supplies_done','message.tetanusmod.supplies_delivered')){$lang.PSObject.Properties.Remove($key)}
$crafting='Craft a Prescription Writer with iron/feather/iron, planks/book/planks, and iron/copper/iron. Clinics and cave camps have a writer, and some lower-rank medics sell the table. Add one paper and one ink sac at the Writer to make a Medical Prescription. Every treatment trade uses emeralds and a written prescription. Nurses treat Tetanus. Nurse Practitioners can offer Rabies Cure, while Medical Doctors can also offer Rabies Vaccine and Veterinary Kits.'
$cases='Sneak and right-click an adult medic to accept a case file. Put deliveries in your main hand and sneak-interact again. You can bring partial stacks. If the case asks you to clean gear, treat an animal, or fight a cave patrol, do the task and report back to the same kind of medic. Use /mediccase to check progress or /mediccase abandon to drop it. Completing a case earns rewards and better prices once per player per world day.'
$tnt='Make Tetanus TNT in a crafting table. Put TNT in the center, Rust in all four corners, and gunpowder in the other four slots. It has twice the explosion power of TNT and leaves a visual rust cloud for 5 to 15 seconds.'
$body=$lang.'guide.tetanusmod.crafting.body' -replace '(?s)\r?\n\r?\n(?:Prescription Writer:|Craft a Prescription Writer with).*',''
$lang.'guide.tetanusmod.crafting.body'=@($body,$crafting,$cases,$tnt) -join "`n`n"
$rabies="Finding rabid variants: explore newly spawned creatures in their usual habitats and look for purple-tinted, unusually aggressive mammals or zombie relatives. Wolves, foxes, rabbits, bats, farm mammals and horses are eligible, as are zombies, husks and drowned. Eligible natural/spawner spawns that do not become Tetanized roll a separate Rabid chance: Easy/Normal/Hard %s/%s/%s percent. Rusty weapons make Tetanized variants, not Rabid variants; giving a mob the Rabies effect does not change it into a permanent Rabid variant. For direct testing, use a Rabid spawn egg from the mod's spawn-egg creative tab."
if(!$lang.'guide.tetanusmod.creatures.body'.Contains('Finding rabid variants:')){$lang.'guide.tetanusmod.creatures.body'+="`n`n"+$rabies}
Json $langPath $lang
& (Join-Path $PSScriptRoot 'Generate-Utility-Icons.ps1')
Write-Output 'Generated Prescription Writer progression, medic case text, Tetanus TNT recipe unlock, and advancement resources.'
