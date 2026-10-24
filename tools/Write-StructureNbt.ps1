param([Parameter(Mandatory=$true)][hashtable]$Structure,[Parameter(Mandatory=$true)][string]$Path)
$ErrorActionPreference='Stop'
function NbtType($value){if($value-is[byte]-or$value-is[bool]){return [byte]1};if($value-is[int]){return [byte]3};if($value-is[long]){return [byte]4};if($value-is[single]){return [byte]5};if($value-is[double]){return [byte]6};if($value-is[string]){return [byte]8};if($value-is[Collections.IDictionary]){return [byte]10};if($value-is[array]){return [byte]9};throw "Unsupported NBT type: $($value.GetType())"}
function Big($bytes){[Array]::Reverse($bytes);$writer.Write([byte[]]$bytes)}
function String($text){$bytes=[Text.Encoding]::UTF8.GetBytes($text);Big ([BitConverter]::GetBytes([uint16]$bytes.Length));$writer.Write($bytes)}
function Payload($value,$type){
    switch($type){
        1 {$writer.Write([byte]$value)}
        3 {Big ([BitConverter]::GetBytes([int]$value))}
        4 {Big ([BitConverter]::GetBytes([long]$value))}
        5 {Big ([BitConverter]::GetBytes([single]$value))}
        6 {Big ([BitConverter]::GetBytes([double]$value))}
        8 {String $value}
        9 {$elementType=[byte]10;if($value.Length){$elementType=NbtType $value[0]};$writer.Write($elementType);Big ([BitConverter]::GetBytes([int]$value.Length));foreach($element in $value){Payload $element $elementType}}
        10 {foreach($key in ($value.Keys|Sort-Object)){$tag=NbtType $value[$key];$writer.Write([byte]$tag);String $key;Payload $value[$key] $tag};$writer.Write([byte]0)}
    }
}
[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($Path))|Out-Null
$file=[IO.File]::Create($Path)
$gzip=[IO.Compression.GZipStream]::new($file,[IO.Compression.CompressionMode]::Compress)
$writer=[IO.BinaryWriter]::new($gzip)
try{$writer.Write([byte]10);String '';Payload $Structure ([byte]10)}finally{$writer.Dispose();$gzip.Dispose();$file.Dispose()}
