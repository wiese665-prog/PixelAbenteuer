package de.pixelabenteuer.game;
import java.util.*;import java.io.*;
public final class VillageResidentsTest {
 static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
 static final class Grid {
  int x,y,step,w,h;byte[] data;
  boolean at(float px,float py,boolean outside){int xx=Math.round((px-x)/step),yy=Math.round((py-y)/step);return xx<0||yy<0||xx>=w||yy>=h?outside:data[yy*w+xx]!=0;}
 }
 static final class World implements VillageResidents.World {
  HashMap<Integer,Grid> masks=new HashMap<>();int room=99;float px=1500,py=500;
  World(String path)throws Exception{try(DataInputStream in=new DataInputStream(new FileInputStream(path))){int count=in.readInt();for(int i=0;i<count;i++){int id=in.readInt();Grid g=new Grid();g.x=in.readInt();g.y=in.readInt();g.step=in.readInt();g.w=in.readInt();g.h=in.readInt();g.data=new byte[g.w*g.h];in.readFully(g.data);masks.put(id,g);}}}
  public boolean blocked(int f,float x,float y){return !masks.containsKey(f)||masks.get(f).at(x,y,true);}
  public boolean road(float x,float y){return masks.get(-100).at(x,y,false);}
  public int playerFloor(){return room;}public float playerX(){return px;}public float playerY(){return py;}
 }
 public static void main(String[] args)throws Exception {
  World world=new World(args[0]);VillageResidents system=new VillageResidents();
  require(system.people.size()==17,"Expected 17 unique inhabitants");HashSet<String> ids=new HashSet<>(),names=new HashSet<>(),art=new HashSet<>();int mobile=0;
  for(VillageResidents.Resident n:system.people){require(ids.add(n.id),"Duplicate ID");require(names.add(n.name),"Duplicate name");require(art.add(n.legacy?"legacy:"+n.id:n.kind!=null?n.kind:n.idle),"Duplicate appearance "+n.name);require(n.lines.length>=3&&!n.title.isEmpty(),"Missing dialogue/title "+n.name);if(n.mobile)mobile++;}
  require(mobile==4,"All four walking sprite families must roam");
  require(system.get("borin").title.equals("Schmied")&&system.get("maela").title.equals("Händlerin")&&system.get("grom").title.equals("Händler")&&system.get("tarek").title.equals("Abenteurer"),"Legacy titles");
  HashMap<String,float[]> fixed=new HashMap<>();for(VillageResidents.Resident n:system.people)if(!n.mobile)fixed.put(n.id,new float[]{n.floor,n.x,n.y});
  for(int tick=0;tick<36000;tick++){
   HashMap<String,float[]> previous=new HashMap<>();for(VillageResidents.Resident n:system.people)previous.put(n.id,new float[]{n.floor,n.x,n.y});
   system.update(.1f,world);int outdoors=0;
   for(VillageResidents.Resident n:system.people){
    if(n.mobile&&n.floor==-1)outdoors++;
    if(!n.mobile){float[] p=fixed.get(n.id);require(n.floor==(int)p[0]&&n.x==p[1]&&n.y==p[2],"Fixed actor moved: "+n.name);}
    else{require(!world.blocked(n.floor,n.x,n.y),"Actor entered terrain: "+n.name+" room="+n.floor+" @ "+n.x+","+n.y);float[] p=previous.get(n.id);if(n.floor==(int)p[0])require(Math.hypot(n.x-p[1],n.y-p[2])<4.6,"Actor jumped: "+n.name);}
   }
   require(outdoors>=2,"All residents vanished indoors");
   for(int i=0;i<system.people.size();i++)for(int j=i+1;j<system.people.size();j++){
    VillageResidents.Resident a=system.people.get(i),b=system.people.get(j);if(a.floor==b.floor&&(a.mobile||b.mobile))require(Math.hypot(a.x-b.x,a.y-b.y)>=a.radius+b.radius+.8,"People clipped: "+a.name+" / "+b.name);
   }
  }
  for(VillageResidents.Resident n:system.people)if(n.mobile)System.out.println(n.debug()+" visits="+n.completedVisits+" mask="+n.visitedRooms);
  for(VillageResidents.Resident n:system.people)if(n.mobile){require(n.completedVisits>=n.visits.length,"No complete house cycle: "+n.name);for(VillageResidents.Visit v:n.visits)require((n.visitedRooms&(1<<v.room))!=0,"Missed assigned room "+v.room+": "+n.name);}
  VillageResidents.Resident lina=system.get("lina");int size=system.people.size();int f=lina.floor;float x=lina.x,y=lina.y;system.restore(lina.id,f,x,y,lina.visitIndex,world);require(size==system.people.size()&&system.get("lina")==lina,"Restore duplicated actor");
  for(VillageResidents.Resident n:system.people){n.speak();require(n.speechTime>0&&!n.speech.isEmpty(),"Missing speech "+n.name);String first=n.speech;n.speak();require(!first.equals(n.speech),"No phrase variation "+n.name);}
  system.reset();world.room=-1;world.px=550;world.py=470;
  for(int tick=0;tick<1800;tick++){system.update(.1f,world);for(VillageResidents.Resident n:system.people)if(n.mobile&&n.floor==-1)require(Math.hypot(n.x-world.px,n.y-world.py)>=n.radius+7.8,"NPC walked through player: "+n.name);}
  System.out.println("Unique identities, full village/house/stair cycles, actor/player collision, dialogue and restore: passed.");
 }
}
