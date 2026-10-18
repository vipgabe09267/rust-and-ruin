using System;
using System.Collections.Generic;
using System.Drawing;
public static class SpriteIsolator {
    public static Bitmap Extract(Bitmap source, Rectangle area) {
        int w=area.Width,h=area.Height;
        bool[] seen=new bool[w*h];
        List<int> best=new List<int>();
        for(int sy=0;sy<h;sy++)for(int sx=0;sx<w;sx++) {
            int start=sy*w+sx;
            if(seen[start]||source.GetPixel(area.X+sx,area.Y+sy).A<32)continue;
            var component=new List<int>();var queue=new Queue<int>();queue.Enqueue(start);seen[start]=true;
            while(queue.Count>0) {
                int index=queue.Dequeue();component.Add(index);int x=index%w,y=index/w;
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    int nx=x+dx,ny=y+dy;if(nx<0||ny<0||nx>=w||ny>=h)continue;
                    int next=ny*w+nx;if(seen[next])continue;
                    seen[next]=true;
                    if(source.GetPixel(area.X+nx,area.Y+ny).A>=32)queue.Enqueue(next);
                }
            }
            if(component.Count>best.Count)best=component;
        }
        if(best.Count==0)throw new Exception("No sprite in "+area);
        int minX=w,minY=h,maxX=0,maxY=0;
        foreach(int index in best){int x=index%w,y=index/w;minX=Math.Min(minX,x);minY=Math.Min(minY,y);maxX=Math.Max(maxX,x);maxY=Math.Max(maxY,y);}
        var result=new Bitmap(maxX-minX+1,maxY-minY+1);
        foreach(int index in best){int x=index%w,y=index/w;result.SetPixel(x-minX,y-minY,source.GetPixel(area.X+x,area.Y+y));}
        return result;
    }
}