package de.pixelabenteuer.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

/** Bounded static-world navigation shared by all hostile mobs. */
public final class MobNavigation {
 public interface Terrain { boolean blocked(float x,float y); }
 private final int cell=24,cols,rows,total;
 private final byte[] terrain;
 private final float[] cost;
 private final int[] parent;
 private final boolean[] closed;
 public int lastExpanded=0,terrainQueries=0;
 public static final int MAX_EXPANSIONS=240;
 public MobNavigation(int width,int height){cols=(width+cell-1)/cell;rows=(height+cell-1)/cell;total=cols*rows;terrain=new byte[total];cost=new float[total];parent=new int[total];closed=new boolean[total];}
 private boolean blocked(int x,int y,Terrain world){
  if(x<0||y<0||x>=cols||y>=rows)return true;
  int n=y*cols+x;
  if(terrain[n]==0){terrain[n]=(byte)(world.blocked(x*cell+cell/2f,y*cell+cell/2f)?2:1);terrainQueries++;}
  return terrain[n]==2;
 }
 public ArrayList<float[]> find(float x,float y,float tx,float ty,Terrain world){
  int sx=Math.max(0,Math.min(cols-1,(int)(x/cell))),sy=Math.max(0,Math.min(rows-1,(int)(y/cell)));
  int gx=Math.max(0,Math.min(cols-1,(int)(tx/cell))),gy=Math.max(0,Math.min(rows-1,(int)(ty/cell)));
  int start=sy*cols+sx,goal=gy*cols+gx,reached=start;double best=Math.hypot(sx-gx,sy-gy);
  Arrays.fill(cost,Float.POSITIVE_INFINITY);Arrays.fill(parent,-1);Arrays.fill(closed,false);
  PriorityQueue<int[]> open=new PriorityQueue<>((a,b)->Integer.compare(a[1],b[1]));cost[start]=0;open.add(new int[]{start,0});
  int[] ox={1,-1,0,0,1,1,-1,-1},oy={0,0,1,-1,1,-1,1,-1};lastExpanded=0;
  while(!open.isEmpty()&&lastExpanded<MAX_EXPANSIONS){
   int n=open.poll()[0];if(closed[n])continue;closed[n]=true;lastExpanded++;
   int cx=n%cols,cy=n/cols;double distance=Math.hypot(cx-gx,cy-gy);
   if(distance<best){best=distance;reached=n;}if(n==goal){reached=n;break;}
   for(int d=0;d<8;d++){
    int nx=cx+ox[d],ny=cy+oy[d];if(blocked(nx,ny,world))continue;
    if(d>=4&&(blocked(nx,cy,world)||blocked(cx,ny,world)))continue;
    int next=ny*cols+nx;float ng=cost[n]+(d>=4?1.4142f:1f);
    if(ng>=cost[next])continue;cost[next]=ng;parent[next]=n;
    open.add(new int[]{next,(int)(ng*100+100*Math.hypot(nx-gx,ny-gy))});
   }
  }
  ArrayList<float[]> route=new ArrayList<>();int n=reached;
  while(n!=start&&n>=0&&route.size()<100){route.add(0,new float[]{n%cols*cell+cell/2f,n/cols*cell+cell/2f});n=parent[n];}
  return route;
 }
}
