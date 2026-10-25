param([string]$FfmpegPath)
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if(!$FfmpegPath){
    $bundled=Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools/ffmpeg') -Filter 'ffmpeg.exe' -Recurse -ErrorAction SilentlyContinue|Select-Object -First 1
    if($bundled){$FfmpegPath=$bundled.FullName}else{$command=Get-Command ffmpeg -ErrorAction SilentlyContinue;if($command){$FfmpegPath=$command.Source}}
}
if(!$FfmpegPath){throw 'FFmpeg is needed to regenerate medical audio. Pass -FfmpegPath or install it on PATH.'}
if(-not ('RustRuinMedicalAudio' -as [type])){
Add-Type -TypeDefinition @"
using System;
using System.IO;
using System.Text;
public static class RustRuinMedicalAudio {
    private static double Glass(double time,double start,double gain) {
        double d=time-start;if(d<0||d>0.32)return 0;
        return gain*Math.Exp(-22*d)*(Math.Sin(2*Math.PI*2630*d)+0.52*Math.Sin(2*Math.PI*4130*d)+0.19*Math.Sin(2*Math.PI*6320*d));
    }
    public static void Write(string path) {
        const int rate=44100,count=44100;
        double[] samples=new double[count];double peak=0,low=0;uint random=0x52A6EFu;
        for(int i=0;i<count;i++){
            double time=(double)i/rate;random=unchecked(random*1664525u+1013904223u);
            double noise=((random>>8)/16777215.0)*2-1;low=low*0.85+noise*0.15;
            double value=Glass(time,0.045,0.42)+Glass(time,0.235,0.27);
            double cloth=(time-0.43)/0.28;
            if(cloth>=0&&cloth<=1)value+=(noise-low)*0.028*Math.Sin(Math.PI*cloth)*Math.Sin(Math.PI*cloth);
            double click=time-0.77;
            if(click>=0&&click<0.08)value+=0.14*Math.Exp(-90*click)*(noise*0.65+Math.Sin(2*Math.PI*1770*click)*0.35);
            double fade=Math.Min(1,Math.Min(time/0.012,(1-time)/0.012));value*=Math.Max(0,fade);
            samples[i]=value;peak=Math.Max(peak,Math.Abs(value));
        }
        using(var writer=new BinaryWriter(File.Create(path))){
            writer.Write(Encoding.ASCII.GetBytes("RIFF"));writer.Write(36+count*2);writer.Write(Encoding.ASCII.GetBytes("WAVEfmt "));
            writer.Write(16);writer.Write((ushort)1);writer.Write((ushort)1);writer.Write(rate);writer.Write(rate*2);writer.Write((ushort)2);writer.Write((ushort)16);
            writer.Write(Encoding.ASCII.GetBytes("data"));writer.Write(count*2);
            for(int i=0;i<count;i++)writer.Write((short)Math.Round(Math.Max(-1,Math.Min(1,samples[i]/peak*0.68))*32767));
        }
    }
}
"@
}
$sourceDirectory=Join-Path $projectRoot 'assets-source/audio'
$targetDirectory=Join-Path $projectRoot 'src/main/resources/assets/tetanusmod/sounds'
[IO.Directory]::CreateDirectory($sourceDirectory)|Out-Null
[IO.Directory]::CreateDirectory($targetDirectory)|Out-Null
$wave=Join-Path $sourceDirectory 'medic_work.wav'
$ogg=Join-Path $targetDirectory 'medic_work.ogg'
[RustRuinMedicalAudio]::Write($wave)
& $FfmpegPath -hide_banner -loglevel error -y -fflags +bitexact -i $wave -map_metadata -1 -ac 1 -ar 44100 -c:a libvorbis -q:a 5 -flags:a +bitexact -fflags +bitexact $ogg
if($LASTEXITCODE-ne0){throw 'FFmpeg failed to encode the original medic work sound'}
Write-Output 'Generated original mono medic work audio: two vial clinks, soft cloth movement and a small instrument click.'
