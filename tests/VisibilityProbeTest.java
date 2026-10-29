package de.pixelabenteuer.game;
public class VisibilityProbeTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  VisibilityProbe.Alpha body=(x,y)->x>=56&&x<=72&&y>=48&&y<=92?255:0;
  VisibilityProbe.Alpha solid=(x,y)->255,empty=(x,y)->0;
  check(VisibilityProbe.masked(solid,16,16,body,0,0,16,16,90,450,20,40,100,500,1),"opaque board missed");
  check(!VisibilityProbe.masked(empty,16,16,body,0,0,16,16,90,450,20,40,100,500,1),"transparent padding creates silhouette");
  check(!VisibilityProbe.masked(solid,16,16,empty,0,0,16,16,90,450,20,40,100,500,1),"empty hero sampled");
  check(!VisibilityProbe.masked(solid,16,16,body,0,0,16,16,400,450,20,40,100,500,1),"distant object creates silhouette");
  check(VisibilityProbe.masked(solid,16,16,body,0,0,16,16,90,439,20,40,100,500,.82f),"projected tree missed");
  check(!VisibilityProbe.masked((x,y)->120,16,16,body,0,0,16,16,90,450,20,40,100,500,1),"translucent edge treated as solid");
  System.out.println("Visibility: opaque occlusion, transparent padding, empty body, distance and projected coordinates passed.");
 }
}
