package de.pixelabenteuer.game;

import java.util.*;

/** One persistent actor per identity, shared by the village and its interiors. */
public final class VillageResidents {
 public static final int OUTSIDE=-1, SMITHY=-2;
 public interface World {
  boolean blocked(int floor,float x,float y);
  boolean road(float x,float y);
  int playerFloor();float playerX();float playerY();
 }
 public static final class Visit {
  public final int house,room;
  Visit(int house,int room){this.house=house;this.room=room;}
 }
 public static final class Resident {
  public final String id,name,title,idle,walk,kind;public final String[] lines;
  public final int cw,ch,columns,rows,foot,merchant;public final float scale,radius;
  public final boolean mobile,legacy;
  public int floor,row=0,visitIndex=0,roamIndex,stops,visitedRooms=0,completedVisits=0;
  public float x,y,anim=0,pause=0,hold=0,speechTime=0,wait=0;
  public boolean moving=false;public String speech="";
  public final Visit[] visits;
  private final int initialFloor;private final float initialX,initialY;private final int order;
  private int stage=0,goalStage=0,pathIndex=0;private float goalX,goalY;
  private final ArrayList<float[]> path=new ArrayList<>();
  public int doorHouse=-1;public float transition=0;private boolean switched=false,entering=false,stairs=false;private int transferRoom;private float transferX,transferY;
  private int speechIndex=0;
  Resident(String id,String name,String title,String art,String kind,int cw,int ch,int columns,int rows,int foot,float scale,int floor,float x,float y,int merchant,boolean mobile,boolean legacy,int order,String[] lines,Visit... visits){
   this.id=id;this.name=name;this.title=title;this.kind=kind;this.idle=art==null?null:"guild/"+art+(mobile&&kind==null?"_Idle_without_shadow":"");this.walk=mobile?(kind==null?"guild/"+art+"_Walk_without_shadow":"built-in"):null;
   this.cw=cw;this.ch=ch;this.columns=columns;this.rows=rows;this.foot=foot;this.scale=scale;this.floor=floor;this.x=x;this.y=y;this.initialFloor=floor;this.initialX=x;this.initialY=y;this.merchant=merchant;this.mobile=mobile;this.legacy=legacy;this.order=order;this.lines=lines;this.visits=visits;radius=legacy?13:kind!=null?12:9;reset();
  }
  void reset(){anim=0;speechIndex=0;floor=initialFloor;x=initialX;y=initialY;row=0;stage=0;goalStage=0;visitIndex=0;roamIndex=order;stops=2+order%2;path.clear();pathIndex=0;transition=0;doorHouse=-1;pause=order*1.3f;wait=0;hold=0;speechTime=0;speech="";moving=false;completedVisits=0;visitedRooms=0;}
  public void speak(){speak(lines[speechIndex++%lines.length]);}
  public void speak(String text){speech=text;speechTime=5;hold=5.5f;moving=false;}
  public int doorFrame(){if(transition<=0)return 0;float elapsed=1-transition;return elapsed<.5f?Math.min(3,(int)(elapsed/.125f)):transition>.32f?3:transition>.12f?4:5;}
  public boolean travelling(){return transition>0;}
  public boolean doorway(){return transition>0&&!stairs;}
  public String debug(){return name+" room="+floor+" @ "+x+","+y+" stage="+stage+" goal="+goalX+","+goalY+" wait="+wait+" transition="+transition;}
 }
 public final ArrayList<Resident> people=new ArrayList<>();
 private final HashMap<String,Resident> byId=new HashMap<>();
 private static final float[][] HOUSES={{550,300},{270,320},{830,300},{270,690},{550,690},{830,690}};
 private static final float[] DOORS={0,64,0,0,64,0};
 private static final float[][] STOPS={{410,470},{690,470},{690,840},{410,840},{410,530},{690,620},{550,520},{830,470},{410,220},{1010,430}};
 public VillageResidents(){
  add(fixed("borin","Borin","Schmied",null,0,0,0,0,1,SMITHY,237,321,1,true,"Eine gute Klinge beginnt mit Geduld.","Bring mir Erz, und ich gebe ihm eine Form.","Stumpfe Waffen haben im Wald nichts verloren."));
  add(fixed("maela","Maela","Händlerin",null,0,0,0,0,1,SMITHY,570,335,2,true,"Ein Heiltrank passt noch in jeden Rucksack.","Ich lege gute Ware für Reisende zurück.","Schau dich in Ruhe um; ich habe Zeit."));
  add(fixed("tarek","Tarek","Abenteurer",null,0,0,0,0,1,SMITHY,622,462,-1,true,"Hinter jedem Waldweg wartet eine Geschichte.","Borin bringt selbst alte Klingen wieder in Form.","Mit vollem Rucksack reist es sich langsamer."));
  add(walker("lina","Lina","Kräuterkundlerin","Citizen1",null,410,440,-1,0,new String[]{"Die Minze am Waldrand duftet heute besonders stark.","Für gute Heiltränke braucht es frische Kräuter.","Ich bringe der Gilde neue Vorräte."},new Visit(1,3),new Visit(5,1),new Visit(4,6)));
  add(walker("jorin","Jorin","Bäcker","Citizen2",null,690,620,-1,1,new String[]{"Im Ofen wartet schon das nächste Brot.","Die Gäste im Gasthaus mögen meine Honigbrötchen.","Nach einer langen Reise hilft ein warmes Essen."},new Visit(4,6),new Visit(2,4),new Visit(5,0)));
  add(walker("rovan","Rovan","Dorfwache","Fighter2",null,410,700,-1,2,new String[]{"Solange ich hier bin, bleiben die Wege sicher.","Im Wald solltest du deine Waffe bereithalten.","Aldric erwartet meinen nächsten Wachbericht."},new Visit(5,0),new Visit(5,2),new Visit(2,4),new Visit(3,5)));
  add(walker("grom","Grom","Händler",null,"orc2",515,525,0,3,new String[]{"Grom hat gute Ware und faire Preise.","Eine Münze bleibt eine Münze, auch für einen Ork.","Im Gasthaus tausche ich Neuigkeiten und Vorräte."},new Visit(2,4),new Visit(5,0),new Visit(1,3)));
  add(fixed("aldric","Aldric","Gildenmeister","Guildmaster",48,32,6,26,2,2,336,126,-1,false,"Die Gilde steht jedem mutigen Herzen offen.","Unsere Aufträge halten das Dorf zusammen.","Rovan berichtet mir von den Wegen außerhalb des Dorfes."));
  add(fixed("kael","Kael","Waffenmeister","Fighter_sword_without_shadow",64,64,6,40,1.8f,0,484,218,-1,false,"Erst die Haltung, dann der Hieb.","Die Übungspuppen verzeihen Fehler; Orks tun das nicht.","Eine ruhige Hand trifft zuverlässiger."));
  add(fixed("varek","Varek","Feuermagier","Mage1_without_shadow",48,64,7,40,1.5f,2,214,226,-1,false,"Feuer gehorcht nur einem ruhigen Geist.","Halt etwas Abstand zu meinen Funken.","Ein kleiner Zauber kann große Wärme schenken."));
  add(fixed("elian","Elian","Zauberlehrling","Mage2_without_shadow",48,64,7,40,1.5f,2,458,226,-1,false,"Diesmal war die Flamme beinahe gerade.","Varek sagt, ich soll zuerst das Atmen üben.","Jeder große Zauber begann einmal ganz klein."));
  add(fixed("orin","Orin","Alchemist","Mage3_without_shadow",48,64,7,40,1.5f,3,516,254,-1,false,"Ein Tropfen zu viel verändert den ganzen Trank.","Lina kennt die besten Kräuter dieses Waldes.","Manche Zutaten entfalten erst bei Mondlicht ihre Kraft."));
  add(fixed("lyra","Lyra","Magierin","Mage4_without_shadow",48,64,7,40,1.5f,2,458,310,-1,false,"Die Runen bewahren alte Erinnerungen.","Magie braucht Aufmerksamkeit, nicht Hast.","Im Wald spüre ich eine seltsame Unruhe."));
  add(fixed("alwin","Alwin","Chronist","Reader1",32,48,11,40,1.4f,1,190,252,-1,false,"Jede Reise verdient eine Zeile in der Chronik.","Dieses Drachenskelett ist älter als unsere Gilde.","Die nächste Seite verrät vielleicht das Geheimnis."));
  add(fixed("mira","Mira","Gelehrte","Reader2",16,32,2,30,1.6f,1,482,252,-1,false,"Die alten Karten passen nicht mehr zu allen Wegen.","Wissen ist leichter zu tragen als eine zweite Rüstung.","Alwin und ich vergleichen gerade die alten Reiseberichte."));
  add(fixed("neris","Neris","Reisende","Talking_person1_without_shadow",32,32,12,30,1.6f,4,516,258,-1,false,"Dieses Gasthaus ist wärmer als mein letztes Lager.","Im Norden sollen noch unentdeckte Wege liegen.","Ein sicherer Rastplatz ist auf Reisen viel wert."));
  add(fixed("edda","Edda","Wirtin","Talking_person2_without_shadow",32,32,12,30,1.6f,4,156,258,-1,false,"Setz dich einen Moment; der Weg läuft nicht davon.","Jorin bringt bald wieder frisches Brot.","Bei mir bekommt jede gute Geschichte ein offenes Ohr."));
 }
 private Resident fixed(String id,String name,String title,String art,int cw,int ch,int cols,int foot,float scale,int room,float x,float y,int merchant,boolean legacy,String... lines){return new Resident(id,name,title,art,null,cw,ch,cols,1,foot,scale,room,x,y,merchant,false,legacy,0,lines);}
 private Resident walker(String id,String name,String title,String art,String kind,float x,float y,int merchant,int order,String[] lines,Visit... visits){return new Resident(id,name,title,art,kind,32,32,12,4,"Fighter2".equals(art)?28:30,1.6f,OUTSIDE,x,y,merchant,true,false,order,lines,visits);}
 private void add(Resident n){if(byId.put(n.id,n)!=null)throw new IllegalStateException("Duplicate resident "+n.id);people.add(n);}
 public Resident get(String id){return byId.get(id);}
 public void reset(){for(Resident n:people)n.reset();}
 public boolean occupied(int floor,float x,float y,Resident except,float radius){
  for(Resident n:people)if(n!=except&&n.floor==floor&&Math.hypot(n.x-x,n.y-y)<n.radius+radius+1)return true;return false;
 }
 private boolean playerAt(World world,int floor,float x,float y,float radius){return world.playerFloor()==floor&&Math.hypot(world.playerX()-x,world.playerY()-y)<radius+8;}
 public Resident nearby(World world){Resident best=null;double distance=Double.POSITIVE_INFINITY;for(Resident n:people)if(n.floor==world.playerFloor()&&!n.travelling()){
  double d=Math.hypot(n.x-world.playerX(),n.y-world.playerY());if(d<(n.id.equals("aldric")?86:56)&&d<distance){best=n;distance=d;}
 }return best;}
 private boolean free(World w,Resident n,int floor,float x,float y,boolean actors){return !w.blocked(floor,x,y)&&(!actors||!occupied(floor,x,y,n,n.radius)&&!playerAt(w,floor,x,y,n.radius));}
 private int indoorCount(){int count=0;for(Resident n:people)if(n.mobile&&(n.floor>=0||n.transition>0||n.stage>=2))count++;return count;}
 public void update(float dt,World world){
  for(Resident n:people){
   n.anim+=dt;n.speechTime=Math.max(0,n.speechTime-dt);n.hold=Math.max(0,n.hold-dt);n.moving=false;
   if(!n.mobile||n.hold>0)continue;
   if(n.transition>0){advanceTransfer(n,dt,world);continue;}
   n.pause=Math.max(0,n.pause-dt);if(n.pause>0)continue;
   if(n.stage==0){
    if(n.stops<=0&&indoorCount()<2){Visit visit=n.visits[n.visitIndex%n.visits.length];n.doorHouse=visit.house;n.stage=2;setGoal(n,HOUSES[visit.house][0]+DOORS[visit.house],HOUSES[visit.house][1]+132);}
    else{float[] p=STOPS[n.roamIndex++%STOPS.length];n.stage=1;setGoal(n,p[0],p[1]);}
   }
   if(n.stage==1||n.stage==2||n.stage==4||n.stage==6||n.stage==8||n.stage==10){
    if(move(n,dt,world))arrive(n,world);
   }else if(n.stage==5){n.pause=10+(n.order%3)*3;n.stage=7;}
   else if(n.stage==7){exitRoom(n);}
   else if(n.stage==3){beginTransfer(n,0,336,312,true,false);}
   else if(n.stage==9){beginTransfer(n,OUTSIDE,HOUSES[n.doorHouse][0]+DOORS[n.doorHouse],HOUSES[n.doorHouse][1]+164,false,false);}
  }
 }
 private void arrive(Resident n,World world){
  n.path.clear();n.pathIndex=0;n.wait=0;
  if(n.stage==1){n.stops--;n.pause=1.5f+n.order*.4f;n.stage=0;}
  else if(n.stage==2)n.stage=3;
  else if(n.stage==4){n.visitedRooms|=1<<n.floor;n.stage=5;}
  else if(n.stage==6){beginTransfer(n,n.floor+1,80,254,true,true);}
  else if(n.stage==8){beginTransfer(n,n.floor-1,592,254,false,true);}
  else if(n.stage==10)n.stage=9;
 }
 private void enterRoom(Resident n){
  Visit v=n.visits[n.visitIndex%n.visits.length];
  if(v.house==5&&n.floor<v.room){n.stage=6;setGoal(n,592,232);}
  else{n.stage=4;setGoal(n,260+n.order*48,236+n.order%2*70);}
 }
 private void exitRoom(Resident n){
  if(n.doorHouse==5&&n.floor>0){n.stage=8;setGoal(n,80,232);}
  else{n.stage=10;setGoal(n,336,326);}
 }
 private void beginTransfer(Resident n,int destination,float x,float y,boolean entering,boolean stairs){
  Visit v=n.visits[n.visitIndex%n.visits.length];
  n.transferRoom=stairs?destination:entering?(v.house==5?0:v.room):OUTSIDE;
  n.transferX=x;n.transferY=y;n.entering=entering;n.stairs=stairs;n.transition=1;n.switched=false;n.path.clear();n.pathIndex=0;
 }
 private void advanceTransfer(Resident n,float dt,World w){
  float next=Math.max(0,n.transition-dt);
  if(!n.switched&&next<=.5f){
   if(!free(w,n,n.transferRoom,n.transferX,n.transferY,true)){n.transition=.51f;return;}
   n.floor=n.transferRoom;n.x=n.transferX;n.y=n.transferY;n.switched=true;
  }
  n.transition=next;
  if(next==0){
   if(n.stairs){if(n.entering)enterRoom(n);else exitRoom(n);}
   else if(n.entering)enterRoom(n);
   else{n.completedVisits++;n.visitIndex=(n.visitIndex+1)%n.visits.length;n.stops=2+n.order%2;n.stage=0;n.doorHouse=-1;n.pause=1;}
  }
 }
 private void setGoal(Resident n,float x,float y){n.goalX=x;n.goalY=y;n.path.clear();n.pathIndex=0;n.wait=0;}
 private boolean move(Resident n,float dt,World w){
  float distance=(float)Math.hypot(n.goalX-n.x,n.goalY-n.y);if(distance<1){n.x=n.goalX;n.y=n.goalY;return true;}
  if(n.pathIndex>=n.path.size()){
   n.path.clear();n.path.addAll(findPath(w,n,n.floor,n.x,n.y,n.goalX,n.goalY,n.wait>1));n.pathIndex=0;
   if(n.path.isEmpty()){n.wait+=.35f;n.pause=.35f;return false;}
  }
  float[] p=n.path.get(n.pathIndex);float dx=p[0]-n.x,dy=p[1]-n.y,d=(float)Math.hypot(dx,dy);
  if(d<1){n.x=p[0];n.y=p[1];n.pathIndex++;return false;}
  float step=Math.min(d,dt*(n.kind==null?34:30));float nx=n.x+dx/d*step,ny=n.y+dy/d*step;
  if(free(w,n,n.floor,nx,ny,true)){
   n.x=nx;n.y=ny;n.row=Math.abs(dx)>Math.abs(dy)?(dx<0?1:2):(dy<0?3:0);n.moving=true;n.wait=0;
  }else{n.wait+=dt;if(n.wait>1.1f){n.path.clear();n.pathIndex=0;}}
  return false;
 }
 /** Four-neighbour A*: plans around furniture and, when yielding, other people. */
 private ArrayList<float[]> findPath(World w,Resident n,int room,float x,float y,float tx,float ty,boolean actors){
  int step=room==OUTSIDE?12:8,minX=room==OUTSIDE?100:16,minY=room==OUTSIDE?110:108;
  int maxX=room==OUTSIDE?1020:656,maxY=room==OUTSIDE?900:332;
  int cols=(maxX-minX)/step+1,rows=(maxY-minY)/step+1,total=cols*rows;
  int sx=Math.max(0,Math.min(cols-1,Math.round((x-minX)/step))),sy=Math.max(0,Math.min(rows-1,Math.round((y-minY)/step)));
  int gx=Math.max(0,Math.min(cols-1,Math.round((tx-minX)/step))),gy=Math.max(0,Math.min(rows-1,Math.round((ty-minY)/step)));
  int start=sy*cols+sx,goal=gy*cols+gx;float[] costs=new float[total];Arrays.fill(costs,Float.POSITIVE_INFINITY);int[] previous=new int[total];Arrays.fill(previous,-1);boolean[] closed=new boolean[total];
  PriorityQueue<float[]> open=new PriorityQueue<>((a,b)->Float.compare(a[1],b[1]));costs[start]=0;open.add(new float[]{start,0});
  while(!open.isEmpty()){
   int current=(int)open.poll()[0];if(closed[current])continue;closed[current]=true;if(current==goal)break;
   int cx=current%cols,cy=current/cols;
   for(int k=0;k<4;k++){
    int xx=cx+(k==0?-1:k==1?1:0),yy=cy+(k==2?-1:k==3?1:0);if(xx<0||yy<0||xx>=cols||yy>=rows)continue;
    int index=yy*cols+xx;float px=minX+xx*step,py=minY+yy*step;
    if(closed[index]||index!=goal&&!free(w,n,room,px,py,actors)||w.blocked(room,px,py))continue;
    float extra=room==OUTSIDE&&!w.road(px,py)?2.4f:1;
    float next=costs[current]+extra;if(next>=costs[index])continue;costs[index]=next;previous[index]=current;
    open.add(new float[]{index,next+Math.abs(xx-gx)+Math.abs(yy-gy)});
   }
  }
  ArrayList<float[]> result=new ArrayList<>();if(!closed[goal])return result;
  for(int at=goal;at!=start;at=previous[at]){if(at<0){result.clear();return result;}result.add(new float[]{minX+(at%cols)*step,minY+(at/cols)*step});}
  Collections.reverse(result);result.add(new float[]{tx,ty});return result;
 }
 public void restore(String id,int floor,float x,float y,int visitIndex,World world){
  Resident n=byId.get(id);if(n==null||!n.mobile)return;n.reset();
  if(!Float.isFinite(x)||!Float.isFinite(y)||floor<OUTSIDE||floor>6)return;
  n.visitIndex=Math.floorMod(visitIndex,n.visits.length);
  if(!free(world,n,floor,x,y,true))return;
  n.floor=floor;n.x=x;n.y=y;n.pause=2;
  if(floor>=0){n.doorHouse=n.visits[n.visitIndex].house;n.stage=7;}else n.stage=0;
 }
}
