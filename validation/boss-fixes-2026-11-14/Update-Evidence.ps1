param(
    [switch]$ArchiveInitial,
    [string[]]$AddReports=@(),
    [string[]]$AddLogs=@(),
    [string]$NativeReleaseXml,
    [string]$NativeReleaseLog,
    [switch]$MarkComplete,
    [switch]$ArchiveClosedPlaytest,
    [switch]$ArchiveAudio,
    [switch]$ArchiveResourceRepeat
)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$output=[IO.Path]::GetFullPath($PSScriptRoot)
$utf8=[Text.UTF8Encoding]::new($false,$true)
$cutoff=[DateTimeOffset]::Parse('2026-11-14T22:39:00Z').UtcDateTime
function Require($value,$message){if(!$value){throw $message}}
function Absolute($relative){if([IO.Path]::IsPathRooted($relative)){return [IO.Path]::GetFullPath($relative)};return [IO.Path]::GetFullPath((Join-Path $root $relative))}
function Html($value){return [Net.WebUtility]::HtmlEncode([string]$value)}
function ReadNative($path){
 $bytes=[IO.File]::ReadAllBytes($path)
 if($bytes.Length-ge2-and$bytes[0]-eq255-and$bytes[1]-eq254){return [Text.UnicodeEncoding]::new($false,$false,$true).GetString($bytes,2,$bytes.Length-2)}
 if($bytes.Length-ge3-and$bytes[0]-eq239-and$bytes[1]-eq187-and$bytes[2]-eq191){return $utf8.GetString($bytes,3,$bytes.Length-3)}
 return $utf8.GetString($bytes)
}
function Fresh($relative){
 $path=Absolute $relative;Require (Test-Path -LiteralPath $path -PathType Leaf) ("Missing current receipt: "+$path)
 $file=Get-Item -LiteralPath $path
 Require ($file.LastWriteTimeUtc-ge$cutoff-and$file.LastWriteTimeUtc.Date-eq$cutoff.Date) ("Stale receipt rejected: "+$file.Name)
 return $file
}
$manifestPath=Join-Path $output 'manifest.json'
if(Test-Path -LiteralPath $manifestPath){$state=ReadNative $manifestPath|ConvertFrom-Json}else{
 $state=[pscustomobject]@{Patch='0.1.1-beta';SnapshotStatus='IN_PROGRESS';CompleteNativeStatus='PENDING';CurrentGraphicalStatus='PARTIAL';UpdatedUtc='';Files=@();Reports=@();RetainedRuns=@();Pending=@('Complete current native release suite','Current medical/Wither/Dragon graphical receipts','Current JEI and REI workstation checks')}
}
$files=[Collections.Generic.List[object]]::new();foreach($file in $state.Files){$files.Add($file)}
$reports=[Collections.Generic.List[object]]::new();foreach($report in $state.Reports){$reports.Add($report)}
$runs=[Collections.Generic.List[object]]::new();foreach($run in $state.RetainedRuns){$runs.Add($run)}
function RecordFile($relative,$file,$hash,$kind,$sourceHash,$sourceBytes){
 $existing=@($files|Where-Object {$_.File-eq$relative})
 if($existing.Count){Require ($existing[0].SHA256-eq$hash) ("An existing immutable receipt differs: "+$relative);return}
 $files.Add([ordered]@{File=$relative;Kind=$kind;SHA256=$hash;Bytes=(Get-Item -LiteralPath (Join-Path $output $relative)).Length;SourceSHA256=$sourceHash;SourceBytes=$sourceBytes;SourceModifiedUtc=$file.LastWriteTimeUtc.ToString('o')})
}
function CopyOriginal($relative,$target,$kind){
 $file=Fresh $relative;$hash=(Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash
 $destination=Join-Path $output $target;[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($destination))|Out-Null
 if(Test-Path -LiteralPath $destination){Require ((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash-eq$hash) ("Refusing to overwrite different archived evidence: "+$target)}
 else{Copy-Item -LiteralPath $file.FullName -Destination $destination}
 Require ((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash-eq$hash) ("Byte-copy verification failed: "+$target)
 RecordFile $target $file $hash $kind $hash $file.Length
 return $target
}
function ArchiveLog($relative){
 $file=Fresh $relative;$name=$file.Name
 $raw=CopyOriginal $relative ('logs/raw/'+$name) 'OriginalLogBytes'
 $text=ReadNative $file.FullName;$target='logs/'+$name;$destination=Join-Path $output $target
 $content=$utf8.GetBytes($text);$sha=[Security.Cryptography.SHA256]::Create()
 try{$hash=[BitConverter]::ToString($sha.ComputeHash($content)).Replace('-','')}finally{$sha.Dispose()}
 if(Test-Path -LiteralPath $destination){Require ((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash-eq$hash) ("Refusing to replace a different normalized log: "+$target)}
 else{[IO.File]::WriteAllBytes($destination,$content)}
 RecordFile $target $file $hash 'NativeLogTextUtf8' (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash $file.Length
 return $target
}
function CollectScreens($node,$items,$location){
 if($null-eq$node-or$node-is[string]-or$node.GetType().IsValueType){return}
 if($node-is[Collections.IEnumerable]-and$node-isnot[pscustomobject]){foreach($value in $node){CollectScreens $value $items $location};return}
 if($node.PSObject.Properties['Screenshot']){
  $label=$location;if($node.PSObject.Properties['View']){$label=[string]$node.View}
  $items.Add([ordered]@{Screenshot=[string]$node.Screenshot;View=$label;NativeRecord=$node})
 }
 foreach($property in $node.PSObject.Properties){if($property.Name-ne'Screenshot'){CollectScreens $property.Value $items ($location+'.'+$property.Name)}}
}
function ArchiveReport($name){
 Require ([IO.Path]::GetFileName($name)-eq$name-and$name-match'^graphical-[a-z0-9-]+\.json$') ('Unsafe report name: '+$name)
 $file=Fresh ('build/client-smoke/'+$name);$report=ReadNative $file.FullName|ConvertFrom-Json
 Require ($report.Result-eq'PASS'-and$report.ActualGraphicalClient-eq$true) ('Only an actual completed graphical PASS can be archived as passing: '+$name)
 foreach($field in @('DirectCriterionGrants','ProgressRevocations')){if($report.PSObject.Properties[$field]){Require ($report.$field-eq0) ('Direct criterion changes in '+$name)}}
 if($name-eq'graphical-beacon-ui-plain.json'){
  Require ($report.NativeHasteIVApplied-and$report.UnpaidIVConfirmationRejected-and$report.OriginalPlayerAndWorldRestored) 'Incomplete actual beacon transaction or restoration proof'
  Require ($report.NativeCurrencyConsumed.NetheriteIngots-eq1-and$report.NativeCurrencyConsumed.TetanizedWitherStars-eq1) 'Unexpected actual beacon payment'
  $actual=@($report.Scales|ForEach-Object {$_.AppliedGuiScale}|Sort-Object -Unique)
  Require ($actual.Count-eq4-and@($actual|Where-Object {$_-notin@(2,3,4,6)}).Count-eq0) 'Beacon scale coverage incomplete'
  foreach($scale in $report.Scales){Require ($scale.RequestedGuiScale-eq$scale.AppliedGuiScale-and$scale.NativeGeometry.ButtonLabelsFit-and$scale.NativeGeometry.NoControlOrSlotOverlap-and$scale.NativeGeometry.ViewportContainsFrame) 'Capped or overlapping beacon view'}
 }
 $copied=CopyOriginal $file.FullName ('reports/'+$name) 'OriginalGraphicalReport'
 $images=[Collections.Generic.List[object]]::new();CollectScreens $report $images '$'
 Require ($images.Count-gt0) ('No original screenshots referenced by '+$name)
 foreach($image in $images){
  $png=[string]$image.Screenshot
  Require ([IO.Path]::GetFileName($png)-eq$png-and$png-match'^rust-and-ruin-[a-z0-9_-]+\.png$') ('Unsafe native screenshot path: '+$png)
  $null=CopyOriginal ('build/client-smoke/screenshots/'+$png) ('screenshots/'+$png) 'OriginalNativePng'
 }
 if(!@($reports|Where-Object {$_.Name-eq$name}).Count){$reports.Add([ordered]@{Name=$name;Result='PASS';File=$copied;SourceModifiedUtc=$file.LastWriteTimeUtc.ToString('o');Screenshots=$images.ToArray()})}
}
if($ArchiveInitial){
 ArchiveReport 'graphical-beacon-ui-plain.json'
 $beaconLog=ArchiveLog '.tools/boss-fixes-2026-11-14/client-beacon-ui-last.log'
 Require ((ReadNative (Absolute '.tools/boss-fixes-2026-11-14/client-beacon-ui-last.log')).Contains('PASS: redesigned native beacon UI at GUI2/3/4/6')) 'Missing actual final beacon GUI PASS log'
 foreach($entry in @(
  @('beacon-external-resize','client-beacon-ui.log','FAILED TEST HARNESS','The window helper resized the actual viewport during the scale matrix. The controller correctly rejected a capped GUI scale. This attempt did not pass.'),
  @('beacon-minimized','client-beacon-ui-confirm.log','FAILED TEST HARNESS','The native test HWND was minimized with a zero client area. Startup timed out at phase0. The later fixture has bounded diagnostics and one measured startup recovery. This attempt did not pass.'),
  @('native-cancelled','native-final.log','CANCELLED','This native invocation was cancelled before completion. Its log is retained. No XML is associated with it: native-final.xml was a stale copy of the preceding failed run and is deliberately excluded.')
 )){
  $log=ArchiveLog ('.tools/boss-fixes-2026-11-14/'+$entry[1])
  if(!@($runs|Where-Object {$_.Id-eq$entry[0]}).Count){$runs.Add([ordered]@{Id=$entry[0];Outcome=$entry[2];Explanation=$entry[3];Log=$log})}
 }
 $nativeXmlFile=Fresh '.tools/boss-fixes-2026-11-14/native-second.xml'
 [xml]$native=ReadNative $nativeXmlFile.FullName;$cases=@($native.SelectNodes('//testcase'));$failed=@($cases|Where-Object {$_.failure-or$_.error})
 Require ($cases.Count-eq415-and$failed.Count-eq1) 'Retained native failure receipt changed; review its actual counts'
 $xml=CopyOriginal $nativeXmlFile.FullName 'reports/native-second.xml' 'OriginalFailedNativeXml'
 $log=ArchiveLog '.tools/boss-fixes-2026-11-14/native-second.log'
 if(!@($runs|Where-Object {$_.Id-eq'native-second'}).Count){
  $runs.Add([ordered]@{Id='native-second';Outcome='FAILED';Tests=$cases.Count;Passed=$cases.Count-$failed.Count;Failures=$failed.Count;FailedCase=[string]$failed[0].name;FailureMessage=[string]$failed[0].failure.message;Explanation='The creative variant-egg matrix used a cramped spawn fixture for the very large Ender Dragon. The full run finished414/415; it is retained as failed. The fixture was corrected for a later release run, whose outcome remains separate.';Xml=$xml;Log=$log})
 }
}
if($ArchiveClosedPlaytest){
 $relative='.tools/boss-fixes-2026-11-14/client-playtest.log';$file=Fresh $relative;$text=ReadNative $file.FullName
 Require ($text.Contains('Native grab/lift/drop/cooldown mismatch')-and$text.Contains('lift=9.261655631359204')-and$text.Contains('BUILD FAILED')) 'The retained combined run does not match the declared closed failure'
 Require ($text.Contains('specialRitual=false total=10 special=0')-and$text.Contains('specialRitual=true total=10 special=10')) 'The partial pillar observations are missing'
 $log=ArchiveLog $relative
 if(!@($runs|Where-Object {$_.Id-eq'combined-playtest'}).Count){
  $runs.Add([ordered]@{Id='combined-playtest';Outcome='FAILED TEST HARNESS';Log=$log;Explanation='The combined visible run completed the separately archived medical and Wither phases. It also logged a native mixed 200-HP ritual with 10 ordinary pillar crystals and a special 1100-HP ritual with 10 special pillar crystals. The later grab-height measurement failed at 9.261655631359204: its baseline preceded 18 approaches and its four-tick polling missed the peak. The subsequent smoke-only measurement records actual player height every native tick from the successful grab start; strict thresholds remain unchanged. That confirmation is separate. This composite run did not pass, and no old Dragon JSON is substituted.'})
 }
}
if($ArchiveAudio){
 $file=Fresh '.tools/boss-fixes-2026-11-14/audio-verification.json';$audio=ReadNative $file.FullName|ConvertFrom-Json
 Require ($audio.Result-eq'PASS'-and$audio.DecodedWithoutError-and$audio.DefeatCueMono-and$audio.DefeatCueSpeedAndPitch-eq.75-and$audio.LocalAttenuationBlocks-eq32) 'Incomplete audio file verification'
 foreach($entry in $audio.Files){
  Require ((Get-FileHash -LiteralPath (Absolute $entry.File) -Algorithm SHA256).Hash-eq$entry.SHA256) ('Audio media changed after its verification: '+$entry.File)
  Require ($entry.Metadata.streams[0].codec_name-eq'vorbis'-and$entry.Metadata.streams[0].channels-eq1-and$entry.Metadata.streams[0].sample_rate-eq'44100') 'Unexpected audio format'
 }
 $archived=CopyOriginal $file.FullName 'reports/audio-verification.json' 'OriginalAudioDecodeAndMetadataReport'
 $value=[pscustomobject]@{Result='PASS';Scope='File decoding and metadata only; no in-game playback claim';File=$archived;OriginalSeconds=$audio.Files[0].Metadata.format.duration;DefeatSeconds=$audio.Files[1].Metadata.format.duration;Codec='Vorbis';Channels=1;SampleRate=44100;SpeedAndPitch=.75;ConfiguredAttenuationBlocks=32}
 $state|Add-Member -NotePropertyName AudioVerification -NotePropertyValue $value -Force
}
if($ArchiveResourceRepeat){
 $file=Fresh '.tools/boss-fixes-2026-11-14/resource-reproducibility.json';$repeat=ReadNative $file.FullName|ConvertFrom-Json
 Require ($repeat.Result-eq'PASS'-and$repeat.ResourceFiles-eq409-and@($repeat.ChangedFiles).Count-eq0) 'Resource repeat did not preserve all 409 resource files byte for byte'
 $archived=CopyOriginal $file.FullName 'reports/resource-reproducibility.json' 'OriginalResourceReproducibilityReport'
 $state|Add-Member -NotePropertyName ResourceReproducibility -NotePropertyValue ([pscustomobject]@{Result='PASS';Files=$repeat.ResourceFiles;ChangedFiles=@();File=$archived}) -Force
}
foreach($report in $AddReports){ArchiveReport $report}
foreach($log in $AddLogs){$null=ArchiveLog $log}
if($NativeReleaseXml-or$NativeReleaseLog){
 Require ($NativeReleaseXml-and$NativeReleaseLog) 'Both complete release XML and its complete matching log are required'
 $xmlFile=Fresh $NativeReleaseXml;$logFile=Fresh $NativeReleaseLog;$text=ReadNative $logFile.FullName
 [xml]$native=ReadNative $xmlFile.FullName;$cases=@($native.SelectNodes('//testcase'));$failures=@($native.SelectNodes('//failure|//error'))
 $receipt=[regex]::Matches($text,'(\d+) GAME TESTS COMPLETE')
 Require ($cases.Count-gt0-and$failures.Count-eq0-and$receipt.Count-eq1-and[int]$receipt[0].Groups[1].Value-eq$cases.Count-and$text.Contains('BUILD SUCCESSFUL')) 'The proposed final native receipt is incomplete, failed or mismatched'
 $xml=CopyOriginal $xmlFile.FullName ('reports/'+$xmlFile.Name) 'OriginalPassingNativeXml';$log=ArchiveLog $logFile.FullName
 if(!@($runs|Where-Object {$_.Id-eq'native-release'}).Count){$runs.Add([ordered]@{Id='native-release';Outcome='PASS';Tests=$cases.Count;Passed=$cases.Count;Failures=0;Xml=$xml;Log=$log;Explanation='Completed current native release run; counts verified against its original XML and complete log.'})}
 $state.CompleteNativeStatus='PASS'
}
$expectedReports=@('graphical-beacon-ui-plain.json','graphical-medical-plain.json','graphical-wither-patch-plain.json','graphical-dragon-ritual-plain.json','graphical-medical-jei.json','graphical-forge-jei.json','graphical-medical-rei.json','graphical-forge-rei.json')
$missing=@($expectedReports|Where-Object {$name=$_;!@($reports|Where-Object {$_.Name-eq$name}).Count})
$state.Pending=@();if($state.CompleteNativeStatus-ne'PASS'){$state.Pending+=,'Complete current native release suite'};$state.Pending+=@($missing)
$state.CurrentGraphicalStatus=if($missing.Count){'PARTIAL'}else{'PASS'}
if($MarkComplete){Require ($state.CompleteNativeStatus-eq'PASS'-and!$missing.Count-and$state.ResourceReproducibility.Result-eq'PASS') 'A complete snapshot requires every current approved report and a complete passing native run';$state.SnapshotStatus='SCOPED_CHECKS_COMPLETE'}
foreach($run in $runs){foreach($pair in @(@('mixed200-HP','mixed 200-HP'),@('special1100-HP','special 1100-HP'),@('with10 ordinary','with 10 ordinary'),@('with10 special','with 10 special'),@('at9.261655631359204','at 9.261655631359204'),@('preceded18approaches','preceded 18 approaches'))){$run.Explanation=$run.Explanation.Replace($pair[0],$pair[1])}}
$state.Files=$files.ToArray();$state.Reports=$reports.ToArray();$state.RetainedRuns=$runs.ToArray();$state.UpdatedUtc=[DateTime]::UtcNow.ToString('o')
$pendingText=if($state.Pending.Count){($state.Pending -join '; ')}else{'No required receipts pending'}
[IO.File]::WriteAllText($manifestPath,($state|ConvertTo-Json -Depth 60)+[Environment]::NewLine,$utf8)
$body=[Text.StringBuilder]::new();$links=[Text.StringBuilder]::new()
foreach($report in $reports){
 $null=$links.Append('<li><a href="#'+(Html $report.Name)+'">'+(Html $report.Name)+'</a></li>')
 $scope=''
 if($report.Name-like'graphical-medical-*'){$scope='Controlled placed writer and medic. Prescriptions, supply delivery, trade, antitoxin use and the cure advancement use actual native interactions. The medic fixture has AI paused.'}
 if($report.Name-eq'graphical-wither-patch-plain.json'){$scope='Actual Creative and Survival final-skull placement, native 650-HP Withers, native timed two-minion waves with 20 HP each, and actual client finishing attacks. No direct spawn-wave, encounter-tick, or cooldown writes. Survival uses synchronized camera flight and invulnerability; each boss is reduced to one HP only after its full-health and timed-wave proof. Loot, native 50 XP, special-star-only drops and minion cleanup are verified. This is functionality testing, not a full survival balance fight.'}
 if($report.Name-eq'graphical-dragon-ritual-plain.json'){$scope='Actual native mixed and special End rituals, 20 accepted combat damage, and 9.99 blocks of mouth lift measured from the successful grab on every server tick. The native 2400-tick cooldown, fall, 500 XP, one sword, one egg and actual egg placement passed. Exactly one positional, non-relative LINEAR defeat-sound instance was observed. Controlled actor abilities, seeded approaches and a one-HP finishing fixture remain disclosed; this is not a full survival balance playthrough.'}
 $null=$body.Append('<section id="'+(Html $report.Name)+'"><h2>PASS: '+(Html $report.Name)+'</h2><p><a href="'+(Html $report.File)+'">Original native JSON</a> &middot; '+@($report.Screenshots).Count+' original screenshot references.</p><p>'+(Html $scope)+'</p><div class="grid">')
 foreach($image in $report.Screenshots){$title=[string]$image.View;if($image.NativeRecord.PSObject.Properties['Mode']){$title=$image.NativeRecord.Mode+' / '+$title};if($image.NativeRecord.PSObject.Properties['RequestedGuiScale']){$title+=' / GUI '+$image.NativeRecord.RequestedGuiScale};$url='screenshots/'+$image.Screenshot;$null=$body.Append('<figure><a href="'+(Html $url)+'"><img src="'+(Html $url)+'" loading="lazy" decoding="async" alt="'+(Html $title)+'"></a><figcaption>'+(Html $title)+' &middot; <a href="'+(Html $url)+'">original PNG</a></figcaption></figure>')}
 $null=$body.Append('</div></section>')
}
$audioHtml=''
if($state.PSObject.Properties['AudioVerification']){
 $audio=$state.AudioVerification
 $audioHtml='<section><h2>PASS: audio file decoding and metadata</h2><p><a href="'+(Html $audio.File)+'">Original verification JSON</a>. Both files decode without errors as mono 44.1kHz Vorbis. The source is '+(Html $audio.OriginalSeconds)+' seconds; the slower/reverberant defeat cue is '+(Html $audio.DefeatSeconds)+' seconds, with 0.75 speed/pitch and a configured 32-block attenuation distance.</p><p>This is file/metadata verification; it does not claim an in-game playback observation.</p></section>'
}
$passedLogs=[Text.StringBuilder]::new()
foreach($file in $files|Where-Object {$_.Kind-eq'NativeLogTextUtf8'-and$_.File-like'logs/client-*'}){
 $text=ReadNative (Join-Path $output $file.File)
 if($text.Contains('PASS:')-and$text.Contains('BUILD SUCCESSFUL')-and!$text.Contains('FAIL:')){
  $name=[IO.Path]::GetFileName($file.File)
  $null=$passedLogs.Append('<li><a href="'+(Html $file.File)+'">'+(Html $name)+'</a> &middot; <a href="logs/raw/'+(Html $name)+'">original bytes</a></li>')
 }
}
$resourceHtml=''
if($state.PSObject.Properties['ResourceReproducibility']){$resourceHtml='<section><h2>PASS: repeated resource generation</h2><p>All 409 generated resource files remained byte-identical, with no changed files. <a href="'+(Html $state.ResourceReproducibility.File)+'">Original equality receipt</a>. This repeat preceded the final REI run; the final build followed REI.</p></section>'}
$nativeHtml=''
foreach($run in $runs|Where-Object {$_.Id-eq'native-release'-and$_.Outcome-eq'PASS'}){
 $nativeHtml='<section><h2>PASS: complete current native release suite</h2><p>'+ $run.Passed +' / '+ $run.Tests +' tests passed, with '+$run.Failures+' failures. <a href="'+(Html $run.Xml)+'">Original XML</a> &middot; <a href="'+(Html $run.Log)+'">Complete UTF-8 log</a>. Counts match the complete build/server run.</p><p>The earlier failed and cancelled runs remain below and are not relabeled.</p></section>'
}
$runHtml=[Text.StringBuilder]::new()
foreach($run in $runs|Where-Object {$_.Outcome-ne'PASS'}){$null=$runHtml.Append('<li><strong>'+(Html $run.Outcome)+' &mdash; '+(Html $run.Id)+'</strong><p>'+(Html $run.Explanation)+'</p><a href="'+(Html $run.Log)+'">Readable UTF-8 log</a>');if($run.PSObject.Properties['Xml']){$null=$runHtml.Append(' &middot; <a href="'+(Html $run.Xml)+'">Original XML</a>')};$null=$runHtml.Append('</li>')}
$completedLines=($reports|ForEach-Object {'- ['+$_.Name+']('+ $_.File +'): '+@($_.Screenshots).Count+' original screenshot references.'}) -join [Environment]::NewLine
$status='In progress: scoped passing evidence'
if($state.SnapshotStatus-ne'IN_PROGRESS'){$status='Complete: all listed scoped checks passed'}
$html=@"
<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Rust &amp; Ruin 0.1.1-beta patch checks</title>
<style>body{margin:0;background:#1b1e20;color:#e7e7df;font:16px/1.6 system-ui,sans-serif}main{max-width:1400px;margin:auto;padding:30px}a{color:#f0bb79}h1{line-height:1.2}h2{border-bottom:1px solid #56605c;padding-bottom:8px;margin-top:40px}.notice{padding:18px;background:#34372e;border-left:4px solid #cb9860}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(310px,1fr));gap:18px}figure{margin:0;padding:10px;background:#2d3233;border:1px solid #59615e;overflow-wrap:anywhere}img{display:block;width:100%;height:auto}figcaption{font-size:14px;padding-top:9px}small{color:#b9c2bb}li p{margin:4px 0 8px}li{margin-bottom:14px}</style>
<main><h1>Rust &amp; Ruin 0.1.1-beta<br>Current patch evidence</h1><div class="notice"><strong>$(Html $status)</strong><p>Current complete native suite: $(Html $state.CompleteNativeStatus). Only the completed graphical reports listed below are verified in this snapshot. Additional checks are included after their own current receipts finish. This snapshot is not a blanket release pass.</p><p><a href="manifest.json">SHA256 manifest</a> &middot; <a href="README.md">Scope and fixture disclosures</a><br><small>Updated $(Html $state.UpdatedUtc)</small></p><p>Pending: $(Html $pendingText)</p></div><h2>How to read this evidence</h2><p>All screenshots are unedited native Minecraft framebuffer captures. Original JSON and XML bytes are retained. Logs have readable UTF-8 copies plus original raw bytes in logs/raw; both hashes are recorded. Earlier failed and cancelled attempts remain labeled below. No older expansion gallery is substituted for these patch receipts.</p><p>The beacon matrix uses a controlled flat world and ingredient fixtures. Actual client menus, currency movement and a final paid IV activation verify the transaction. It checks II, III, unpaid IV, paid IV and Regeneration at uncapped GUI scales 2, 3, 4, and 6, then restores its player, world and window.</p><ul>$links</ul>$body$nativeHtml$resourceHtml$audioHtml<h2>Complete passing client logs</h2><ul>$passedLogs</ul><h2>Retained failed and cancelled attempts</h2><ul>$runHtml</ul><p>These attempts are not passing release evidence. In particular, no stale XML is attached to the cancelled run.</p><footer>Local static gallery. No scripts, external assets, synthetic screenshots or hosting.</footer></main></html>
"@
[IO.File]::WriteAllText((Join-Path $output 'index.html'),$html,$utf8)
$resourceMarkdown=''
if($state.PSObject.Properties['ResourceReproducibility']){$resourceMarkdown='Repeated generation preserved all 409 resource files byte for byte: [original equality receipt](reports/resource-reproducibility.json). The repeat preceded the final REI run; the final build followed REI.'}
$audioMarkdown=''
if($state.PSObject.Properties['AudioVerification']){$audioMarkdown='Audio file decoding/metadata also passed: [original JSON](reports/audio-verification.json). Both files are mono 44.1kHz Vorbis; the source is '+$state.AudioVerification.OriginalSeconds+' seconds and the processed cue '+$state.AudioVerification.DefeatSeconds+' seconds. This does not claim in-game playback.'}
$readme=@"
# Rust & Ruin 0.1.1-beta patch evidence

Status: **$(Html $state.SnapshotStatus)**. Current complete native release run: **$(Html $state.CompleteNativeStatus)**. Open [the local gallery](index.html) or inspect [manifest.json](manifest.json) for original receipt paths, dates, byte counts and SHA256 hashes.

## Completed graphical evidence

The beacon report records uncapped GUI scales 2, 3, 4, and 6 and 20 original screenshots: II, III, IV without its star, IV with payment, and Regeneration at each scale. It verifies all 10 controls and 38 native slots fit without overlap, blocks unpaid IV confirmation, consumes exactly one netherite ingot and one Tetanized Wither Star through the real menu, applies Haste IV, and restores the fixture. No advancement criteria are granted or reset.

Completed reports:

$completedLines

$audioMarkdown

$resourceMarkdown

## Scope and remaining work

Only completed reports explicitly added to this snapshot count as passing. Pending receipts: $pendingText. Completed graphical phases are listed in the manifest and gallery; the combined visible-client log is archived only after it closes. A previous 414/415 run is retained as **failed**, and the later interrupted native invocation is **cancelled**. The cancelled run has no XML here: its apparent XML was stale and is deliberately excluded.

Both failed beacon attempts remain: one external helper resized the live viewport, and the other client was minimized with zero client area until startup timed out. These are recorded as failed harness attempts, followed by the independent successful matrix. The closed combined playtest also remains failed overall: its medical and Wither phases passed separately, followed by a grab-height measurement failure. Its original log and exact fixture explanation are retained; the independent Dragon confirmation is recorded separately when complete.

All PNGs are original native framebuffer captures, copied byte for byte. Logs are additionally normalized to UTF-8 for reading; logs/raw contains the unmodified originals. The manifest records source and archived hashes. Generated HTML, Markdown and the manifest itself are indexes, not native receipts.

The beacon uses controlled player abilities, a manually constructed pyramid and ingredient fixtures. It is a focused UI/payment check, not an ordinary survival playthrough or a guarantee of every modpack combination. The current Wither proof uses actual Creative and Survival rituals and timed waves, but has camera-flight/invulnerability fixtures and reduces each boss to one HP only for the finishing attack. Its native loot/XP checks do not claim a full survival balance fight. The medical proof uses a placed writer and an AI-paused medic; outputs, deliveries, trades, cure use and its advancement are real native actions.
"@
[IO.File]::WriteAllText((Join-Path $output 'README.md'),$readme,$utf8)
Write-Output ('Archived '+$reports.Count+' passing graphical reports; '+$files.Count+' hashed evidence files; full native status='+$state.CompleteNativeStatus+'.')
