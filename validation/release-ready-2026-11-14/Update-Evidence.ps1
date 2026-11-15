param(
 [switch]$ArchiveInitial,
 [switch]$ArchiveFinalProbe,
 [string[]]$AddReports=@(),
 [string[]]$AddLogs=@(),
 [string[]]$AddReceipts=@(),
 [string]$CraftLog,
 [string]$CraftMinUtc="2026-11-14T23:40:00Z",
 [string]$NativeFinalXml,
 [string]$NativeFinalLog,
 [string[]]$RequiredReports=@(),
 [switch]$MarkComplete
)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$output=$PSScriptRoot
$utf8=[Text.UTF8Encoding]::new($false,$true)
$cutoff=[DateTimeOffset]::Parse('2026-11-14T23:27:00Z').UtcDateTime
function Require($condition,$message){if(!$condition){throw $message}}
function Absolute($relative){if([IO.Path]::IsPathRooted($relative)){return [IO.Path]::GetFullPath($relative)};return [IO.Path]::GetFullPath((Join-Path $root $relative))}
function Html($value){return [Net.WebUtility]::HtmlEncode([string]$value)}
function ReadNative($path){
 $bytes=[IO.File]::ReadAllBytes($path)
 if($bytes.Length-ge2-and$bytes[0]-eq255-and$bytes[1]-eq254){return [Text.UnicodeEncoding]::new($false,$false,$true).GetString($bytes,2,$bytes.Length-2)}
 if($bytes.Length-ge3-and$bytes[0]-eq239-and$bytes[1]-eq187-and$bytes[2]-eq191){return $utf8.GetString($bytes,3,$bytes.Length-3)}
 return $utf8.GetString($bytes)
}
function Fresh($relative){
 $file=Get-Item -LiteralPath (Absolute $relative)
 Require (!$file.PSIsContainer-and$file.LastWriteTimeUtc-ge$cutoff) ('Missing or stale evidence: '+$relative)
 return $file
}
$state=ReadNative (Join-Path $output 'manifest.json')|ConvertFrom-Json
$files=[Collections.Generic.List[object]]::new();foreach($value in $state.Files){$files.Add($value)}
$reports=[Collections.Generic.List[object]]::new();foreach($value in $state.Reports){$reports.Add($value)}
$runs=[Collections.Generic.List[object]]::new();foreach($value in $state.Runs){$runs.Add($value)}
function RecordFile($relative,$source,$kind,$sourceHash){
 $destination=Join-Path $output $relative;$hash=(Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash
 $previous=@($files|Where-Object {$_.File-eq$relative})
 if($previous.Count){Require ($previous[0].SHA256-eq$hash) ('Immutable evidence changed: '+$relative);return}
 $files.Add([ordered]@{File=$relative;Kind=$kind;Bytes=(Get-Item -LiteralPath $destination).Length;SHA256=$hash;SourceSHA256=$sourceHash;SourceBytes=$source.Length;SourceModifiedUtc=$source.LastWriteTimeUtc.ToString('o')})
}
function CopyOriginal($relative,$target,$kind){
 $file=Fresh $relative;$hash=(Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash
 $destination=Join-Path $output $target
 [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($destination))|Out-Null
 if(Test-Path -LiteralPath $destination){Require ((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash-eq$hash) ('Refusing different archived bytes: '+$target)}
 else{Copy-Item -LiteralPath $file.FullName -Destination $destination}
 RecordFile $target $file $kind $hash
 return $target
}
function ArchiveLog($relative,$group){
 $file=Fresh $relative;$name=$file.Name;$raw=CopyOriginal $file.FullName ('logs/raw/'+$group+'/'+$name) 'OriginalLogBytes'
 $text=ReadNative $file.FullName;$target='logs/'+$group+'/'+$name;$destination=Join-Path $output $target
 [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($destination))|Out-Null
 $bytes=$utf8.GetBytes($text)
 if(Test-Path -LiteralPath $destination){Require ((ReadNative $destination)-ceq$text) ('Refusing different normalized log: '+$target)}
 else{[IO.File]::WriteAllBytes($destination,$bytes)}
 RecordFile $target $file 'NativeLogTextUtf8' (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash
 return $target
}
function AddRun($id,$outcome,$candidate,$explanation,$log,$xml,$tests,$failures){
 if(@($runs|Where-Object {$_.Id-eq$id}).Count){return}
 $runs.Add([ordered]@{Id=$id;Outcome=$outcome;Candidate=$candidate;Explanation=$explanation;Log=$log;Xml=$xml;Tests=$tests;Failures=$failures})
}
function CollectScreens($node,$images,$location){
 if($null-eq$node-or$node.GetType().IsValueType){return}
 if($node-is[string]){
  if($node-match'^rust-and-ruin-[a-z0-9_-]+\.png$'){$images.Add([ordered]@{Screenshot=$node;View=$location+' '+$node;EvidenceSource='OriginalGraphicalReportProperty';OriginalPropertyPath=$location})}
  return
 }
 if($node-is[Collections.IEnumerable]-and$node-isnot[pscustomobject]){foreach($entry in $node){CollectScreens $entry $images $location};return}
 if($node.PSObject.Properties['Screenshot']){$view=$location;if($node.PSObject.Properties['View']){$view=$node.View};$images.Add([ordered]@{Screenshot=[string]$node.Screenshot;View=[string]$view})}
 foreach($property in $node.PSObject.Properties){if($property.Name-ne'Screenshot'){CollectScreens $property.Value $images ($location+'.'+$property.Name)}}
}
function ArchiveReport($name){
 Require ([IO.Path]::GetFileName($name)-eq$name-and$name-match'^graphical-[a-z0-9-]+\.json$') ('Unsafe report name: '+$name)
 if(@($reports|Where-Object {$_.Name-eq$name}).Count){return}
 $file=Fresh ('build/client-smoke/'+$name);$report=ReadNative $file.FullName|ConvertFrom-Json
 Require ($report.Result-eq'PASS'-and($report.ActualGraphicalClient-eq$true-or($name-match'^graphical-advancements-(plain|jei|rei)\.json$'-and$report.ActualGraphicalPlayer-eq$true)-or($name-match'^graphical-advancement-layout-(plain|jei|rei)\.json$'-and$report.ActualConnectedPlayer-eq$true)-or($name-match'^graphical-trap-regression-(plain|jei|rei)\.json$'-and$report.NativeForwardAndJumpKeys-and$report.NativeUseKeyClicks-eq3-and$report.DirectTriggerCalls-eq0-and$report.DirectResetCalls-eq0)-or($name-match'^graphical-proximity-audio-(plain|jei|rei)\.json$'-and$report.NativeClientNetworkEffectCommands-eq4-and$report.ExpectedInfectionPlayEvents-eq1-and$report.ObservedInfectionPlayEvents-eq1-and$report.ObservedMedicWorkEvents-eq1-and$report.PlayerConfigProbeStateRestored))) ('Not a completed actual graphical report: '+$name)
 foreach($field in @('DirectCriterionGrants','ProgressRevocations','CriterionRevocations')){if($report.PSObject.Properties[$field]){Require ($report.$field-eq0) ('Advancement grants/resets found: '+$name)}}
 if($name-eq'graphical-beacon-ui-plain.json'){
  Require ($report.NativeHasteIVApplied-and$report.UnpaidIVConfirmationRejected-and$report.OriginalPlayerAndWorldRestored) 'Missing native IV/payment/restoration proof'
  Require ($report.NativeCurrencyConsumed.NetheriteIngots-eq1-and$report.NativeCurrencyConsumed.TetanizedWitherStars-eq1) 'Unexpected paid IV transaction currency'
  $scales=@($report.Scales|ForEach-Object {$_.AppliedGuiScale}|Sort-Object -Unique)
  Require ($scales.Count-eq4-and@($scales|Where-Object {$_-notin@(2,3,4,6)}).Count-eq0) 'Incomplete native beacon GUI scale matrix'
  foreach($scale in $report.Scales){Require ($scale.RequestedGuiScale-eq$scale.AppliedGuiScale-and$scale.NativeGeometry.ButtonLabelsFit-and$scale.NativeGeometry.NoControlOrSlotOverlap-and$scale.NativeGeometry.ViewportContainsFrame) 'Capped or overlapping beacon GUI proof'}
 }
 if($name-match'^graphical-advancement-layout-(plain|jei|rei)\.json$'){
  Require ($report.VisibleModNodes-eq30-and$report.ClientSelectedSlotRestored-and$report.ExistingCriteriaDatesPreserved-gt0) 'Incomplete actual layout/restoration/criterion preservation'
  $requested=@($report.Scales|ForEach-Object {$_.RequestedGuiScale}|Sort-Object -Unique)
  Require (($requested-join',')-eq'2,3,4,6') 'Incomplete native advancement GUI scale matrix'
  foreach($scale in $report.Scales){
   Require ($scale.NativeCapRespected-and$scale.NativeAppliedScale-eq$scale.RequestedGuiScale) 'Capped native advancement scale'
   foreach($id in @('minecraft:adventure/root','minecraft:end/root','minecraft:husbandry/root','minecraft:nether/root','minecraft:story/root','tetanusmod:root')){
    Require ($scale.NativeTabs-contains$id) ('Missing native advancement tab: '+$id)
   }
   Require ($report.Keyboard.('NativeHomeRecenteredAtScale'+$scale.RequestedGuiScale)) 'Native Home navigation did not recenter'
  }
  Require ($report.NativeActionEvidence.AppleUsedStatDelta-eq1-and$report.NativeActionEvidence.CowKilled-and$report.NativeActionEvidence.CraftingTablePickedUp-and$report.NativeActionEvidence.EndRootComplete-and$report.NativeActionEvidence.NetherRootComplete) 'Missing actual vanilla-root actions'
  foreach($shot in $report.Screenshots){Require ($shot.CapturePhase-eq'GameRenderer.render:TAIL'-and$shot.AppliedGuiScale-eq$shot.RequestedGuiScale) 'Layout screenshot not a rendered native requested-scale frame'}
 }
 $images=[Collections.Generic.List[object]]::new();CollectScreens $report $images '$'
 if($name-match'^graphical-trap-regression-(plain|jei|rei)\.json$'){
  Require ([string]$report.ConnectedPlayer-match'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$') 'Missing connected trap actor UUID'
  foreach($action in @('survival-capture','survival-native-movement-held','native-top-click-reset','native-captive-entity-click-reset','native-side-click-reset','creative-native-walking-immune')){
   Require (@($report.Evidence|Where-Object {$_.Action-eq$action}).Count-eq1) ('Missing actual trap action: '+$action)
  }
  $viewer=[regex]::Match($name,'-(plain|jei|rei)\.json$').Groups[1].Value
  foreach($label in @('survival-before','survival-captured','survival-held','top-before-reset','top-after-reset','captive-blocking-jaws','captive-after-reset','side-before-reset','side-after-reset','creative-before','creative-bypass')){
   $images.Add([ordered]@{Screenshot='rust-and-ruin-'+$viewer+'-trap-'+$label+'.png';View=$label;EvidenceSource='NativeActionReceiptAndVerifiedSourceFilename';SourceController='GraphicalTrapRegressionAudit.java'})
  }
 }
 if($name-match'^graphical-proximity-audio-(plain|jei|rei)\.json$'){
  Require ([string]$report.ActualConnectedPlayer-match'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$') 'Missing connected audio actor UUID'
  $viewer=[regex]::Match($name,'-(plain|jei|rei)\.json$').Groups[1].Value
  $mapping=@(
   @('far32-new-command-infection','far32-command-silent'),
   @('approach-already-infected-to6','approach6-still-silent'),
   @('fresh-near6-command-single-positional-voice','fresh-near-voice'),
   @('later-command-refresh-no-extra-voice','refresh-silent'),
   @('near-procedural-spawn-client-rusted-flag-no-voice','procedural-rust-skin-silent'),
   @('native-medic-work-single-custom-client-event','medic-work-custom-sound')
  )
  foreach($pair in $mapping){
   $action=$pair[0];Require (@($report.Receipts|Where-Object {$_.Action-eq$action}).Count-eq1) ('Missing native audio action: '+$action)
   $images.Add([ordered]@{Screenshot='rust-and-ruin-'+$viewer+'-audio-'+$pair[1]+'.png';View=$action;EvidenceSource='NativeActionReceiptAndVerifiedSourceFilename';SourceController='GraphicalProximityAudioAudit.java'})
  }
 }
 Require ($images.Count-gt0) ('No native screenshot references: '+$name)
 $copied=CopyOriginal $file.FullName ('reports/final/'+$name) 'OriginalGraphicalReport'
 $archived=[Collections.Generic.List[object]]::new()
 foreach($image in $images){
  $png=$image.Screenshot
  Require ([IO.Path]::GetFileName($png)-eq$png-and$png-match'^rust-and-ruin-[a-z0-9_-]+\.png$') ('Unsafe screenshot: '+$png)
  $target=CopyOriginal ('build/client-smoke/screenshots/'+$png) ('screenshots/final/'+$png) 'OriginalNativePng'
  $archived.Add([ordered]@{Screenshot=$png;View=$image.View;File=$target;EvidenceSource=if($image.EvidenceSource){$image.EvidenceSource}else{'OriginalGraphicalReportProperty'};OriginalPropertyPath=$image.OriginalPropertyPath})
 }
 $reports.Add([ordered]@{Name=$name;Result='PASS';Candidate='FINAL';File=$copied;EvidenceSource='OriginalGraphicalReport';SourceModifiedUtc=$file.LastWriteTimeUtc.ToString('o');Scope=$report.Scope;Screenshots=$archived.ToArray()})
}
if($ArchiveInitial){
 $xmlFile=Fresh '.tools/release-ready-2026-11-14/native-full.xml';[xml]$native=ReadNative $xmlFile.FullName
 $cases=@($native.SelectNodes('//testcase'));$failures=@($native.SelectNodes('//failure|//error'))
 Require ($cases.Count-eq418-and$failures.Count-eq1) 'Unexpected retained pre-final native results'
 $xml=CopyOriginal $xmlFile.FullName 'reports/pre-final/native-full.xml' 'OriginalFailedNativeXml'
 $log=ArchiveLog '.tools/release-ready-2026-11-14/native-full.log' 'pre-final'
 AddRun 'native-pre-final' 'FAILED' 'PRE-FINAL D287' 'Completed 417/418; one Allay variant spawn-egg assertion failed. This is retained as failed. The later423-test suite is a separate run.' $log $xml $cases.Count $failures.Count
 $log=ArchiveLog '.tools/release-ready-2026-11-14/client-plain-full.log' 'pre-final'
 $text=ReadNative (Absolute '.tools/release-ready-2026-11-14/client-plain-full.log')
 Require ($text.Contains('requires a fresh actor')-and$text.Contains('rusty_business')-and$text.Contains('BUILD FAILED')) 'Unexpected retained broad-client failure'
 AddRun 'client-pre-final-composite' 'FAILED HARNESS' 'PRE-FINAL D287' 'The run completed31 ordinary recipe transactions, 15 potion forms, and weathering/cleaning actions, then correctly refused a causal advancement journey on an actor that already earned rusty_business. Overall failure is retained. Its crafting/brewing PNGs remain in the shared capture directory but are not included as final-candidate proof; a fresh final-candidate crafting pass will be archived separately.' $log $null $null $null
 $null=CopyOriginal '.tools/release-ready-2026-11-14/production-boot.log' 'logs/raw/pre-final/production-boot-invalid.log' 'OriginalInvalidHarnessLog'
 $log=ArchiveLog '.tools/release-ready-2026-11-14/production-boot.log' 'pre-final'
 AddRun 'production-harness-initial' 'INVALID HARNESS' 'PRE-FINAL D287' 'The first launch had a named development game jar on its classpath and used the project working directory. It loaded neither the production API nor the packaged mod. It does not test the release artifact.' $log $null $null $null
 $log=ArchiveLog '.tools/release-ready-2026-11-14/production-boot-corrected.log' 'pre-final'
 AddRun 'production-pre-final-eula-guard' 'SCOPED PASS' 'PRE-FINAL D287' 'Corrected intermediary production discovery reached the unaccepted EULA guard, exit 0, no world. This is not a complete mod/server initialization or gameplay claim.' $log $null $null $null
 $null=CopyOriginal 'build/client-smoke/graphical-crafting-stations-plain.json' 'reports/pre-final/graphical-crafting-stations-plain.json' 'PreFinalPartialCraftDispatchReport'
 $xmlFile=Fresh '.tools/release-ready-2026-11-14/native-focused.xml';[xml]$focused=ReadNative $xmlFile.FullName
 $focusedCases=@($focused.SelectNodes('//testcase'));$focusedFailures=@($focused.SelectNodes('//failure|//error'))
 Require ($focusedCases.Count-eq15-and$focusedFailures.Count-eq0) 'Focused final-shot/native acquisition receipt does not pass15/15'
 $xml=CopyOriginal $xmlFile.FullName 'reports/final/native-focused.xml' 'OriginalPassingFocusedNativeXml'
 $log=ArchiveLog '.tools/release-ready-2026-11-14/native-focused.log' 'final'
 Require ((ReadNative (Absolute '.tools/release-ready-2026-11-14/native-focused.log')).Contains('BUILD SUCCESSFUL')) 'Focused native build did not complete'
 AddRun 'native-focused-final-shot' 'PASS' 'FINAL E832' '15 focused native cases: five final-shot regressions and 10 acquisition regressions. Actual broken bow/crossbow/Multishot firing, projectile NBT, infection hits and plain controls. This is focused evidence, separate from the full suite.' $log $xml 15 0
 foreach($entry in @('candidate-package-diff.json','client-release-review.json','client-package-audit.json')){
  $group=if($entry-eq'candidate-package-diff.json'){'final'}else{'pre-final'}
  $null=CopyOriginal ('.tools/release-ready-2026-11-14/'+$entry) ('reports/'+$group+'/'+$entry) 'SourceOrPackageReview'
 }
}
if($ArchiveFinalProbe){
 $file=Fresh '.tools/release-ready-2026-11-14/production-boot-final.log';$text=ReadNative $file.FullName
 Require ($text.Contains('tetanusmod 0.1.1-beta')-and$text.Contains('fabric-api 0.92.6+1.20.1')-and$text.Contains('agree to the EULA')-and$text-notmatch'ERROR|Exception|has crashed') 'Final production namespace probe did not reach the expected guard cleanly'
 $server=Absolute '.tools/release-ready-2026-11-14/production-server'
 Require ((ReadNative (Join-Path $server 'eula.txt')).Trim()-eq'eula=false') 'Unexpected accepted EULA'
 Require (!(Test-Path -LiteralPath (Join-Path $server 'isolated-release-test'))) 'Unexpected generated production world'
 Require ((Get-FileHash -LiteralPath (Join-Path $server 'mods/rust-and-ruin-0.1.1-beta.jar') -Algorithm SHA256).Hash-eq$state.CandidateSHA256) 'Probe does not contain the final candidate JAR'
 $log=ArchiveLog $file.FullName 'final'
 AddRun 'production-final-eula-guard' 'SCOPED PASS' 'FINAL E832' 'Exact final packaged JAR and Fabric API are discovered using the intermediary game jar in an isolated production cwd. Clean exit 0 at EULA=false; no world created. The guard prevents full server lifecycle and gameplay, which are tested separately.' $log $null $null $null
}
foreach($name in $AddReports){ArchiveReport $name}
foreach($path in $AddLogs){$null=ArchiveLog $path 'final'}
foreach($path in $AddReceipts){$file=Fresh $path;$null=CopyOriginal $file.FullName ('reports/final/'+$file.Name) 'OriginalSupplementaryReceipt'}
if($CraftLog){
 $file=Fresh $CraftLog;$text=ReadNative $file.FullName
 $entries=@([regex]::Matches($text,'GRAPHICAL_CRAFT_PASS (?<id>tetanusmod:[a-z0-9_/-]+) type=(?<type>[A-Z_]+) count=(?<n>\d+) native-network-output=true'))
 $reportFile=Fresh 'build/client-smoke/graphical-crafting-stations-plain.json';$craft=ReadNative $reportFile.FullName|ConvertFrom-Json
 $craftCutoff=[DateTimeOffset]::Parse($CraftMinUtc).UtcDateTime
 Require ($reportFile.LastWriteTimeUtc-ge$craftCutoff-and$craft.Result-eq'PASS') 'Crafting station metadata is stale or did not pass'
 $actualIds=@($entries|ForEach-Object {$_.Groups['id'].Value}|Sort-Object -Unique)
 $expectedIds=@(Get-ChildItem -LiteralPath (Absolute 'src/main/resources/data/tetanusmod/recipes') -Filter '*.json' -File|Where-Object {$_.BaseName-notin@('medical_prescription','tetanized_end_crystal')}|ForEach-Object {'tetanusmod:'+$_.BaseName}|Sort-Object)
 Require ($entries.Count-eq$actualIds.Count-and$actualIds.Count-eq$craft.OrdinaryRecipesCrafted-and($actualIds-join'|')-eq($expectedIds-join'|')) 'Actual native craft receipts do not match every ordinary recipe id'
 Require ($text.Contains('GRAPHICAL_CRAFT_ALL_PASS ordinary_recipes='+$actualIds.Count)) 'No completed ordinary-crafting receipt'
 $copied=CopyOriginal $reportFile.FullName 'reports/final/graphical-crafting-stations-plain.json' 'OriginalCraftDispatchReport'
 $log=ArchiveLog $file.FullName 'final'
 $images=[Collections.Generic.List[object]]::new()
 foreach($entry in $entries){
  $id=$entry.Groups['id'].Value;$path=$id.Substring('tetanusmod:'.Length)
  Require ($path-match'^[a-z0-9_]+$') 'Unexpected nested recipe screenshot path'
  foreach($stage in @('before','after')){
   $png='rust-and-ruin-plain-craft-'+$path+'-'+$stage+'.png'
   $source=Fresh ('build/client-smoke/screenshots/'+$png)
   Require ($source.LastWriteTimeUtc-ge$craftCutoff) ('Old crafting screenshot rejected: '+$png)
   $target=CopyOriginal $source.FullName ('screenshots/final/'+$png) 'OriginalNativePng'
   $images.Add([ordered]@{Screenshot=$png;View=$id+' '+$stage;File=$target;RecipeId=$id;EvidenceSource='NativeCraftReceipt';NativeType=$entry.Groups['type'].Value;NativeOutputCount=[int]$entry.Groups['n'].Value})
  }
 }
 if(!@($reports|Where-Object {$_.Name-eq'graphical-crafting-stations-plain.json'}).Count){
  $reports.Add([ordered]@{Name='graphical-crafting-stations-plain.json';Result='PASS';Candidate='FINAL';File=$copied;EvidenceSource='NativeCraftReceipt';NativeLog=$log;Scope='Every ordinary recipe through real native networked menus; before/after screenshots derived from verified source naming, not an invented Screenshot field. Dedicated prescription/forge stations require their separate reports.';SourceModifiedUtc=$reportFile.LastWriteTimeUtc.ToString('o');Screenshots=$images.ToArray()})
 }
}
if($NativeFinalXml-or$NativeFinalLog){
 Require ($NativeFinalXml-and$NativeFinalLog) 'A complete native run requires both XML and its own log'
 $xmlFile=Fresh $NativeFinalXml;$logFile=Fresh $NativeFinalLog;[xml]$native=ReadNative $xmlFile.FullName;$text=ReadNative $logFile.FullName
 $cases=@($native.SelectNodes('//testcase'));$failures=@($native.SelectNodes('//failure|//error'));$receipt=[regex]::Matches($text,'(\d+) GAME TESTS COMPLETE')
 Require ($cases.Count-eq423-and$failures.Count-eq0-and$receipt.Count-eq1-and[int]$receipt[0].Groups[1].Value-eq$cases.Count-and$text.Contains('BUILD SUCCESSFUL')) 'Full final423-test native suite incomplete, failed or mismatched'
 $xml=CopyOriginal $xmlFile.FullName 'reports/final/native-final.xml' 'OriginalPassingNativeXml';$log=ArchiveLog $logFile.FullName 'final'
 AddRun 'native-final-complete' 'PASS' 'FINAL E832' 'Complete unfiltered 423-case native run. XML and complete log counts agree; no failures. Earlier native failures remain separate.' $log $xml $cases.Count 0
 $state.CompleteNativeStatus='PASS'
}
if($RequiredReports.Count){$state|Add-Member -NotePropertyName RequiredReports -NotePropertyValue @($RequiredReports) -Force}
elseif($state.PSObject.Properties['RequiredReports']){$RequiredReports=@($state.RequiredReports)}
$missing=@($RequiredReports|Where-Object {$name=$_;!@($reports|Where-Object {$_.Name-eq$name-and$_.Candidate-eq'FINAL'}).Count})
$state.Pending=@()
if($state.CompleteNativeStatus-ne'PASS'){$state.Pending+=,'Complete final423-test native suite'}
if($RequiredReports.Count){$state.Pending+=@($missing)}else{$state.Pending+=,'Root-approved complete final graphical matrix'}
if($MarkComplete){
 Require ($state.CompleteNativeStatus-eq'PASS'-and$RequiredReports.Count-gt0-and$missing.Count-eq0) 'Cannot mark complete without full native PASS and all explicitly required final graphical reports'
 $state.SnapshotStatus='SCOPED_CHECKS_COMPLETE'
}
$state.Files=$files.ToArray();$state.Reports=$reports.ToArray();$state.Runs=$runs.ToArray();$state.UpdatedUtc=[DateTime]::UtcNow.ToString('o')
[IO.File]::WriteAllText((Join-Path $output 'manifest.json'),($state|ConvertTo-Json -Depth 14),$utf8)
$body=[Text.StringBuilder]::new()
foreach($report in $reports){
 $null=$body.Append('<section><h2>PASS: '+(Html $report.Name)+'</h2><p>Final candidate. <a href="'+(Html $report.File)+'">Original native report</a>. '+@($report.Screenshots).Count+' screenshot references.</p><details><summary>View original screenshots</summary><div class="grid">')
 foreach($image in $report.Screenshots){$null=$body.Append('<figure><a href="'+(Html $image.File)+'"><img loading="lazy" src="'+(Html $image.File)+'" alt="'+(Html $image.View)+'"></a><figcaption>'+(Html $image.View)+'</figcaption></figure>')}
 $null=$body.Append('</div></details></section>')
}
$runHtml=[Text.StringBuilder]::new()
foreach($run in $runs){$null=$runHtml.Append('<li><strong>'+(Html $run.Outcome)+' &#8212; '+(Html $run.Id)+'</strong> ['+(Html $run.Candidate)+']<p>'+(Html $run.Explanation)+'</p><a href="'+(Html $run.Log)+'">Readable UTF-8 log</a>');if($run.Xml){$null=$runHtml.Append(' &middot; <a href="'+(Html $run.Xml)+'">Original XML</a>')};$null=$runHtml.Append('</li>')}
$otherLinks=[Text.StringBuilder]::new()
foreach($file in $files|Where-Object {$_.Kind-eq'OriginalSupplementaryReceipt'-or($_.Kind-eq'NativeLogTextUtf8'-and$_.File-like'logs/final/client-*')}){
 $null=$otherLinks.Append('<li><a href="'+(Html $file.File)+'">'+(Html $file.File)+'</a></li>')
}
$pendingText=if($state.Pending.Count){$state.Pending-join'; '}else{'None for the explicitly listed scope'}
$status=if($state.SnapshotStatus-eq'SCOPED_CHECKS_COMPLETE'){'Listed checks complete'}else{'In progress &#8212; not a final release pass'}
$html=@"
<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Rust &amp; Ruin final release checks</title><style>body{margin:0;background:#1d2022;color:#e8e6df;font:16px/1.6 system-ui,sans-serif}main{max-width:1400px;margin:auto;padding:30px}a{color:#efbc84}h1{line-height:1.2}h2{border-bottom:1px solid #59615f;padding-bottom:8px;margin-top:36px}.notice{padding:18px;background:#343b36;border-left:4px solid #d39a65}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(310px,1fr));gap:16px}figure{margin:0;padding:10px;background:#303638;border:1px solid #59615f;overflow-wrap:anywhere}img{display:block;width:100%;height:auto}figcaption{font-size:14px;padding-top:8px}li{margin-bottom:16px}code{overflow-wrap:anywhere}small{color:#bbc3bb}</style><main><h1>Rust &amp; Ruin 0.1.1-beta<br>Final release-readiness evidence</h1><div class="notice"><strong>$(Html $status)</strong><p>Final candidate <code>$(Html $state.CandidateSHA256)</code>. Full native suite: $(Html $state.CompleteNativeStatus).</p><p>Pending: $(Html $pendingText)</p><a href="manifest.json">SHA256 manifest</a> &middot; <a href="README.md">Scope and fixture disclosures</a><br><small>Updated $(Html $state.UpdatedUtc)</small></div><h2>What this snapshot proves</h2><p>Final-candidate native and visible-client receipts are separated from pre-final controls and failed harness attempts. Original JSON/XML/PNG bytes are retained. Readable logs are normalized to UTF-8; logs/raw retains exact original bytes, including UTF-16 originals. Both hashes appear in the manifest. No previous gallery is relabeled as a new run.</p><p>The only changed packaged entry versus the pre-final candidate is CombatInfection.class. The native final-shot regressions exercise real weapon destruction, per-projectile enchantment snapshots, saved NBT, actual infection and plain controls. Pre-final crafting/brewing images remain available in the shared capture directory, but only their genuine log receipts are retained here as pre-final controls.</p>$body<h2>Native runs, production probes and retained failures</h2><ul>$runHtml</ul><h2>Supporting receipts and complete client logs</h2><ul>$otherLinks</ul><h2>Limits</h2><p>Controlled fixtures supply materials, entities, status effects and structures. Dedicated causal journeys separately test earned advancements; baseline render specimens do not claim ordinary survival progression. Production namespace probes stop at EULA=false and create no world: they verify discovery and early bootstrap, not a complete production server session. Passing this listed matrix does not guarantee every modpack combination.</p><footer>Local static gallery; original Minecraft captures; no external assets, scripts, synthetic images or hosting.</footer></main></html>
"@
[IO.File]::WriteAllText((Join-Path $output 'index.html'),$html,$utf8)
$reportLines=($reports|ForEach-Object {'- ['+$_.Name+']('+ $_.File+'): '+@($_.Screenshots).Count+' original screenshot references.'})-join[Environment]::NewLine
$readme=@"
# Rust & Ruin final release-readiness evidence

Status: **$($state.SnapshotStatus)**. Full final native suite: **$($state.CompleteNativeStatus)**.

Open [the local gallery](index.html) or [manifest.json](manifest.json). Final candidate SHA256: $($state.CandidateSHA256). The pre-final D287 candidate is labeled separately; only CombatInfection.class changed between the two packages.

## Completed final graphical reports

$reportLines

## Native and earlier attempts

The manifest lists each native run, production probe and retained failure separately. The pre-final native run completed417/418 and remains failed. The initial composite client passed 31 ordinary crafting transactions, 15 potion forms and weathering/cleaning actions, then failed its fresh-actor guard because earlier actions had already awarded rusty_business. Its original PNGs remain in the shared capture directory; they are not substituted for the planned final-candidate crafting pass. Its original logs and station-dispatch JSON are controls, not final gameplay proof.

The 15-case focused native run covers five real final-shot cases and 10 acquisition cases. A complete final native PASS is recorded only after the separate unfiltered 423-case XML and complete build/server log finish and agree.

Production namespace probes use the actual packaged JAR, Fabric API and intermediary Minecraft classes in an isolated cwd. They stop at EULA=false, exit without creating a world, and do not claim full production server initialization or gameplay.

## Source and fixture disclosures

Baseline screenshots use supplied items, effects, controlled mobs and placed templates. They verify actual native render layers, potion tooltips, disease HUD/tab hearts, gloves, cabinet, lockjaw gesture, medic clothing and trade UI. These specimens do not earn all advancements; a separate causal journey is required for that claim.

The final-shot fix reads the current weapon object's retained enchantment NBT, matches the registered Tetanus enchantment and clamps levels 0-2. It uses no global shot cache or cross-action state. Native tests exercise final bow/crossbow shots, every Multishot arrow/firework, normal trident and unbroken controls, saved projectile NBT and actual damage/infection.

All original receipts and PNGs are copied byte for byte. Human-readable logs are UTF-8; logs/raw retains original bytes. The manifest records original and normalized hashes. Generated HTML, Markdown and manifest are indexes, not native receipts. No previous evidence gallery is rewritten.

Pending: $pendingText.
"@
[IO.File]::WriteAllText((Join-Path $output 'README.md'),$readme,$utf8)
[pscustomobject]@{Status=$state.SnapshotStatus;GraphicalReports=$reports.Count;NativeStatus=$state.CompleteNativeStatus;HashedFiles=$files.Count;Screenshots=@($files|Where-Object {$_.Kind-eq'OriginalNativePng'}).Count}|ConvertTo-Json