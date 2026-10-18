param([string]$Jar)
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if(!$Jar){$Jar=Join-Path $projectRoot 'build/libs/rust-and-ruin-0.1.0-beta.jar'}
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.Drawing
$wrapperHash=(Get-FileHash -LiteralPath (Join-Path $projectRoot 'gradle/wrapper/gradle-wrapper.jar') -Algorithm SHA256).Hash
if($wrapperHash-ne'D3B261C2820E9E3D8D639ED084900F11F4A86050A8F83342ADE7B6BC9B0D2BDD'){throw 'Wrapper JAR differs from official Gradle 8.6 checksum'}
$properties=[IO.File]::ReadAllText((Join-Path $projectRoot 'gradle/wrapper/gradle-wrapper.properties'))
if(!$properties.Contains('distributionSha256Sum=9631d53cf3e74bfa726893aee1f8994fee4e060c401335946dba2156f440f24c')){throw 'Gradle distribution checksum is not pinned'}
$archive=[IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($Jar))
function Read-JsonEntry($name){$entry=$archive.GetEntry($name);if(!$entry){throw "Missing $name"};$reader=[IO.StreamReader]::new($entry.Open());try{return($reader.ReadToEnd()|ConvertFrom-Json)}finally{$reader.Dispose()}}
try {
    $entryNames=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach($entry in $archive.Entries){
        if(!$entryNames.Add($entry.FullName)){throw "Duplicate archive entry: $($entry.FullName)"}
        if($entry.FullName.StartsWith('/')-or$entry.FullName-match'^[A-Za-z]:'-or$entry.FullName.Contains('\')-or($entry.FullName-split'/')-contains'..'){throw "Unsafe archive path: $($entry.FullName)"}
        if($entry.FullName-match'(^|/)(\.env|credentials|id_rsa|\.git)(/|$)' -or $entry.FullName-match'^(mezz/|me/shedaniel/|dev/architectury/|com/tetanusmod/test/)'){throw "Private/development content bundled: $($entry.FullName)"}
    }
    $meta=Read-JsonEntry 'fabric.mod.json'
    if($meta.id-ne'tetanusmod'-or$meta.version-ne'0.1.0-beta'){throw 'Unexpected mod identity/version'}
    if(@($meta.depends.PSObject.Properties).Count-ne 4){throw 'Missing required runtime dependency declarations'}
    $extra=@($meta.depends.PSObject.Properties.Name|Where-Object {$_-notin@('fabricloader','fabric-api','minecraft','java')})
    if($extra.Count){throw 'Unexpected mandatory mod dependencies'}
    foreach($name in @('tetanus_vaccine','protective_gloves','field_guide')){
        $null=Read-JsonEntry "assets/tetanusmod/models/item/$name.json"
        $null=Read-JsonEntry "data/tetanusmod/recipes/$name.json"
        if(!$archive.GetEntry("assets/tetanusmod/textures/item/$name.png")){throw "Missing $name texture"}

    }
    if(!$archive.GetEntry('assets/tetanusmod/textures/mob_effect/tetanus_resistance.png')){throw 'Missing resistance icon'}
    $resourceRoot=Join-Path $projectRoot 'src/main/resources'
    $resources=@(Get-ChildItem -LiteralPath $resourceRoot -Recurse -File)
    foreach($resource in $resources) {
        $name=$resource.FullName.Substring($resourceRoot.Length+1).Replace('\','/')
        $entry=$archive.GetEntry($name)
        if(!$entry){throw "Source resource missing from JAR: $name"}
        if($name-in@('fabric.mod.json','tetanus.mixins.json')) {
            $sourceJson=[IO.File]::ReadAllText($resource.FullName)
            if($name-eq'fabric.mod.json'){$sourceJson=$sourceJson.Replace('${version}','0.1.0-beta')}
            $expected=$sourceJson|ConvertFrom-Json
            $actual=Read-JsonEntry $name
            if($name-eq'tetanus.mixins.json') {
                if($actual.refmap-ne'rust-and-ruin-refmap.json'-or!$archive.GetEntry($actual.refmap)){throw 'Missing or unexpected remapped mixin references'}
                $actual.PSObject.Properties.Remove('refmap')
            }
            if(($expected|ConvertTo-Json -Compress -Depth 40)-cne($actual|ConvertTo-Json -Compress -Depth 40)){throw "Stale processed metadata: $name"}
        }else {
            $hash=[Security.Cryptography.SHA256]::Create();$stream=$entry.Open()
            try{$packaged=[BitConverter]::ToString($hash.ComputeHash($stream)).Replace('-','')}
            finally{$stream.Dispose();$hash.Dispose()}
            if($packaged-ne(Get-FileHash -LiteralPath $resource.FullName -Algorithm SHA256).Hash){throw "Stale or altered packaged resource: $name"}
        }
    }
    foreach($entry in $archive.Entries|Where-Object FullName -like '*.png') {
        $stream=$entry.Open()
        try {
            $bitmap=[Drawing.Bitmap]::new($stream)
            try {
                if($entry.FullName-match'^assets/tetanusmod/textures/(item|mob_effect)/') {
                    for($x=0;$x-lt$bitmap.Width;$x++){if($bitmap.GetPixel($x,0).A-or$bitmap.GetPixel($x,$bitmap.Height-1).A){throw "Clipped packaged sprite: $($entry.FullName)"}}
                    for($y=0;$y-lt$bitmap.Height;$y++){if($bitmap.GetPixel(0,$y).A-or$bitmap.GetPixel($bitmap.Width-1,$y).A){throw "Clipped packaged sprite: $($entry.FullName)"}}
                }
            }finally{$bitmap.Dispose()}
        }finally{$stream.Dispose()}
    }
    $why=Read-JsonEntry 'data/tetanusmod/advancements/why_did_i_eat_that.json'
    $strange=Read-JsonEntry 'data/tetanusmod/advancements/strange_meat.json'
    if(!$why.criteria.get_salmonella-or!$strange.criteria.obtain_infected_meat){throw 'Advancement triggers changed'}
    [pscustomobject]@{Result='PASS';Jar=[IO.Path]::GetFullPath($Jar);Bytes=(Get-Item -LiteralPath $Jar).Length;SHA256=(Get-FileHash -LiteralPath $Jar -Algorithm SHA256).Hash;OfficialWrapperSHA256=$wrapperHash;Pngs=@($archive.Entries|Where-Object FullName -like '*.png').Count;MandatoryMods='Fabric API';DevelopmentCodeBundled=$false}|ConvertTo-Json
}finally{$archive.Dispose()}
