param([string]$FfmpegPath)
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if(!$FfmpegPath){
 $bundled=Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools/ffmpeg') -Filter ffmpeg.exe -Recurse -ErrorAction SilentlyContinue|Select-Object -First 1
 if($bundled){$FfmpegPath=$bundled.FullName}else{$command=Get-Command ffmpeg -ErrorAction SilentlyContinue;if($command){$FfmpegPath=$command.Source}}
}
if(!$FfmpegPath){throw 'FFmpeg is required to regenerate the dragon defeat cue.'}
$source=Join-Path $projectRoot 'src/main/resources/assets/tetanusmod/sounds/tetanus_effect.ogg'
$target=Join-Path $projectRoot 'src/main/resources/assets/tetanusmod/sounds/dragon_defeated.ogg'
& $FfmpegPath -hide_banner -loglevel error -y -fflags +bitexact -i $source -map_metadata -1 -af 'asetrate=33075,aresample=44100,aecho=0.8:0.65:80|151|243:0.27|0.18|0.10,volume=0.85' -ac 1 -ar 44100 -c:a libvorbis -q:a 5 -flags:a +bitexact -fflags +bitexact $target
if($LASTEXITCODE-ne0){throw 'FFmpeg failed to encode the slowed dragon defeat sound.'}
$path=Join-Path $projectRoot 'src/main/resources/assets/tetanusmod/sounds.json'
$sounds=Get-Content -LiteralPath $path -Raw -Encoding UTF8|ConvertFrom-Json
$sounds|Add-Member -NotePropertyName dragon_defeated -NotePropertyValue @{subtitle='subtitles.tetanusmod.dragon_defeated';sounds=@(@{name='tetanusmod:dragon_defeated';stream=$false;attenuation_distance=32})} -Force
[IO.File]::WriteAllText($path,($sounds|ConvertTo-Json -Depth 70)+"`n",[Text.UTF8Encoding]::new($false))
Write-Output 'Generated mono dragon victory voice: 0.75x pitch/speed and three short reverberation reflections; local32block attenuation.'
