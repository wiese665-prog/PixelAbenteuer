package de.pixelabenteuer.game;
public class AmbientMotionTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  int restingTrees=0,restingBirds=0,differentTrees=0,differentBirds=0;
  for(int i=0;i<12000;i++){
   float t=i*.05f;
   int a=AmbientMotion.frame(t,110,310,13,6,12,16),b=AmbientMotion.frame(t,104,630,13,6,12,16);
   int c=AmbientMotion.frame(t,1220,550,20,7,10,15),d=AmbientMotion.frame(t,1266,568,20,7,10,15);
   check(a>=0&&a<13&&c>=0&&c<20,"frame outside sheet");
   if(a==0)restingTrees++;if(c==0)restingBirds++;
   if(a!=b)differentTrees++;if(c!=d)differentBirds++;
   check(a==AmbientMotion.frame(t,110,310,13,6,12,16),"non-deterministic phase");
  }
  check(restingTrees>9000,"trees animate too often");check(restingBirds>8500,"birds animate too often");
  check(differentTrees>500&&differentBirds>500,"ambient objects synchronized");
  System.out.println("Ambient animation: sheet bounds, deterministic offsets, long idle periods and asynchronous trees/birds passed.");
 }
}
