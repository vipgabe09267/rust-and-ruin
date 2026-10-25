$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$resourceRoot=Join-Path $projectRoot 'src/main/resources'
$utf8=[Text.UTF8Encoding]::new($false)
Add-Type -AssemblyName System.Drawing
function Json($relative,$value){$path=Join-Path $resourceRoot $relative;[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path))|Out-Null;[IO.File]::WriteAllText($path,($value|ConvertTo-Json -Depth 60)+"`n",$utf8)}
function Texture($name,$size,[scriptblock]$pixel){
    $bitmap=[Drawing.Bitmap]::new($size,$size)
    try{for($y=0;$y-lt$size;$y++){for($x=0;$x-lt$size;$x++){$bitmap.SetPixel($x,$y,(& $pixel $x $y))}};$bitmap.Save((Join-Path $resourceRoot "assets/tetanusmod/textures/$name.png"),[Drawing.Imaging.ImageFormat]::Png)}finally{$bitmap.Dispose()}
}
# Cool brushed steel has controlled horizontal grain, rolled edges and broad highlights.
Texture 'block/cabinet_steel' 32 {param($x,$y)
    if($x-eq0-or$y-eq0-or$x-eq31-or$y-eq31){return [Drawing.Color]::FromArgb(255,114,128,135)}
    $grain=(($x*11+$y*3)%5)-2;$shine=[int][Math]::Round(13*[Math]::Cos(($x-11)*[Math]::PI/32));$v=181+$shine+$grain
    if($y%8-eq0){$v-=4};return [Drawing.Color]::FromArgb(255,$v,($v+8),($v+12))
}
Texture 'block/cabinet_recess' 16 {param($x,$y)$v=65+(($x*3+$y)%4);return [Drawing.Color]::FromArgb(255,$v,($v+13),($v+19))}
Texture 'block/cabinet_cap' 16 {param($x,$y)$v=91+(($x%3)*13);if($y-eq0-or$y-eq15){$v+=30};return [Drawing.Color]::FromArgb(255,$v,($v+12),($v+17))}
# Cutout glass is deliberately clear: no opaque white cube around the medicines.
Texture 'block/cabinet_glass' 32 {param($x,$y)
    if($x-eq0-or$y-eq0-or$x-eq31-or$y-eq31){return [Drawing.Color]::FromArgb(255,151,197,204)}
    if(($x+$y-ge20-and$x+$y-le21-and$x-ge4-and$x-le17)-or($x+$y-eq42-and$x-ge17-and$x-le25)){return [Drawing.Color]::FromArgb(255,205,233,236)}
    return [Drawing.Color]::Transparent
}
Texture 'block/cabinet_vial_glass' 16 {param($x,$y)
    if($x-eq0-or$y-eq0-or$x-eq15-or$y-eq15){return [Drawing.Color]::FromArgb(255,145,189,194)}
    if(($x-eq2-or$x-eq3)-and$y-ge3-and$y-le11){return [Drawing.Color]::FromArgb(255,223,242,241)}
    return [Drawing.Color]::Transparent
}
foreach($spec in @(@('teal',34,126,137),@('amber',159,93,30),@('plum',113,67,112))){
    $base=$spec
    Texture "block/cabinet_vial_$($spec[0])" 16 {param($x,$y)$light=if($x-ge2-and$x-le4){24}else{0};$shade=[int]($y/3);return [Drawing.Color]::FromArgb(255,([int]$base[1]+$light-$shade),([int]$base[2]+$light-$shade),([int]$base[3]+$light-$shade))}
}
Texture 'block/cabinet_label' 16 {param($x,$y)
    if($x-eq0-or$y-eq0-or$x-eq15-or$y-eq15){return [Drawing.Color]::FromArgb(255,158,177,178)}
    if(($y-ge3-and$y-le5-and$x-ge3-and$x-le12)-or($y-ge8-and$y-le9-and$x-ge3-and$x-le10)){return [Drawing.Color]::FromArgb(255,53,100,109)}
    if($y-eq12-and$x-ge3-and$x-le7){return [Drawing.Color]::FromArgb(255,153,98,60)}
    return [Drawing.Color]::FromArgb(255,223,231,218)
}
Texture 'block/cabinet_badge' 16 {param($x,$y)
    if(($x-ge6-and$x-le9-and$y-ge3-and$y-le12)-or($y-ge6-and$y-le9-and$x-ge3-and$x-le12)){return [Drawing.Color]::FromArgb(255,158,48,43)}
    return [Drawing.Color]::FromArgb(255,227,231,224)
}
function Faces($texture){$faces=@{};foreach($side in @('up','down','north','south','east','west')){$faces[$side]=@{texture=$texture;uv=@(0,0,16,16)}};return $faces}
function Box($bounds,$texture){return @{from=@($bounds[0],$bounds[1],$bounds[2]);to=@($bounds[3],$bounds[4],$bounds[5]);faces=(Faces $texture)}}
function DoorBox($bounds,$texture,$opening){
    $b=$bounds
    if($opening-eq'left'){$b=@((2+$bounds[2]-3),$bounds[1],(3-$bounds[3]+2),(2+$bounds[5]-3),$bounds[4],(3-$bounds[0]+2))}
    elseif($opening-eq'right'){$b=@((14-$bounds[5]+3),$bounds[1],(3+$bounds[0]-14),(14-$bounds[2]+3),$bounds[4],(3+$bounds[3]-14))}
    return Box $b $texture
}
$textures=@{steel='tetanusmod:block/cabinet_steel';recess='tetanusmod:block/cabinet_recess';glass='tetanusmod:block/cabinet_glass';vial='tetanusmod:block/cabinet_vial_glass';teal='tetanusmod:block/cabinet_vial_teal';amber='tetanusmod:block/cabinet_vial_amber';plum='tetanusmod:block/cabinet_vial_plum';label='tetanusmod:block/cabinet_label';cap='tetanusmod:block/cabinet_cap';badge='tetanusmod:block/cabinet_badge';particle='tetanusmod:block/cabinet_steel'}
$variants=@{};$closedHalves=@{}
foreach($half in @('lower','upper')){foreach($opening in @('closed','closed_right','left','right')){
    $isClosed=$opening.StartsWith('closed')
    $suffix=if($opening-eq'closed'){''}elseif($opening-eq'closed_right'){'_right'}elseif($opening-eq'left'){'_open'}else{'_open_right'}
    $name='medic_cabinet_'+$half+$suffix;$elements=@()
    # Folded metal case, recessed back, thin shelves and ventilated kick plate.
    foreach($b in @(@(1.5,0,13.5,14.5,16,14.5),@(1.5,0,3,2.5,16,13.5),@(13.5,0,3,14.5,16,13.5),@(2.5,0,3,13.5,1.5,13.5),@(2.5,8,3.5,13.5,8.5,13.5),@(2.5,15,3,13.5,16,13.5))){$elements+=Box $b '#steel'}
    $elements+=Box @(2.5,1.5,13.4,13.5,15,13.5) '#recess'
    if($half-eq'lower'){foreach($x in @(4,6,8,10)){$elements+=Box @($x,0.25,2.9,($x+1),0.65,3.05) '#recess'}}
    foreach($shelf in @(1.5,8.5)){foreach($slot in @(0,1,2)){
        $x=3.4+$slot*3.2;$y=$shelf;$flavor=@('#teal','#amber','#plum')[$slot]
        # Actual shoulders, neck, cap and label make individual apothecary bottles readable.
        $elements+=Box @($x,($y+0.2),8.6,($x+2.2),($y+3.6),10.8) '#vial'
        $elements+=Box @(($x+0.18),($y+0.3),8.78,($x+2.02),($y+2.6),10.62) $flavor
        $elements+=Box @(($x+0.3),($y+3.6),8.9,($x+1.9),($y+4.0),10.5) '#vial'
        $elements+=Box @(($x+0.65),($y+4.0),9.25,($x+1.55),($y+4.9),10.15) '#vial'
        $elements+=Box @(($x+0.48),($y+4.8),9.08,($x+1.72),($y+5.35),10.32) '#cap'
        $elements+=Box @(($x+0.2),($y+1.1),8.54,($x+2.0),($y+2.35),8.59) '#label'
    }}
    # A real framed glass door opens away from the case, with the correct hinge.
    foreach($b in @(@(2,1,2.2,14,2,3.2),@(2,14.2,2.2,14,15.2,3.2),@(2,2,2.2,3,14.2,3.2),@(13,2,2.2,14,14.2,3.2))){$elements+=DoorBox $b '#steel' $opening}
    $elements+=DoorBox @(3,2,2.65,13,14.2,2.7) '#glass' $opening
    $handleX=if($opening-eq'right'-or$opening-eq'closed_right'){3.2}else{11.8}
    $elements+=DoorBox @($handleX,6.0,1.65,($handleX+0.55),9.0,2.0) '#cap' $opening
    foreach($y in @(6.1,8.4)){$elements+=DoorBox @($handleX,$y,1.9,($handleX+0.55),($y+0.5),2.25) '#steel' $opening}
    if($half-eq'upper'){$elements+=DoorBox @(10.9,14.25,2.12,12.8,15.15,2.2) '#badge' $opening}
    Json "assets/tetanusmod/models/block/$name.json" @{parent='minecraft:block/block';textures=$textures;elements=$elements}
    if($opening-eq'closed'){$closedHalves[$half]=$elements}
    foreach($facing in @('north','east','south','west')){foreach($hinge in @('left','right')){
        if($isClosed){if(($opening-eq'closed'-and$hinge-eq'right')-or($opening-eq'closed_right'-and$hinge-eq'left')){continue}}elseif($opening-ne$hinge){continue}
        foreach($powered in @('false','true')){$rotation=@{north=0;east=90;south=180;west=270}[$facing];$isOpen=(!$isClosed).ToString().ToLowerInvariant();$variants["facing=$facing,half=$half,hinge=$hinge,open=$isOpen,powered=$powered"]=@{model="tetanusmod:block/$name";y=$rotation}}
    }}
}}
foreach($id in @('medic_cabinet','field_medic_cabinet')){
    Json "assets/tetanusmod/blockstates/$id.json" @{variants=$variants}
    Json "data/tetanusmod/loot_tables/blocks/$id.json" @{type='minecraft:block';pools=@(@{rolls=1;entries=@(@{type='minecraft:item';name='tetanusmod:medic_cabinet'});conditions=@(@{condition='minecraft:survives_explosion'},@{condition='minecraft:block_state_property';block="tetanusmod:$id";properties=@{half='lower'}})})}
}
# Inventory preview shows the complete two-block case, not a misleading single half.
$itemElements=@($closedHalves['lower'])
foreach($element in $closedHalves['upper']){$upper=@{from=@($element.from[0],($element.from[1]+16),$element.from[2]);to=@($element.to[0],($element.to[1]+16),$element.to[2]);faces=$element.faces};$itemElements+=$upper}
Json 'assets/tetanusmod/models/item/medic_cabinet.json' @{parent='minecraft:block/block';textures=$textures;elements=$itemElements;display=@{gui=@{rotation=@(20,225,0);translation=@(0,-3,0);scale=@(0.4,0.4,0.4)};ground=@{translation=@(0,1,0);scale=@(0.25,0.25,0.25)};fixed=@{translation=@(0,-8,0);scale=@(0.45,0.45,0.45)};thirdperson_righthand=@{rotation=@(75,45,0);translation=@(0,-2,0);scale=@(0.3,0.3,0.3)}}}
# A 64-pixel image on a 32-unit atlas gives two texels per model unit for stitches.
$glove=[Drawing.Bitmap]::new(64,64)
function Fill($x,$y,$w,$h,$r,$g,$b){for($py=$y*2;$py-lt($y+$h)*2;$py++){for($px=$x*2;$px-lt($x+$w)*2;$px++){$grain=(($px*3+$py*5)%3)-1;$glove.SetPixel($px,$py,[Drawing.Color]::FromArgb(255,($r+$grain),($g+$grain),($b+$grain)))}}}
try{
    Fill 0 0 16 8 158 117 65
    Fill 4 4 4 4 183 143 86;Fill 12 4 4 4 144 103 58
    Fill 0 12 16 5 63 69 64;Fill 0 20 10 6 163 123 72
    $seam=[Drawing.Color]::FromArgb(255,98,71,42);$thread=[Drawing.Color]::FromArgb(255,220,184,118)
    foreach($edge in @(0,8,16,24)){for($y=8;$y-lt16;$y++){$glove.SetPixel($edge,$y,$seam)};for($x=$edge+1;$x-lt$edge+8;$x+=2){$glove.SetPixel($x,8,$thread)}}
    # Four narrow finger panels, a knuckle seam and reinforced palm; no plain brown pot.
    foreach($start in @(8,24)){
        foreach($x in @(($start+2),($start+4),($start+6))){for($y=12;$y-lt16;$y++){$glove.SetPixel($x,$y,$seam)}}
        for($x=$start+1;$x-lt$start+7;$x++){$glove.SetPixel($x,11,$seam)}
    }
    for($x=0;$x-lt32;$x+=2){$glove.SetPixel($x,33,$thread)}
    $glove.Save((Join-Path $resourceRoot 'assets/tetanusmod/textures/entity/gloves.png'),[Drawing.Imaging.ImageFormat]::Png)
}finally{$glove.Dispose()}
Write-Output 'Generated stainless-steel/glass cabinets, shaped labeled vials, hinge-correct doors and tailored glove atlas.'
