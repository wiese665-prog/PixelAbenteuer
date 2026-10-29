package de.pixelabenteuer.game;
import java.util.ArrayList;
public class MobNavigationTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  MobNavigation nav=new MobNavigation(1600,1000);
  MobNavigation.Terrain wall=(x,y)->x>700&&x<748&&y<700;
  ArrayList<float[]> route=nav.find(620,420,820,420,wall);
  check(nav.lastExpanded<=240,"unbounded search");check(!route.isEmpty(),"no progress around wall");
  for(float[] p:route)check(!wall.blocked(p[0],p[1]),"route through wall");
  int initial=nav.terrainQueries;for(int i=0;i<200;i++)nav.find(620,420,820,420,wall);
  check(nav.terrainQueries==initial,"static terrain repeatedly queried");
  MobNavigation sealed=new MobNavigation(1600,1000);
  for(int i=0;i<100;i++){
   sealed.find(100,100,1500,900,(x,y)->x>120&&x<145);
   check(sealed.lastExpanded<=MobNavigation.MAX_EXPANSIONS,"node budget exceeded");
  }
  check(sealed.terrainQueries<=67*42,"terrain cache not bounded by map");
  MobNavigation corner=new MobNavigation(96,96);
  ArrayList<float[]> diagonal=corner.find(12,12,36,36,(x,y)->(x>24&&y<24)||(x<24&&y>24));
  check(diagonal.isEmpty(),"cuts a blocked diagonal corner");
  System.out.println("Mob navigation: bounded expansions, cached terrain, blocked-corner safety and repeated blocked searches passed.");
 }
}
