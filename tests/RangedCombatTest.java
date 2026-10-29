package de.pixelabenteuer.game;
import java.util.*;
public class RangedCombatTest {
 static class Enemy implements RangedCombat.Target {float x,y;int hp=20,hits;Enemy(float x,float y){this.x=x;this.y=y;}public float shotX(){return x;}public float shotY(){return y;}public float shotRadius(){return 2;}public boolean shotAlive(){return hp>0;}}
 static class World implements RangedCombat.World {List<Enemy> enemies=new ArrayList<>();float wall=Float.MAX_VALUE;public boolean blocked(float x,float y){return x>=wall;}public Iterable<Enemy> targets(){return enemies;}public void hit(RangedCombat.Target t,int damage){Enemy e=(Enemy)t;e.hp-=damage;e.hits++;}}
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void main(String[] args){
  check(RangedCombat.role("Jagdbogen {7}")==1&&RangedCombat.role("Magierstab")==2&&RangedCombat.role("Iron Sword")==0,"equipment class mapping");
  check(RangedCombat.manaCost(1)==0&&RangedCombat.manaCost(2)==6,"mana cost");
  for(int role=1;role<=2;role++)for(int direction=0;direction<4;direction++){
   int dx=direction==0?1:direction==1?-1:0,dy=direction==2?1:direction==3?-1:0;
   RangedCombat c=new RangedCombat();World w=new World();Enemy near=new Enemy(dx*48,dy*48),far=new Enemy(dx*100,dy*100);w.enemies.add(far);w.enemies.add(near);
   check(c.launch(role,0,0,dx,dy,7),"launch");for(int f=0;f<60;f++)c.update(.05f,w);
   check(near.hp==13&&near.hits==1&&far.hp==20,"swept nearest hit, four directions, no double damage");check(c.shots.isEmpty(),"hit removed");
  }
  RangedCombat c=new RangedCombat();World w=new World();w.wall=25;Enemy e=new Enemy(40,0);w.enemies.add(e);c.launch(1,0,0,1,0,7);for(int i=0;i<20;i++)c.update(.05f,w);check(e.hp==20&&c.shots.isEmpty(),"wall occlusion");
  w.wall=Float.MAX_VALUE;w.enemies.clear();for(int i=0;i<40;i++)c.launch(2,0,0,1,0,4);check(c.shots.size()==12,"bounded count");for(int i=0;i<200;i++)c.update(.05f,w);check(c.shots.isEmpty(),"range expiry");
  check(!c.launch(1,0,0,0,0,4)&&!c.launch(1,0,0,Float.NaN,0,4),"invalid vectors");c.launch(1,0,0,1,0,2);c.update(0,w);check(c.shots.get(0).x==0,"pause");c.clear();check(c.shots.isEmpty(),"door/save reset");
  System.out.println("Ranged combat: four directions, swept hits, single damage, walls, range, mana, bounded count and reset passed.");
 }
}
