package de.pixelabenteuer.game;
/** The town, eastern tower and all public entrances share one safety boundary. */
final class VillageSafety {
 static final int TOWER_X=1110,TOWER_Y=350;
 private static boolean box(float x,float y,float p,float l,float t,float r,float b){return x>l-p&&x<r+p&&y>t-p&&y<b+p;}
 static boolean area(float x,float y,float p){return box(x,y,p,100,96,1020,890)||box(x,y,p,940,140,1230,590);}
 static boolean protectedArea(float x,float y){return area(x,y,24);}
 static boolean wildlifeExcluded(float x,float y){return area(x,y,80);}
 static boolean treeClearance(float x,float y){return Math.abs(x-TOWER_X)<120&&Math.abs(y-TOWER_Y)<230;}
}
