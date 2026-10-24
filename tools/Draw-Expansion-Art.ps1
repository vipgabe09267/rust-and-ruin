$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$textureRoot=Join-Path $PSScriptRoot '../src/main/resources/assets/tetanusmod/textures'
function Color($hex){[Drawing.ColorTranslator]::FromHtml($hex)}
function Rect($g,$color,$x,$y,$w,$h){$brush=[Drawing.SolidBrush]::new((Color $color));try{$g.FillRectangle($brush,[int]$x,[int]$y,[int]$w,[int]$h)}finally{$brush.Dispose()}}
function Poly($g,$color,$coords){$brush=[Drawing.SolidBrush]::new((Color $color));try{$points=[Drawing.Point[]]@($coords|ForEach-Object {[Drawing.Point]::new($_[0],$_[1])});$g.FillPolygon($brush,$points)}finally{$brush.Dispose()}}
function Line($g,$color,$x1,$y1,$x2,$y2,$width=1){$pen=[Drawing.Pen]::new((Color $color),$width);try{$g.DrawLine($pen,$x1,$y1,$x2,$y2)}finally{$pen.Dispose()}}
function Sprite($name,$draw){
    $small=[Drawing.Bitmap]::new(32,32);$g=[Drawing.Graphics]::FromImage($small);$g.SmoothingMode=[Drawing.Drawing2D.SmoothingMode]::None
    & $draw $g
    $g.Dispose();$result=[Drawing.Bitmap]::new(64,64);$graphics=[Drawing.Graphics]::FromImage($result);$graphics.InterpolationMode=[Drawing.Drawing2D.InterpolationMode]::NearestNeighbor;$graphics.PixelOffsetMode=[Drawing.Drawing2D.PixelOffsetMode]::Half
    $graphics.DrawImage($small,[Drawing.Rectangle]::new(0,0,64,64),0,0,32,32,[Drawing.GraphicsUnit]::Pixel);$graphics.Dispose();$small.Dispose()
    $path=Join-Path $textureRoot $name;[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path))|Out-Null;$result.Save($path,[Drawing.Imaging.ImageFormat]::Png);$result.Dispose()
}
foreach($rusty in @($false,$true)){
    $id=if($rusty){'rusted_iron_spear'}else{'iron_spear'}
    Sprite "item/$id.png" {
        param($g)
        Line $g '#261C17' 5 27 24 8 4;Line $g '#8E5C36' 5 27 24 8 2;Line $g '#C28B50' 6 26 22 10
        foreach($v in @(10,13,16)){Line $g '#3A2B24' $v (32-$v) ($v+2) (30-$v) 2}
        Poly $g '#252E31' @(@(19,10),@(25,3),@(29,2),@(28,7),@(23,13))
        $metal=if($rusty){'#9F542F'}else{'#A5BEC3'};$light=if($rusty){'#DB9551'}else{'#E2F0E8'}
        Poly $g $metal @(@(21,10),@(26,4),@(28,3),@(26,8),@(23,11));Line $g $light 23 9 27 4
        Rect $g '#6B5A43' 20 11 4 2;Rect $g '#C6AD75' 21 11 2 1
        if($rusty){Rect $g '#663722' 24 6 2 2;Rect $g '#D7893C' 22 9 1 1}
    }
}
Sprite 'item/trap_reset_tool.png' {
    param($g)
    Line $g '#201E1A' 7 26 19 12 5;Line $g '#A05D35' 7 25 18 13 3;Line $g '#D7994F' 8 24 17 14
    Poly $g '#303B3C' @(@(14,12),@(14,7),@(20,3),@(24,3),@(21,7),@(22,10),@(26,7),@(27,11),@(23,16),@(18,16))
    Poly $g '#9BAEAB' @(@(16,11),@(16,8),@(20,5),@(18,9),@(22,13),@(25,10),@(22,14),@(18,14));Line $g '#DBE5CF' 17 8 20 5
    Rect $g '#4E3C2F' 11 19 3 2
}
foreach($vaccine in @($false,$true)){
    $id=if($vaccine){'rabies_vaccine'}else{'rabies_cure'}
    Sprite "item/$id.png" {
        param($g)
        Poly $g '#25242D' @(@(11,4),@(20,4),@(20,9),@(24,12),@(24,26),@(21,29),@(10,29),@(7,26),@(7,12),@(11,9))
        Rect $g '#B1B9C2' 9 13 13 12;Rect $g '#D4DFDA' 10 11 11 2
        $liquid=if($vaccine){'#B75A8B'}else{'#70579B'};Rect $g $liquid 10 16 11 9
        Rect $g '#E3E9DA' 9 14 2 8;Rect $g '#7786A6' 21 14 1 11;Rect $g '#F0EAD0' 10 26 10 1
        Rect $g '#664732' 11 4 9 4;Rect $g '#B4834C' 12 4 7 2;Rect $g '#DBB879' 13 4 5 1
        if($vaccine){Poly $g '#E9D6AE' @(@(12,17),@(19,17),@(19,21),@(16,25),@(12,21));Rect $g '#5F4573' 15 18 2 4}
        else{Rect $g '#F6E3CB' 14 17 3 7;Rect $g '#F6E3CB' 12 19 7 3}
        Rect $g '#ECF5E3' 11 12 2 1
    }
}
foreach($cooked in @($false,$true)){
    $id=if($cooked){'cooked_rabid_meat'}else{'rabid_meat'}
    Sprite "item/$id.png" {
        param($g)
        Poly $g '#30202A' @(@(3,18),@(6,11),@(12,6),@(21,5),@(28,11),@(27,21),@(21,27),@(10,28),@(4,24))
        $meat=if($cooked){'#925B36'}else{'#8E426B'};$shade=if($cooked){'#5E3C28'}else{'#572949'};$light=if($cooked){'#C38B50'}else{'#C97D8F'}
        Poly $g $meat @(@(5,18),@(8,12),@(14,8),@(21,7),@(25,12),@(25,20),@(20,25),@(11,26),@(6,23))
        Line $g $light 8 14 15 10 2;Line $g $shade 9 22 19 20 2;Line $g $light 15 12 21 10
        Poly $g '#E0C7AC' @(@(15,16),@(20,14),@(23,16),@(21,19),@(17,20),@(14,18));Rect $g '#886965' 17 17 3 1
        Rect $g $shade 9 18 2 2;Rect $g $shade 20 23 2 1;Rect $g $light 7 20 2 1
    }
}
Sprite 'mob_effect/rabies.png' {
    param($g)
    Poly $g '#3B2036' @(@(5,27),@(4,7),@(10,3),@(15,9),@(21,3),@(27,7),@(26,27),@(16,29))
    Poly $g '#AA476B' @(@(7,24),@(6,9),@(10,6),@(15,12),@(21,6),@(25,9),@(24,25),@(16,27))
    Rect $g '#E9BD9E' 8 14 5 2;Rect $g '#E9BD9E' 19 14 5 2;Rect $g '#351A32' 10 15 2 2;Rect $g '#351A32' 20 15 2 2
    Poly $g '#E7D9C8' @(@(11,20),@(16,18),@(21,20),@(20,25),@(12,25));Rect $g '#3E2337' 14 20 4 2
    Rect $g '#F4E9D4' 22 23 3 3;Rect $g '#F4E9D4' 25 20 2 2
}
Sprite 'mob_effect/rabies_resistance.png' {
    param($g)
    Poly $g '#3A2638' @(@(5,4),@(26,4),@(26,19),@(22,25),@(16,29),@(9,25),@(5,19))
    Poly $g '#B5769B' @(@(7,6),@(24,6),@(24,18),@(20,24),@(16,27),@(11,23),@(7,18));Line $g '#F1D5C5' 8 7 22 7 2
    Rect $g '#F3E1C9' 14 10 4 12;Rect $g '#F3E1C9' 10 14 12 4
}
Sprite 'mob_effect/disoriented.png' {
    param($g)
    Poly $g '#33291F' @(@(4,11),@(10,5),@(22,4),@(28,10),@(28,22),@(22,28),@(9,28),@(3,21))
    Line $g '#D5A15C' 8 13 12 9 3;Line $g '#D5A15C' 12 9 23 10 3;Line $g '#D5A15C' 23 10 24 20 3;Line $g '#D5A15C' 24 20 13 23 3;Line $g '#D5A15C' 13 23 11 16 3;Line $g '#D5A15C' 11 16 18 14 3
    Rect $g '#F4D998' 4 5 3 1;Rect $g '#F4D998' 5 4 1 3;Rect $g '#F4D998' 26 26 3 1
}
# Exact vanilla villager UVs: jacket front (6,44,8,20), arms (44,22), body (16,20).
# Keep the eyes/nose untouched; the old overlay painted the wrong regions.
$coat=[Drawing.Bitmap]::new(64,64);$g=[Drawing.Graphics]::FromImage($coat)
$ivory='#E5EADF';$shade='#B9CAC2';$edge='#8FA79B';$green='#3B655D';$deep='#294A45'
# Dark green scrubs and sturdy boots, underneath a long coat.
Rect $g $green 16 20 28 18;Rect $g $deep 22 26 8 2
Rect $g $green 0 22 16 16;Rect $g '#343D39' 0 34 16 4;Rect $g '#667166' 4 34 4 1
# Jacket sides, front and back. Each face has seam and hem shading.
foreach($face in @(@(0,6),@(6,8),@(14,6),@(20,8))){$x=$face[0];$w=$face[1];Rect $g $ivory $x 44 $w 20;Rect $g $shade $x 44 1 20;Rect $g $edge ($x+$w-1) 44 1 20;Rect $g $shade $x 62 $w 2}
Rect $g $ivory 6 38 16 6
# Open collar and lapels; subtle brass buttons and two patch pockets.
Rect $g $green 9 44 2 5;Rect $g $shade 8 44 1 2;Rect $g $shade 11 44 1 2
Rect $g '#F4F3E5' 7 46 1 7;Rect $g '#F4F3E5' 12 46 1 7
Rect $g $shade 10 49 1 13;foreach($y in @(50,53,56)){Rect $g '#A28C61' 10 $y 1 1}
foreach($x in @(7,11)){Rect $g $shade $x 55 2 4;Rect $g '#D6DED2' $x 56 2 2}
Rect $g '#A24B43' 7 48 1 3;Rect $g '#A24B43' 6 49 3 1
Rect $g $shade 24 49 1 13;Rect $g $green 21 46 6 1
# White sleeves with green cuffs and folded gloved hands.
Rect $g $ivory 44 22 16 12;Rect $g $shade 44 26 1 8;Rect $g $shade 55 26 1 8
Rect $g $green 44 32 16 2;Rect $g $ivory 40 38 24 8
Rect $g $green 44 42 8 4;Rect $g '#6E9986' 45 42 6 1;Rect $g $deep 48 43 1 3
# Small paper cap at the TOP of the head, leaving both eyes fully visible.
Rect $g $ivory 8 0 8 8;Rect $g $ivory 0 8 32 3;Rect $g $shade 0 10 32 1
Rect $g $green 11 8 1 3;Rect $g $green 10 9 3 1
$g.Dispose();$dir=Join-Path $textureRoot 'entity/villager/profession';[IO.Directory]::CreateDirectory($dir)|Out-Null;$coat.Save((Join-Path $dir 'medic.png'),[Drawing.Imaging.ImageFormat]::Png);$coat.Dispose()
foreach($name in @('rusted_iron_block','medic_cabinet')){
    $bitmap=[Drawing.Bitmap]::new(16,16)
    for($y=0;$y-lt16;$y++){for($x=0;$x-lt16;$x++){
        $noise=($x*17+$y*31+$x*$y*7)%101
        if($name-eq'rusted_iron_block'){
            $hex=if($noise-lt36){'#9B5B35'}elseif($noise-lt49){'#CE8240'}elseif($noise-lt58){'#633B29'}else{'#A7AAA0'}
            if($x-eq0-or$y-eq0-or$x-eq15-or$y-eq15){$hex='#5D635E'}
            if(($x-in@(2,13))-and($y-in@(2,13))){$hex='#D5C795'}
        }else{
            $hex=if($y%4-eq0){'#A7AB99'}else{'#E5DFC5'}
            if($x-eq0-or$x-eq15){$hex='#7A6850'}
            if(($x-ge6-and$x-le9-and$y-ge3-and$y-le12)-or($y-ge6-and$y-le9-and$x-ge3-and$x-le12)){$hex='#A83E35'}
        }
        $bitmap.SetPixel($x,$y,(Color $hex))
    }}
    $dir=Join-Path $textureRoot 'block';[IO.Directory]::CreateDirectory($dir)|Out-Null;$bitmap.Save((Join-Path $dir ($name+'.png')),[Drawing.Imaging.ImageFormat]::Png);$bitmap.Dispose()
}
foreach($half in @($false,$true)){
    $heart=[Drawing.Bitmap]::new(9,9);$mask=@('000000000','011001100','111111110','111111110','011111100','001111000','000110000','000000000','000000000')
    for($y=0;$y-lt9;$y++){for($x=0;$x-lt9;$x++){if($mask[$y][$x]-eq'1'-and(!$half-or$x-lt4)){$hex=if($x+$y-lt5){'#E28DA6'}elseif($y-gt4){'#692F65'}else{'#B24579'};$heart.SetPixel($x,$y,(Color $hex))}}}
    $name=if($half){'rabies_heart_half.png'}else{'rabies_heart.png'};$dir=Join-Path $textureRoot 'gui';[IO.Directory]::CreateDirectory($dir)|Out-Null;$heart.Save((Join-Path $dir $name),[Drawing.Imaging.ImageFormat]::Png);$heart.Dispose()
}
Write-Output 'Drew expansion sprites, medical clothing, block textures, effects and rabies hearts.'
# Stitched leather veterinary case with brass corners, bottle slots and a paw badge.
Sprite 'item/veterinary_kit.png' {
    param($g)
    Rect $g '#211C19' 4 11 24 16;Rect $g '#70472E' 5 12 22 14
    Rect $g '#A97647' 6 12 20 3;Rect $g '#492E23' 6 24 20 2
    Rect $g '#292520' 10 6 12 6;Rect $g '#B79557' 11 7 10 2
    Rect $g '#70472E' 12 9 8 3;Rect $g '#BC9B62' 5 12 3 3;Rect $g '#BC9B62' 24 12 3 3
    Rect $g '#BC9B62' 5 23 3 3;Rect $g '#BC9B62' 24 23 3 3
    foreach($x in @(9,12,15,18,21)){Rect $g '#C39B6A' $x 14 1 1;Rect $g '#AE8759' $x 24 1 1}
    Rect $g '#304D41' 8 16 16 7;Rect $g '#658168' 9 16 14 1
    Rect $g '#E5D7AE' 14 20 5 2;Rect $g '#E5D7AE' 15 19 3 1
    foreach($x in @(12,15,18,21)){Rect $g '#E5D7AE' $x 17 2 2}
    Rect $g '#D5B36E' 15 12 3 3;Rect $g '#503829' 16 13 1 1
}