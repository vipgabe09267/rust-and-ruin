$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$root=Join-Path $PSScriptRoot '../src/main/resources/assets/tetanusmod/textures'
$colors=@{
    'o' = '#382D2D'
    'd' = '#4B474A'
    's' = '#757A79'
    'h' = '#A6ACA7'
    'c' = '#9B5140'
    'r' = '#CC7149'
    'g' = '#F4A36B'
    'u' = '#3A564F'
    't' = '#64877B'
    'v' = '#91B3A0'
    'w' = '#F5DEB5'
    'b' = '#C0A477'
    'm' = '#88644C'
}

function Write-Sprite($file, [string[]]$rows, [int]$padding=0){
    $bitmap=[Drawing.Bitmap]::new(16+$padding*2,16+$padding*2)
    try {
        for($y=0;$y-lt16;$y++){
            for($x=0;$x-lt16;$x++){
                $pixel=[string]$rows[$y][$x]
                if($pixel-eq'.'){continue}
                $bitmap.SetPixel($x+$padding,$y+$padding,[Drawing.ColorTranslator]::FromHtml($colors[$pixel]))
            }
        }
        $bitmap.Save((Join-Path $root $file),[Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $bitmap.Dispose()
    }
}

$magnet=@(
    '................'
    '..oooo...oooo...'
    '.oggro...orrgo..'
    '.orrco...ocrro..'
    '.ocroo...oorco..'
    '.odhso...osdho..'
    '.odhso...osdho..'
    '.odhso...osdho..'
    '.odhsso.oosdho..'
    '..odhssossdho...'
    '..oddhsssdhso...'
    '...oddhdhsoo....'
    '....oooooo......'
    '................'
    '................'
    '................'
)
$splint=@(
    '................'
    '.........ooo....'
    '........omwo....'
    '.......omwbo....'
    '......ouuvo.....'
    '.....outuvo.....'
    '....omuutuo.....'
    '...omwouuo......'
    '..omwbwoo.......'
    '.omwbwo.........'
    '.owbwo..........'
    '..ooo...........'
    '................'
    '................'
    '................'
    '................'
)
Write-Sprite 'item/rust_magnet.png' $magnet
Write-Sprite 'item/field_splint.png' $splint
Write-Sprite 'mob_effect/splinted.png' $splint 1
