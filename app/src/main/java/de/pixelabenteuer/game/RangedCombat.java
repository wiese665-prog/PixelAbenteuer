package de.pixelabenteuer.game;

import java.util.ArrayList;
import java.util.List;

/** Bounded, swept projectile simulation shared by archery and magic. */
final class RangedCombat {
 static final int ARCHER=1,MAGE=2,MAX_SHOTS=12;
 interface Target { float shotX();float shotY();float shotRadius();boolean shotAlive(); }
 interface World { boolean blocked(float x,float y);Iterable<? extends Target> targets();void hit(Target target,int damage); }
 static final class Shot {
  final int kind,damage;final float vx,vy;float x,y,remaining;
  Shot(int k,float xx,float yy,float dx,float dy,int d){kind=k;x=xx;y=yy;damage=d;float norm=(float)Math.hypot(dx,dy);float speed=k==ARCHER?340:250;vx=dx/norm*speed;vy=dy/norm*speed;remaining=k==ARCHER?400:330;}
 }
 final ArrayList<Shot> shots=new ArrayList<>();
 static int role(String name){if(name==null)return 0;return name.startsWith("Jagdbogen")?ARCHER:name.startsWith("Magierstab")?MAGE:0;}
 static int manaCost(int role){return role==MAGE?6:0;}
 static float cooldown(int role){return role==MAGE?.75f:role==ARCHER?.65f:.42f;}
 boolean launch(int kind,float x,float y,float dx,float dy,int damage){
  if((kind!=ARCHER&&kind!=MAGE)||shots.size()>=MAX_SHOTS||!Float.isFinite(dx)||!Float.isFinite(dy)||Math.hypot(dx,dy)<.001)return false;
  shots.add(new Shot(kind,x,y,dx,dy,Math.max(1,damage)));return true;
 }
 void clear(){shots.clear();}
 void update(float dt,World world){
  if(dt<=0||!Float.isFinite(dt))return;dt=Math.min(.05f,dt);
  for(int i=shots.size()-1;i>=0;i--){
   Shot s=shots.get(i);float speed=(float)Math.hypot(s.vx,s.vy);float distance=Math.min(s.remaining,speed*dt);
   int steps=Math.max(1,(int)Math.ceil(distance/3));boolean removed=false;
   for(int step=0;step<steps;step++){
    float part=distance/steps;s.x+=s.vx/speed*part;s.y+=s.vy/speed*part;s.remaining-=part;
    if(world.blocked(s.x,s.y)){removed=true;break;}
    Target nearest=null;float best=Float.MAX_VALUE;
    for(Target t:world.targets())if(t.shotAlive()){
     float dx=s.x-t.shotX(),dy=s.y-t.shotY(),d=dx*dx+dy*dy,r=t.shotRadius()+(s.kind==MAGE?4:2);
     if(d<=r*r&&d<best){best=d;nearest=t;}
    }
    if(nearest!=null){world.hit(nearest,s.damage);removed=true;break;}
   }
   if(removed||s.remaining<=.01f)shots.remove(i);
  }
 }
}
