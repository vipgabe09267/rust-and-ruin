param([string]$Report)
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$utf8=[Text.UTF8Encoding]::new($false,$true)
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Web.Extensions
$jsonParser=[Web.Script.Serialization.JavaScriptSerializer]::new()
$jsonParser.MaxJsonLength=16777216
Push-Location $projectRoot
try {
    $paths=@(& git ls-files)
    if($LASTEXITCODE-ne 0){throw 'Cannot inventory tracked files'}
    $paths+=@(& git ls-files --others --exclude-standard)
    if($LASTEXITCODE-ne 0){throw 'Cannot inventory new project files'}
    $deleted=@(& git ls-files --deleted)
    $paths=@($paths|Sort-Object -Unique|Where-Object {$_ -notin $deleted})
    $inventory=@()
    $lang=Get-Content -LiteralPath 'src/main/resources/assets/tetanusmod/lang/en_us.json' -Raw -Encoding UTF8|ConvertFrom-Json
    foreach($relative in $paths) {
        $path=Join-Path $projectRoot $relative
        if(!(Test-Path -LiteralPath $path -PathType Leaf)){throw "Missing tracked file: $relative"}
        $extension=[IO.Path]::GetExtension($relative).ToLowerInvariant()
        $check='File inventory and encoding; execution reviewed separately'
        if($extension-notin@('.png','.mp3','.ogg','.wav','.jar','.nbt')) {
            $bytes=[IO.File]::ReadAllBytes($path)
            $originalLog=$extension-eq'.log'-and($relative-match'^validation/(camp-audio|clinic-advancements)-2026-10-25/evidence/[^/]+\.log$'-or$relative-match'^validation/(boss-fixes|release-ready)-2026-11-14/logs/(?:raw/)?(?:(?:final|pre-final)/)?[^/]+\.log$'-or$relative-match'^validation/expansion-2026-11-14/reports/[^/]+\.log$')
            if($originalLog-and$bytes.Length-ge2-and$bytes[0]-eq255-and$bytes[1]-eq254){
                # Native Windows PowerShell receipts retain their original UTF-16LE bytes.
                $contents=[Text.UnicodeEncoding]::new($false,$true,$true).GetString($bytes)
                $check='Original UTF-16LE execution log; strict decoding and text integrity checked'
            }else{$contents=$utf8.GetString($bytes)}
            if($contents.Contains([char]0)){throw "NUL in text file: $relative"}
            if($contents-match'(?m)^(<<<<<<< |=======\s*$|>>>>>>> )'){throw "Conflict marker: $relative"}
            # Preserve glyphs already emitted by the native console in immutable receipts.
            if(!$originalLog-and$contents.Contains([char]0xFFFD)){throw "Replacement character in text: $relative"}
        }
        switch($extension) {
            '.json' {
                # Original UTF-8 receipts may retain a BOM; decode strictly, then remove
                # only that leading encoding marker before parsing their unchanged bytes.
                $jsonText=$contents
                if($jsonText.StartsWith([string][char]0xFEFF,[StringComparison]::Ordinal)){$jsonText=$jsonText.Substring(1)}
                try{$null=$jsonParser.DeserializeObject($jsonText)}
                catch{throw "Invalid JSON in ${relative}: $($_.Exception.Message)"}
                $check='Parsed JSON; resource references checked by Gradle and runtime'
            }
            '.wav' {
                $wave=[IO.File]::ReadAllBytes($path)
                if($wave.Length-lt44-or[Text.Encoding]::ASCII.GetString($wave,0,4)-ne'RIFF'-or[Text.Encoding]::ASCII.GetString($wave,8,4)-ne'WAVE'-or[BitConverter]::ToUInt32($wave,4)-ne$wave.Length-8){throw "Invalid RIFF/WAVE header: $relative"}
                $check='RIFF/WAVE source header validated; full audio decode recorded separately'
            }
            '.xml' {$null=[xml]$contents;$check='Parsed XML report'}
            '.ps1' {
                $tokens=$null;$errors=$null
                $null=[Management.Automation.Language.Parser]::ParseInput($contents,[ref]$tokens,[ref]$errors)
                if($errors.Count){throw "PowerShell syntax error in ${relative}: $($errors[0].Message)"}
                $check='PowerShell syntax parsed; manual review recorded separately'
            }
            '.java' {
                foreach($match in [regex]::Matches($contents,'Text\.translatable\("((?:tooltip|message|guide|commands|gui|advancements|effect|item|block|entity|jei|enchantment|subtitles)\.(?:tetanusmod|rustandruin)\.[^"\r\n]+)"')) {
                    $key=$match.Groups[1].Value
                    if(!$key.EndsWith('.')-and!$lang.PSObject.Properties[$key]){throw "Missing translation $key used by $relative"}
                }
                $check='UTF-8 and static translation keys; compilation and manual review recorded separately'
            }
            '.png' {
                $image=[Drawing.Bitmap]::FromFile($path)
                try {
                    if($image.Width-lt 1-or$image.Height-lt 1){throw "Empty image: $relative"}
                    if($relative-match'^src/main/resources/assets/tetanusmod/textures/(item|mob_effect)/') {
                        for($x=0;$x-lt$image.Width;$x++){if($image.GetPixel($x,0).A-or$image.GetPixel($x,$image.Height-1).A){throw "Sprite touches top/bottom edge: $relative"}}
                        for($y=0;$y-lt$image.Height;$y++){if($image.GetPixel(0,$y).A-or$image.GetPixel($image.Width-1,$y).A){throw "Sprite touches side edge: $relative"}}
                    }
                }finally{$image.Dispose()}
                $check='Decoded image; runtime item/status sprite margins checked'
            }
            '.nbt' {
                $bytes=[IO.File]::ReadAllBytes($path)
                if($bytes.Length-lt 3-or$bytes[0]-ne 31-or$bytes[1]-ne 139){throw "Structure is not gzip NBT: $relative"}
                $check='Gzip NBT header; structure runtime checks recorded separately'
            }
            '.mp3' {$check='Source audio; decoded separately with FFprobe/FFmpeg'}
            '.ogg' {$check='Packaged audio; decoded separately with FFprobe/FFmpeg'}
            '.jar' {$check='Official Gradle wrapper checksum checked by Audit-Release'}
            '.cs' {$check='UTF-8; compilation and resource-generation results recorded separately'}
            '.md' {
                foreach($match in [regex]::Matches($contents,'!?\[[^\]]*\]\(([^)]+)\)')) {
                    $target=$match.Groups[1].Value
                    if($target-match'^(https?://|#|mailto:|<)'){continue}
                    $target=($target-split'#',2)[0]
                    if($target-and!(Test-Path -LiteralPath (Join-Path ([IO.Path]::GetDirectoryName($path)) $target))){throw "Broken local link $target in $relative"}
                }
                $check='UTF-8 and local links checked; prose reviewed separately'
            }
        }
        $inventory+=[pscustomobject]@{Path=$relative;Bytes=(Get-Item -LiteralPath $path).Length;SHA256=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash;Check=$check}
    }
    $result=[pscustomobject]@{Result='PASS';Files=$inventory.Count;Scope='Tracked files plus new nonignored project files; compiler/runtime checks run separately';Inventory=$inventory}
    $json=$result|ConvertTo-Json -Depth 6
    if($Report){
        $reportPath=[IO.Path]::GetFullPath($Report)
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($reportPath))|Out-Null
        [IO.File]::WriteAllText($reportPath,$json+"`n",[Text.UTF8Encoding]::new($false))
    }
    [pscustomobject]@{Result='PASS';Files=$inventory.Count;Kinds=($inventory|Group-Object Check|ForEach-Object {"$($_.Count) $($_.Name)"})}|ConvertTo-Json -Depth 4
}finally{Pop-Location}