$ErrorActionPreference='Stop'
$resourceRoot=Join-Path $PSScriptRoot '../src/main/resources'
$utf8=[Text.UTF8Encoding]::new($false)
# Display ancestry is deliberately balanced. IDs, criteria, rewards and existing
# progress keys are unchanged; a parent is not a new gameplay requirement.
$parents=[ordered]@{
 iron_has_a_darker_side='root'
 rusty_business='iron_has_a_darker_side'
 spear='rusty_business'
 rust_arrows='spear'
 trap_reset='rust_arrows'
 rusty_hook='rusty_business'
 infected='root'
 lockjaw_sonata='infected'
 antitoxin='lockjaw_sonata'
 vaccinated='antitoxin'
 glove_protection='vaccinated'
 shield_protection='glove_protection'
 strange_meat='infected'
 why_did_i_eat_that='strange_meat'
 rabies='strange_meat'
 rabies_cured='rabies'
 why_did_i_do_that='infected'
 tetanized_mob='root'
 rusty_guardian='tetanized_mob'
 nice_one='rusty_guardian'
 beacon_placed='nice_one'
 beacon_full='beacon_placed'
 clinic='root'
 medic='clinic'
 supply_run='medic'
 clean_bill_of_health='supply_run'
}
foreach($id in $parents.Keys){
 $path=Join-Path $resourceRoot "data/tetanusmod/advancements/$id.json"
 $value=Get-Content -LiteralPath $path -Encoding UTF8 -Raw|ConvertFrom-Json
 if($null-eq$value.display){throw "Missing displayed advancement $id"}
 $value.parent="tetanusmod:$($parents[$id])"
 [IO.File]::WriteAllText($path,($value|ConvertTo-Json -Depth 60)+"`n",$utf8)
}
$langPath=Join-Path $resourceRoot 'assets/tetanusmod/lang/en_us.json'
$lang=Get-Content -LiteralPath $langPath -Encoding UTF8 -Raw|ConvertFrom-Json
$lang|Add-Member -MemberType NoteProperty -Name 'gui.tetanusmod.advancements.navigation' -Value 'Drag / scroll | Shift: sideways | Home: start' -Force
$lang|Add-Member -MemberType NoteProperty -Name 'gui.tetanusmod.advancements.navigation_mouse' -Value 'Drag / scroll | Shift: sideways' -Force
$lang|Add-Member -MemberType NoteProperty -Name 'advancements.tetanusmod.supply_run.title' -Value 'Special Delivery' -Force
$lang|Add-Member -MemberType NoteProperty -Name 'advancements.tetanusmod.supply_run.description' -Value 'Fulfil a medic supply request.' -Force
[IO.File]::WriteAllText($langPath,($lang|ConvertTo-Json -Depth 60)+"`n",$utf8)
Write-Output 'Balanced advancement display branches; all existing criterion keys and rewards retained.'
