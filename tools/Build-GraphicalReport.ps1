param(
    [Parameter(Mandatory=$true)][string]$SourceRoot,
    [Parameter(Mandatory=$true)][string]$RunStartMarker,
    [Parameter(Mandatory=$true)][string]$ClientLog,
    [string]$OutputRoot='validation/playtest-2026-10-24'
)
$ErrorActionPreference='Stop'
$project=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$utf8=[Text.UTF8Encoding]::new($false,$true)
function Absolute($path){if([IO.Path]::IsPathRooted($path)){return [IO.Path]::GetFullPath($path)};return [IO.Path]::GetFullPath((Join-Path $project $path))}
function Require($condition,$message){if(!$condition){throw $message}}
function ReadText($path){
    $bytes=[IO.File]::ReadAllBytes($path)
    if($bytes.Length-ge2-and$bytes[0]-eq255-and$bytes[1]-eq254){return [Text.UnicodeEncoding]::new($false,$false,$true).GetString($bytes,2,$bytes.Length-2)}
    if($bytes.Length-ge2-and$bytes[0]-eq254-and$bytes[1]-eq255){return [Text.UnicodeEncoding]::new($true,$false,$true).GetString($bytes,2,$bytes.Length-2)}
    if($bytes.Length-ge3-and$bytes[0]-eq239-and$bytes[1]-eq187-and$bytes[2]-eq191){return $utf8.GetString($bytes,3,$bytes.Length-3)}
    return $utf8.GetString($bytes)
}
function Html($value){return [Net.WebUtility]::HtmlEncode([string]$value)}
$source=Absolute $SourceRoot;$output=Absolute $OutputRoot;$marker=Absolute $RunStartMarker;$logPath=Absolute $ClientLog
Require ($output.StartsWith((Join-Path $project 'validation')+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) 'Output must remain inside the project validation directory.'
Require (Test-Path -LiteralPath $marker -PathType Leaf) 'Missing final-run start marker.'
$started=[DateTimeOffset]::Parse((ReadText $marker).Trim(),[Globalization.CultureInfo]::InvariantCulture,[Globalization.DateTimeStyles]::None).UtcDateTime
function Fresh($path){
    Require (Test-Path -LiteralPath $path -PathType Leaf) "Missing final-run evidence: $path"
    $file=Get-Item -LiteralPath $path
    Require ($file.LastWriteTimeUtc-ge$started) "Stale evidence predates the final run: $($file.Name)"
    return $file
}
function Report($name){$file=Fresh (Join-Path $source $name);return (ReadText $file.FullName|ConvertFrom-Json)}
$result=Report 'result.json'
Require ($result.Result-eq'PASS'-and$result.Profile-eq'jei') 'A passing FINAL full JEI matrix result is required.'
Require ([DateTimeOffset]::Parse($result.StartedUTC).UtcDateTime-eq$started) 'The result and start marker identify different runs.'
$clientResultFile=Fresh (Join-Path $source 'client-result.txt');$clientResult=(ReadText $clientResultFile.FullName).Trim()
Require ($clientResult.StartsWith('PASS:')-and$clientResult-notmatch'focused graphical profile') 'A focused profile is not a complete release pass.'
$logFile=Fresh $logPath;$log=ReadText $logFile.FullName
Require ($log.Contains('GRAPHICAL_CRAFT_ALL_PASS recipes=29')) 'Missing full graphical crafting receipt.'
Require ($log.Contains('GRAPHICAL_ACQUIRE_PASS: fifteen native brewed potions')) 'Missing the complete native potion acquisition receipt.'
Require ($log.Contains('GRAPHICAL_ACQUIRE_PASS: all nine weathering/cleaning pairs')) 'Missing the complete weathering/cleaning receipt.'
$clientAdv=Report 'graphical-advancements-jei.json';$serverAdv=Report 'graphical-advancement-server-coverage-jei.json';$despawn=Report 'graphical-despawn-jei.json'
Require ($clientAdv.Result-eq'PASS'-and$clientAdv.ActualGraphicalPlayer-eq$true-and$clientAdv.ActiveAdvancements-eq28-and$clientAdv.DirectCriterionGrants-eq0) 'Client advancement proof is incomplete.'
Require ($serverAdv.Result-eq'PASS'-and$serverAdv.ActiveAdvancements-eq28-and$serverAdv.DirectCriterionGrants-eq0) 'Server advancement proof is incomplete.'
$advances=@($clientAdv.Advancements.PSObject.Properties)
Require ($advances.Count-eq28-and@($serverAdv.Completed.PSObject.Properties).Count-eq28) 'Expected 28 active advancement records.'
Require (@($serverAdv.NativeConsumptions).Count-eq7) 'Expected seven independently consumed native doses.'
foreach($dose in $serverAdv.NativeConsumptions){Require ($dose.DoseCount-eq1-and$dose.NativeUsageStatDelta-eq1) 'A medicine/food dose was not consumed exactly once.'}
Require ($despawn.Result-eq'PASS'-and$despawn.ActualGraphicalClient-eq$true-and$despawn.NativeAutomaticTickOnly-eq$true) 'Both actual graphical-client and native automatic despawn proofs must be true.'
Require ($despawn.DirectCheckDespawnCalls-eq0-and$despawn.DirectTargetRemovalCalls-eq0) 'Despawn was forced by the test.'
$targets=@($despawn.Specimens|Where-Object {!$_.Control});$controls=@($despawn.Specimens|Where-Object {$_.Control})
Require ($targets.Count-eq6-and$controls.Count-eq4) 'Expected six hostile specimens and four persistence/livestock controls.'
foreach($specimen in $despawn.Specimens){
    Require ($specimen.BeforeClientVisible-eq$true-and$specimen.ChunkLoadedWhileAway-eq$true-and$specimen.EntityTickingWhileAway-eq$true) "Despawn specimen was not visible, loaded, and ticking: $($specimen.Label)"
    if($specimen.Control){Require ($specimen.ClientPresentAfterReturn-eq$true-and$specimen.ServerPresentAfterReturn-eq$true-and$specimen.NativeTicksWhileAway-ge20) "Persistence control disappeared or did not tick: $($specimen.Label)"}
    else{Require ($specimen.NativeRemovalReason-eq'DISCARDED'-and$specimen.ServerUuidAbsentWhileAway-eq$true-and$specimen.ClientPresentAfterReturn-eq$false-and$specimen.ServerPresentAfterReturn-eq$false) "Native hostile despawn was not confirmed on both sides: $($specimen.Label)"}
}
$photos=Join-Path $source 'screenshots';$photoFiles=@(Get-ChildItem -LiteralPath $photos -File -Filter 'rust-and-ruin-jei-*.png')
Require ($photoFiles.Count-eq$result.FreshScreenshots-and$photoFiles.Count-ge100) 'The immutable final screenshot snapshot is incomplete.'
foreach($photo in $photoFiles){$null=Fresh $photo.FullName}
$gallery=[Collections.Generic.List[object]]::new();$needed=@{}
function Proof($category,$id,$title,$description,$images){
    if($images.Count-eq2-and$images[0]-is[string]-and$images[1]-is[string]){$images=,@($images)}
    $list=@();foreach($image in $images){
        $name=[string]$image[0];Require ([IO.Path]::GetFileName($name)-eq$name-and$name.StartsWith('rust-and-ruin-jei-')-and$name.EndsWith('.png')) "Unsafe screenshot filename: $name"
        $file=Fresh (Join-Path $photos $name);$needed[$name]=$file.FullName
        $list+=[ordered]@{File=$name;Stage=[string]$image[1]}
    }
    $gallery.Add([ordered]@{Category=$category;Id=$id;Title=$title;Description=$description;Images=$list})
}
$recipeFiles=@(Get-ChildItem -LiteralPath (Join-Path $project 'src/main/resources/data/tetanusmod/recipes') -File -Filter '*.json'|Sort-Object Name)
Require ($recipeFiles.Count-eq29) 'The recipe inventory changed; review this release gallery before changing its expected counts.'
$craftReceipts=[regex]::Matches($log,'GRAPHICAL_CRAFT_PASS\s+(tetanusmod:[a-z0-9_/]+)\s+type=([A-Z_]+)\s+count=(\d+)\s+native-network-output=true')
Require ($craftReceipts.Count-eq29-and@($craftReceipts|ForEach-Object {$_.Groups[1].Value}|Sort-Object -Unique).Count-eq29) 'Expected exactly 29 unique native network crafting receipts.'
$recipeIds=@();$cookingCount=0
foreach($file in $recipeFiles){
    $recipe=ReadText $file.FullName|ConvertFrom-Json;$id='tetanusmod:'+$file.BaseName;$recipeIds+=$id
    $receipt=@($craftReceipts|Where-Object {$_.Groups[1].Value-eq$id});Require ($receipt.Count-eq1) "Missing native recipe receipt: $id"
    $kind=switch($recipe.type){'minecraft:crafting_shaped'{'TABLE'}'minecraft:crafting_shapeless'{'TABLE'}'minecraft:smelting'{'FURNACE'}'minecraft:smoking'{'SMOKER'}'minecraft:campfire_cooking'{'CAMPFIRE'}'tetanusmod:rust_arrows'{'SMITHING'}default{throw "Unsupported graphical recipe kind: $($recipe.type)"}}
    Require ($receipt[0].Groups[2].Value-eq$kind) "Wrong native workstation receipt: $id"
    $stem='rust-and-ruin-jei-craft-'+$file.BaseName
    $images=@(@(($stem+'-before.png'),'Ingredients / native result preview'),@(($stem+'-after.png'),'Acquired output / consumed inputs'))
    if($kind-in@('FURNACE','SMOKER','CAMPFIRE')){$images=@($images[0],@(($stem+'-cooked.png'),'Native cooked output'),$images[1]);$cookingCount++}
    Proof 'Recipes' $id $file.BaseName.Replace('_',' ') ("Native $kind; output count "+$receipt[0].Groups[3].Value+'. Ingredient fixtures only; output taken through the real client menu or native campfire pickup.') $images
}
Require ($cookingCount-eq6) 'Expected all six cooking variants.'
foreach($id in @('cow-manure','tetanus','salmonella','lockjaw-sonata','rabies')){foreach($form in @('drinkable','splash','lingering')){
    Require ([regex]::IsMatch($log,'GRAPHICAL_BREW_PASS:\s+'+[regex]::Escape($id)+'\s+'+$form+'\s+via native screen clicks and brewing')) "Missing native brewing receipt: $id / $form"
    Proof 'Brewing' ($id+'/'+$form) ($id.Replace('-',' ')+' / '+$form) 'Native brewing stand and client inventory clicks; the recipe engine consumes the ingredient and fuel. The 400-tick wait is accelerated.' @(@(('rust-and-ruin-jei-brew-'+$id+'-'+$form+'.png'),'Brewed native output'))
}}
Proof 'Brewing' 'all-earned-potions' 'All fifteen earned potions' 'Five complete brewing chains, each retained in drinkable, splash and lingering form in the actual client inventory.' @(@('rust-and-ruin-jei-brew-all-earned-potions.png','Actual acquired inventory'))
$rustIds=@('rusted_nail','rusted_bent_nail','rusted_rebar','rusted_sharpened_rebar','rusted_metal_pipe','rusted_caltrops','rusted_barbed_wire','rusted_iron_spear','rusted_bear_trap')
foreach($id in $rustIds){
    Require ([regex]::IsMatch($log,'GRAPHICAL_ACQUIRE_PASS:\s+tetanusmod:'+$id+'\s+[^\r\n]*wire-brush use restored clean item')) "Missing native weathering/cleaning receipt: $id"
    Proof 'Weathering' ('tetanusmod:'+$id) $id.Replace('_',' ') 'Individual native receipt confirmed this item rusted, was picked up, and was cleaned with a client wire-brush action. Shared screenshots show all nine specimens; these are not nine separate image captures.' @(@('rust-and-ruin-jei-acquire-weathering-water.png','Shared: water exposure'),@('rust-and-ruin-jei-acquire-all-nine-rusted.png','Shared: all nine acquired'),@('rust-and-ruin-jei-acquire-all-nine-brushed.png','Shared: all nine cleaned; nine Rust earned'))
}
foreach($advance in $advances){
    $id=$advance.Name;$proof=$advance.Value;Require ($proof.ClientProgress-eq'COMPLETE') "Client advancement is not complete: $id"
    Require (@($serverAdv.Completed.PSObject.Properties|Where-Object {$_.Name-eq$id}).Count-eq1) "Missing server advancement action: $id"
    Require ($proof.Screenshot-eq('rust-and-ruin-jei-adv-'+$id.Replace('/','-')+'.png')) "Advancement screenshot filename disagrees with its ID: $id"
    if($id-eq'recipes/unlock'){Require (@($proof.ClientUnlockedRecipeIds|Sort-Object -Unique).Count-eq29-and@($recipeIds|Where-Object {$_-notin$proof.ClientUnlockedRecipeIds}).Count-eq0) 'The client recipe reward did not include all 29 recipes.'}
    Proof 'Advancements' $id $id.Replace('_',' ') $proof.Action @(@($proof.Screenshot,$(if($id-eq'recipes/unlock'){'Recipe-book reward; no visible advancement node'}else{'Native completed advancement widget'})))
}
foreach($row in @(@('despawn-before','Native specimens before departure','Six hostile variants and four named/livestock controls were visible to the client.'),@('despawn-away-160-blocks','Player 160 blocks away','Source chunks remain loaded and entity ticking. No direct checkDespawn or target-removal calls.'),@('despawn-after-native-removal','Native removal and retained controls','Six hostile variants were naturally discarded; named and livestock controls remained. Both server UUID lookup and returning client visibility were checked.'))){Proof 'Despawn' $row[0] $row[1] $row[2] @(@(('rust-and-ruin-jei-'+$row[0]+'.png'),'Actual native scene'))}
$visuals=@(
@('fresh-survival-advancements','Fresh Survival advancement tab'),@('medicine-cabinet','Stainless steel cabinet'),@('medicine-cabinet-open','Cabinet doors and labeled vials'),@('cabinet-inventory','Complete cabinet inventory icon'),
@('gloves-first-person','Fitted gloves: first person'),@('gloves-left-hand','Fitted gloves: left hand'),@('gloves-third-person','Fitted gloves: third person'),@('clinic-traps','Clinic at ground level and trap models'),@('clinic-interior','Clinic interior'),
@('medic-biomes','Seven medic uniform palettes'),@('medic-tent','Underground medic tent'),@('medic-nurse-trades','Nurse trades'),@('medic-attending-scrolled','Attending trades and native scrollbar'),@('medic-hero-discount','Hero of the Village discount'),@('field-medic-supplies','Scarce field medic supplies'),@('native-tent-poi-locate','Native underground medic POI locate'),@('spawn-eggs','Dedicated species-tinted egg tab'),@('lockjaw-attempt','Blocked Lockjaw eating gesture'),@('hud','Tetanus health display'),@('rabies-hud','Rabies health display'))
foreach($row in $visuals){Proof 'Visuals' $row[0] $row[1] 'Original native client capture from the same final JEI run; open the full PNG to inspect details.' @(@(('rust-and-ruin-jei-'+$row[0]+'.png'),'Native client screenshot'))}
# Every source has been validated before publishing any PASS page or copying evidence.
Add-Type -AssemblyName System.Drawing
foreach($name in $needed.Keys){$bitmap=[Drawing.Bitmap]::FromFile($needed[$name]);try{Require ($bitmap.Width-ge320-and$bitmap.Height-ge200) "Unexpectedly small native screenshot: $name"}finally{$bitmap.Dispose()}}
[IO.Directory]::CreateDirectory((Join-Path $output 'graphical/screenshots'))|Out-Null
[IO.Directory]::CreateDirectory((Join-Path $output 'graphical/reports'))|Out-Null
$manifestFiles=[Collections.Generic.List[object]]::new()
function CopyEvidence($inputPath,$relative){
    $file=Fresh $inputPath;$hash=(Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash
    $target=Join-Path $output $relative;Copy-Item -LiteralPath $file.FullName -Destination $target -Force
    Require ((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash-eq$hash) "Copied evidence hash mismatch: $relative"
    $manifestFiles.Add([ordered]@{File=$relative.Replace('\','/');Bytes=$file.Length;SHA256=$hash;SourceModifiedUtc=$file.LastWriteTimeUtc.ToString('o')})
}
foreach($name in ($needed.Keys|Sort-Object)){CopyEvidence $needed[$name] ('graphical/screenshots/'+$name)}
foreach($name in @('result.json','client-result.txt','graphical-advancements-jei.json','graphical-advancement-server-coverage-jei.json','graphical-despawn-jei.json')){CopyEvidence (Join-Path $source $name) ('graphical/reports/'+$name)}
# Windows PowerShell redirects stdout as UTF-16. Preserve text while normalizing the published log to UTF-8.
$publishedLog=Join-Path $output 'graphical/reports/client-log.txt';[IO.File]::WriteAllText($publishedLog,$log,$utf8)
$manifestFiles.Add([ordered]@{File='graphical/reports/client-log.txt';Bytes=(Get-Item -LiteralPath $publishedLog).Length;SHA256=(Get-FileHash -LiteralPath $publishedLog -Algorithm SHA256).Hash;SourceSHA256=(Get-FileHash -LiteralPath $logPath -Algorithm SHA256).Hash;SourceModifiedUtc=$logFile.LastWriteTimeUtc.ToString('o');Encoding='Original text normalized to UTF-8 without BOM; original source hash retained'})
$categoryOrder=@('Recipes','Brewing','Weathering','Advancements','Despawn','Visuals');$buttons=[Text.StringBuilder]::new();$null=$buttons.Append('<button type="button" class="selected" data-filter="all" aria-pressed="true">All checks</button>')
foreach($category in $categoryOrder){$count=@($gallery|Where-Object {$_.Category-eq$category}).Count;$null=$buttons.Append('<button type="button" data-filter="'+(Html $category)+'" aria-pressed="false">'+(Html $category)+' <span>'+ $count +'</span></button>')}
$cards=[Text.StringBuilder]::new()
foreach($proof in $gallery){
    $search=($proof.Title+' '+$proof.Id+' '+$proof.Description).ToLowerInvariant()
    $null=$cards.Append('<article class="proof" data-category="'+(Html $proof.Category)+'" data-search="'+(Html $search)+'"><div class="card-head"><span class="category">'+(Html $proof.Category)+'</span><h2>'+(Html $proof.Title)+'</h2><code>'+(Html $proof.Id)+'</code></div><p>'+(Html $proof.Description)+'</p><div class="frames">')
    foreach($frame in $proof.Images){$url='graphical/screenshots/'+$frame.File;$null=$cards.Append('<figure><a href="'+(Html $url)+'" target="_blank" rel="noopener"><img loading="lazy" decoding="async" src="'+(Html $url)+'" alt="'+(Html ($proof.Title+' - '+$frame.Stage))+'"></a><figcaption>'+(Html $frame.Stage)+'</figcaption></figure>')}
    $null=$cards.Append('</div></article>')
}
$startedText=Html $started.ToString('yyyy-MM-dd HH:mm:ss UTC');$proofCount=$gallery.Count;$imageCount=$needed.Count
$gloveLink=if(Test-Path -LiteralPath (Join-Path $output 'gloves/index.html')){'<a href="gloves/index.html">Opaque-sleeve glove before/after comparisons</a>'}else{''}
$html=@"
<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Rust &amp; Ruin graphical release checks</title>
<style>
:root{color-scheme:dark;--bg:#101517;--panel:#192125;--line:#344047;--text:#e8eeed;--muted:#adbcbf;--rust:#d99865;--green:#b4d9bd}*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:16px/1.55 system-ui,Segoe UI,sans-serif}a{color:var(--rust)}header,main,footer{max-width:1500px;margin:auto;padding:32px 24px}header{border-bottom:1px solid var(--line);background:linear-gradient(120deg,#273137,#171e21)}.eyebrow,.category{text-transform:uppercase;letter-spacing:.12em;font-size:12px;color:var(--rust)}h1{font-size:clamp(28px,4vw,46px);line-height:1.15;margin:12px 0}header p{max-width:920px;color:var(--muted)}.pass{display:inline-block;color:var(--green);border:1px solid #51775d;border-radius:6px;padding:4px 9px;font-weight:650}.metrics{display:flex;flex-wrap:wrap;gap:12px;margin-top:24px}.metric{padding:10px 14px;border:1px solid var(--line);border-radius:8px;min-width:130px}.metric strong{font-size:26px;display:block}.metric small{color:var(--muted)}details{max-width:1100px;margin-top:22px;color:var(--muted)}summary{cursor:pointer;color:var(--text)}.controls{position:sticky;top:0;z-index:2;background:#101517f5;padding:18px 0;border-bottom:1px solid var(--line);backdrop-filter:blur(12px)}label{display:block;margin-bottom:7px;color:var(--muted)}input{width:100%;max-width:650px;padding:11px 13px;background:var(--panel);border:1px solid var(--line);border-radius:7px;color:var(--text);font:inherit}nav{display:flex;gap:8px;flex-wrap:wrap;margin-top:14px}button{cursor:pointer;padding:7px 11px;background:var(--panel);border:1px solid var(--line);border-radius:6px;color:var(--text);font:inherit;font-size:14px}button.selected{border-color:var(--rust);color:var(--rust)}button span{opacity:.7;margin-left:5px}input:focus,button:focus-visible,a:focus-visible{outline:2px solid var(--rust);outline-offset:3px}.status{color:var(--muted);font-size:14px;margin:12px 0}.proof{margin:22px 0;padding:20px;background:var(--panel);border:1px solid var(--line);border-radius:10px}.proof h2{margin:4px 0;font-size:21px;font-weight:650;text-transform:capitalize}.proof p{max-width:1100px;color:var(--muted);margin:12px 0 18px}.proof code{font-size:12px;color:var(--muted);overflow-wrap:anywhere}.frames{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(360px,100%),1fr));gap:14px}figure{margin:0;min-width:0}figure a{display:block;background:#090d0f;border:1px solid var(--line);border-radius:6px;overflow:hidden}img{display:block;width:100%;height:auto;max-height:460px;object-fit:contain}figcaption{font-size:13px;color:var(--muted);padding-top:7px}footer{border-top:1px solid var(--line);color:var(--muted);font-size:14px}footer a{margin-right:18px}[hidden]{display:none!important}@media(max-width:650px){header,main,footer{padding:22px 14px}.proof{padding:14px}.controls{position:static}.metric{min-width:105px;flex:1}.frames{grid-template-columns:1fr}}
</style></head><body>
<header><span class="eyebrow">Minecraft 1.20.1 Fabric / final JEI run</span><h1>Rust &amp; Ruin graphical release checks</h1><span class="pass">Verified final-run evidence</span><p>Browse original Minecraft screenshots from the same completed run. Every included file was checked against the final-run start marker; copied PNGs retain their original bytes and SHA-256 hashes.</p>
<div class="metrics"><div class="metric"><strong>29</strong><small>native recipes</small></div><div class="metric"><strong>15</strong><small>brewed potion forms</small></div><div class="metric"><strong>9</strong><small>rust / clean pairs</small></div><div class="metric"><strong>28</strong><small>active advancements</small></div><div class="metric"><strong>6 + 4</strong><small>despawn targets / controls</small></div></div>
<details><summary>Methods and limits</summary><p>These are automated checks in an actual graphical integrated client. Crafting, smithing, potion transfers and cleaning use native client interactions; native cooking and brewing engines consume real ingredients, with long cooking waits accelerated. Ingredients and test fixtures may be prepared with Creative access. Crafted and brewed outputs are acquired rather than inserted by the test.</p><p>Advancement actions use native server gameplay methods and actual client item-use timers; synchronized native advancement widgets are inspected and captured. This does not mean every action was a physical mouse click. There are 27 visible advancement nodes and one recipe-book reward. The retired Creative-only Bad Egg ID is excluded from the 28 active advancements.</p><p>Despawn uses automatic native ticks while source chunks remain loaded and entity ticking. Six ordinary hostile variants disappear, while a name-tagged hostile and three livestock controls remain. Nine weathering receipts share three overview screenshots, labeled as shared images. These checks do not replace owner Survival playtesting or a long-running multiplayer session.</p></details>
</header><main><div class="controls"><label for="search">Find a recipe, advancement, item or check</label><input id="search" type="search" placeholder="Try cabinet, vaccine, smithing, guardian or despawn" autocomplete="off"><nav aria-label="Check categories">$buttons</nav><p id="status" class="status" role="status" aria-live="polite">$proofCount checks / $imageCount original screenshots</p></div><div id="gallery">$cards</div><p id="empty" hidden>No matching checks. Clear the search or choose another category.</p></main>
<footer><p>$gloveLink</p><p>Final run started $startedText. The gallery uses JEI screenshots; the other recipe-viewer configurations have separate validation records.</p><p><a href="graphical/manifest.json">Evidence manifest and SHA-256 hashes</a><a href="graphical/reports/result.json">Final run result</a><a href="graphical/reports/graphical-advancements-jei.json">Client advancement proof</a><a href="graphical/reports/graphical-despawn-jei.json">Native despawn proof</a><a href="graphical/reports/client-log.txt">Complete client log</a></p></footer>
<script>
(function(){'use strict';var search=document.getElementById('search'),cards=Array.from(document.querySelectorAll('.proof')),buttons=Array.from(document.querySelectorAll('[data-filter]')),category='all';function apply(){var query=search.value.trim().toLowerCase(),shown=0;cards.forEach(function(card){var visible=(category==='all'||card.dataset.category===category)&&card.dataset.search.indexOf(query)!==-1;card.hidden=!visible;if(visible)shown++;});document.getElementById('status').textContent=shown+' of '+cards.length+' checks shown. Click an image to open the original PNG.';document.getElementById('empty').hidden=shown!==0;}search.addEventListener('input',apply);buttons.forEach(function(button){button.addEventListener('click',function(){category=button.dataset.filter;buttons.forEach(function(other){var selected=other===button;other.classList.toggle('selected',selected);other.setAttribute('aria-pressed',String(selected));});apply();});});apply();})();
</script></body></html>
"@
[IO.File]::WriteAllText((Join-Path $output 'index.html'),$html+"`n",$utf8)
$manifest=[ordered]@{Result='PASS';Scope='Final full JEI graphical run; original native PNGs';StartedUTC=$started.ToString('o');GeneratedUTC=[DateTime]::UtcNow.ToString('o');StartMarkerSHA256=(Get-FileHash -LiteralPath $marker -Algorithm SHA256).Hash;SourceSnapshotScreenshots=$photoFiles.Count;IncludedOriginalScreenshots=$needed.Count;Recipes=29;CookingVariants=6;PotionForms=15;WeatheringPairs=9;ActiveAdvancements=28;VisibleAdvancementNodes=27;RecipeBookRewards=1;RetiredAdvancement='variant_egg';NativeDespawnTargets=6;RetainedControls=4;NativeAutomaticTickOnly=$true;PageSHA256=(Get-FileHash -LiteralPath (Join-Path $output 'index.html') -Algorithm SHA256).Hash;GeneratorSHA256=(Get-FileHash -LiteralPath $PSCommandPath -Algorithm SHA256).Hash;Proofs=$gallery.ToArray();Files=$manifestFiles.ToArray()}
[IO.File]::WriteAllText((Join-Path $output 'graphical/manifest.json'),($manifest|ConvertTo-Json -Depth 15)+"`n",$utf8)
[pscustomobject]@{Result='PASS';Page=(Join-Path $output 'index.html');Proofs=$gallery.Count;OriginalScreenshots=$needed.Count;FreshFinalRun=$started.ToString('o')}|ConvertTo-Json
