param([Parameter(Mandatory=$true)][System.Collections.IDictionary]$Language)
$resourceRoot=Join-Path $PSScriptRoot '../src/main/resources'
$utf8=[Text.UTF8Encoding]::new($false)
function Json($relative,$value){$path=Join-Path $resourceRoot $relative;[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path))|Out-Null;[IO.File]::WriteAllText($path,($value|ConvertTo-Json -Depth 40)+"`n",$utf8)}
Json 'assets/tetanusmod/models/item/veterinary_kit.json' @{parent='minecraft:item/generated';textures=@{layer0='tetanusmod:item/veterinary_kit'}}
Json 'data/tetanusmod/recipes/veterinary_kit.json' @{type='minecraft:crafting_shaped';category='equipment';pattern=@('LGL','ARA',' S ');key=@{L=@{item='minecraft:leather'};G=@{item='minecraft:golden_apple'};A=@{item='tetanusmod:tetanus_antitoxin'};R=@{item='tetanusmod:rabies_cure'};S=@{item='minecraft:string'}};result=@{item='tetanusmod:veterinary_kit'}}
$Language['item.tetanusmod.veterinary_kit']='Veterinary Kit'
$Language['tooltip.tetanusmod.veterinary_kit']='Treat one tetanized or rabid animal. Single use.'
$Language['message.tetanusmod.animal_treated']='The infection has cleared. Your animal is back to normal.'
$Language['message.tetanusmod.coated']='Wax coating applied. Wet weather and use wear it away.'
$Language['tooltip.tetanusmod.coating']='Waxed: %s seconds of wet-weather protection'
$Language['message.tetanusmod.supply_request']='Medic supply request: %s x %s. Sneak and right-click with the delivery to earn lower prices.'
$Language['message.tetanusmod.supplies_done']='This medic has enough supplies today. Come back next world day.'
$Language['message.tetanusmod.supplies_delivered']='Supplies delivered. This medic now offers you better prices.'
$Language['commands.tetanusmod.inspect_mob']='Choose a mob to inspect.'
$Language['commands.tetanusmod.population']='Within %s blocks: %s mobs; %s tetanized, %s rabid, %s animals; %s persistent or protected by species rules. Loaded chunks only.'
$Language['jei.tetanusmod.coating_title']='Wax coating'
$Language['jei.tetanusmod.coating_use']='Use wax; metal in other hand'
$Language['jei.tetanusmod.coating_cost']='One honeycomb per item'
$Language['jei.tetanusmod.coating_wear']='%s wet seconds; use wears it too'
$Language['jei.tetanusmod.coating']='Hold honeycomb in one hand and clean, rustable equipment in the other, then use the honeycomb. Costs one honeycomb per item. Protection lasts 10 minutes of water/rain exposure by default, and also wears through use. It does not clean rusty equipment.'
foreach($a in @(@('clean_bill_of_health','Clean Bill of Health','Treat a permanently infected animal with a Veterinary Kit.','tetanusmod:veterinary_kit','treat_animal','tetanized_mob'),@('supply_run','Just What the Doctor Ordered','Complete a medic supply request for better prices.','minecraft:paper','deliver_supplies','medic'))){
    $Language["advancements.tetanusmod.$($a[0]).title"]=$a[1];$Language["advancements.tetanusmod.$($a[0]).description"]=$a[2]
    Json "data/tetanusmod/advancements/$($a[0]).json" @{parent="tetanusmod:$($a[5])";display=@{icon=@{item=$a[3]};title=@{translate="advancements.tetanusmod.$($a[0]).title"};description=@{translate="advancements.tetanusmod.$($a[0]).description"};frame='goal';show_toast=$true;announce_to_chat=$true;hidden=$false};criteria=@{$a[4]=@{trigger='minecraft:impossible'}}}
}
$Language['guide.tetanusmod.prevention.body']+="`n`nWax coating: use honeycomb with clean metal in your other hand. One honeycomb per item protects against wet weather; protection wears through use too.`n`nSneak and right-click a medic to see today's supply request. Deliver it for a reputation discount. Each medic accepts one delivery per world day."
$Language['guide.tetanusmod.creatures.body']+="`n`nA Veterinary Kit cures a permanently tetanized or rabid animal, restoring its normal appearance, movement and drops. It is single use and costs a golden apple, two antitoxins, Rabies Cure, leather and string."
$Language['guide.tetanusmod.salvage.body']+="`n`nAbandoned workshops sometimes generate in temperate Overworld biomes. Watch for rusty traps among their worn equipment, scrap and occasional medicine. They appear in newly generated chunks."
$Language['guide.tetanusmod.commands.body']="Operator permission level 2:`n/rustandruin summon minecraft:spider ~ ~ ~`n/rustandruin tetanize @e[type=minecraft:zombie,distance=..10]`n/rustandruin inspect <entity>`n/rustandruin population [1-128]`n`nInspection and population counts use loaded mobs and do not load chunks. Commands do not mutate players.`n`nSounds: /playsound tetanusmod:tetanus_effect, tetanusmod:spasm or tetanusmod:vomit master @a.`n`nBoth client and server need Rust & Ruin and Fabric API. Configure config/tetanusmod.json while the server is stopped. Natural animal, other mob and dungeon spawner rates have separate Easy/Normal/Hard settings."
& (Join-Path $PSScriptRoot 'Generate-Workshop.ps1')