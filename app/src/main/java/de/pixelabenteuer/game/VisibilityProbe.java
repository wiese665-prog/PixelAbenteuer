package de.pixelabenteuer.game;
/** Bounded alpha probes distinguish real occlusion from transparent sprite padding. */
final class VisibilityProbe {
 interface Alpha {int at(int x,int y);}
 static boolean masked(Alpha object,int bitmapW,int bitmapH,Alpha player,int sx,int sy,int sw,int sh,float left,float top,float width,float height,float x,float footY,float projection){
  if(width<=0||height<=0||left>x+32||left+width<x-32||top>footY||top+height<footY-64)return false;
  for(int iy=48;iy<=92;iy+=11)for(int ix=56;ix<=72;ix+=8){
   if(player.at(ix,iy)<128)continue;
   float xx=x+ix-64,yy=footY+(iy-96)/projection;
   float u=(xx-left)/width,v=(yy-top)/height;if(u<0||u>=1||v<0||v>=1)continue;
   int tx=sx+Math.min(sw-1,(int)(u*sw)),ty=sy+Math.min(sh-1,(int)(v*sh));
   if(tx>=0&&ty>=0&&tx<bitmapW&&ty<bitmapH&&object.at(tx,ty)>=160)return true;
  }return false;
 }
}
