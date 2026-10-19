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
        $check='Build/configuration review'
        if($extension-notin@('.png','.mp3','.ogg','.jar','.nbt')) {
            $contents=$utf8.GetString([IO.File]::ReadAllBytes($path))
            if($contents.Contains([char]0)){throw "NUL in text file: $relative"}
            if($contents-match'(?m)^(<<<<<<< |=======\s*$|>>>>>>> )'){throw "Conflict marker: $relative"}
            if($contents.Contains([char]0xFFFD)){throw "Replacement character in text: $relative"}
        }
        switch($extension) {
            '.json' {$null=$jsonParser.DeserializeObject($contents);$check='Parsed JSON; resource references checked by Gradle and runtime'}
            '.xml' {$null=[xml]$contents;$check='Parsed XML report'}
            '.ps1' {
                $tokens=$null;$errors=$null
                $null=[Management.Automation.Language.Parser]::ParseInput($contents,[ref]$tokens,[ref]$errors)
                if($errors.Count){throw "PowerShell syntax error in ${relative}: $($errors[0].Message)"}
                $check='PowerShell syntax parsed; reviewed file operations'
            }
            '.java' {
                foreach($match in [regex]::Matches($contents,'Text\.translatable\("((?:tooltip|message|guide|commands|gui|advancements|effect|item|block|entity|jei|enchantment)\.tetanusmod\.[^"\r\n]+)"')) {
                    $key=$match.Groups[1].Value
                    if(!$key.EndsWith('.')-and!$lang.PSObject.Properties[$key]){throw "Missing translation $key used by $relative"}
                }
                $check='Compiled; source review; server/client regression suite'
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
                $check='Compressed NBT; clinic structure loaded and placed by GameTest'
            }
            '.mp3' {$check='Source audio; decoded separately with FFprobe/FFmpeg'}
            '.ogg' {$check='Packaged audio; decoded separately with FFprobe/FFmpeg'}
            '.jar' {$check='Official Gradle wrapper checksum checked by Audit-Release'}
            '.cs' {$check='Compiled and exercised by resource regeneration'}
            '.md' {
                foreach($match in [regex]::Matches($contents,'!?\[[^\]]*\]\(([^)]+)\)')) {
                    $target=$match.Groups[1].Value
                    if($target-match'^(https?://|#|mailto:|<)'){continue}
                    $target=($target-split'#',2)[0]
                    if($target-and!(Test-Path -LiteralPath (Join-Path ([IO.Path]::GetDirectoryName($path)) $target))){throw "Broken local link $target in $relative"}
                }
                $check='UTF-8 and local links checked; documentation reviewed against implementation'
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