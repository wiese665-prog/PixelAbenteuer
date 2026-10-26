package de.pixelabenteuer.game;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
 @Override public void onCreate(Bundle b) {
  super.onCreate(b);
  getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);
  setContentView(new Game());
 }
 class Mob {
  String kind,action="idle";float x,y,vx,vy,anim=0,decision=0,attackTimer=0,hurtTimer=0,deathTimer=0,healthVisible=0;int hp,maxHp,level,damage,dir=0;boolean dead=false;float damageNumberTime=0;int lastDamage=0;
  Mob(String k,float xx,float yy) {kind=k;x=xx;y=yy;level=1+(int)(Math.random()*(k.startsWith("orc")?6:4));int baseHp=k.equals("orc3")?12:k.equals("orc2")?9:k.equals("orc1")?7:k.equals("boar")?8:k.equals("deer")?5:k.equals("fox")?4:2;maxHp=baseHp+(level-1)*(k.equals("boar")||k.startsWith("orc")?3:2);hp=maxHp;damage=(k.equals("orc3")?3:k.equals("orc2")?2:k.equals("orc1")?1:0)+(level-1)/3;}
  boolean hostile(){return kind.startsWith("orc");}
 }
 class Decoration {
  Bitmap image,mask;float x,y,w,h;boolean solid;
  Decoration(Bitmap b,Bitmap collision,float xx,float yy,float ww,float hh,boolean s){image=b;mask=collision==null?b:collision;x=xx;y=yy;w=ww;h=hh;solid=s;}
 }
 class Drop {String item;float x,y,bob;Drop(String i,float xx,float yy,float t){item=i;x=xx;y=yy;bob=t;}}
 class Game extends View {
  final int WORLD_W=1600,WORLD_H=1000;
  Canvas canvas;Paint paint=new Paint();Random random=new Random(42);
  ArrayList<Mob> mobs=new ArrayList<>();ArrayList<Decoration> decorations=new ArrayList<>();ArrayList<Drop> drops=new ArrayList<>();LinkedHashMap<String,Integer> inventory=new LinkedHashMap<>();boolean inventoryOpen=false;String pickupNotice="";float noticeTime=0;
  HashMap<String,Bitmap> sprites=new HashMap<>();
  float px=180,py=180,joyX=0,joyY=0,elapsed=0,attackAnim=0,hurtAnim=0,deathAnim=0,attackCooldown=0,playerHealthVisible=0;
  int hp=12,maxHp=12,kills=0,animalKills=0,playerDir=0,playerLevel=1,exp=0,attackPower=3,lastPlayerDamage=0;float playerDamageTime=0;float swordTimer=0;boolean sword=false;
  long previous=System.nanoTime();
  Game(){super(MainActivity.this);setFocusable(true);setFocusableInTouchMode(true);paint.setFilterBitmap(false);loadSprites();loadDecorations();spawnMobs();}
  void spawnMobs(){
   String[] animals={"boar","fox","deer","black_grouse"};
   for(int i=0;i<28;i++){float x=80+random.nextInt(1440),y=80+random.nextInt(840);if(!blocked(x,y))mobs.add(new Mob(animals[i%4],x,y));}
   for(int i=0;i<18;i++){float x=380+random.nextInt(1100),y=80+random.nextInt(840);if(!blocked(x,y))mobs.add(new Mob("orc"+(1+i%3),x,y));}
  }
  void loadSprites(){
   String[] kinds={"boar","fox","deer","black_grouse","orc1","orc2","orc3","player_sword","player_unarmed"};
   String[] actions={"idle","walk","run","attack","run_attack","walk_attack","flight","hurt","death"};
   String[] dirs={"front","back","left","right"};
   for(String k:kinds)for(String a:actions)for(String d:dirs){
    String key=k+"_"+a+"_"+d;
    try(InputStream in=getAssets().open("generated/"+key+".png")){Bitmap b=BitmapFactory.decodeStream(in);if(b!=null)sprites.put(key,b);}catch(Exception ignored){}
   }
  }
  Bitmap asset(String filename){
   try(InputStream in=getAssets().open("sprites/"+filename)){return BitmapFactory.decodeStream(in);}catch(Exception ignored){return null;}
  }
  void loadDecorations(){
   ArrayList<Bitmap> rocks=new ArrayList<>(),rockMasks=new ArrayList<>(),ruins=new ArrayList<>(),plants=new ArrayList<>();
   String[] ruinTypes={"Blue-gray_ruins","Brown-gray_ruins","Brown_ruins","Sand_ruins"};
   for(String type:ruinTypes)for(int i=1;i<=5;i++){Bitmap b=asset(type+i+".png");if(b!=null)ruins.add(b);}
   for(int type=1;type<=6;type++)for(int i=1;i<=5;i++){Bitmap b=asset("Rock"+type+"_"+i+".png");if(b!=null){rocks.add(b);Bitmap mask=asset("Rock"+type+"_"+i+"_no_shadow.png");rockMasks.add(mask==null?b:mask);}}
   try{
    String[] filenames=getAssets().list("sprites");
    if(filenames!=null)for(String filename:filenames){
     String lower=filename.toLowerCase(java.util.Locale.US);
     if(lower.endsWith(".png")&&(lower.contains("bush")||lower.contains("cactus")||lower.contains("fern")||lower.contains("fran")||lower.contains("plant"))){
      Bitmap b=asset(filename);if(b!=null)plants.add(b);
     }
    }
   }catch(Exception ignored){}
   for(int i=0;i<19;i++){float x=300+(i*251)%1200,y=90+(i*197)%810;Bitmap b=ruins.isEmpty()?null:ruins.get(i%ruins.size());decorations.add(new Decoration(b,b,x,y,94,90,true));}
   for(int i=0;i<85;i++){float x=60+(i*167)%1480,y=65+(i*283)%870;if(Math.hypot(x-180,y-180)<140)continue;Bitmap b=rocks.isEmpty()?null:rocks.get(i%rocks.size());decorations.add(new Decoration(b,rockMasks.isEmpty()?b:rockMasks.get(i%rockMasks.size()),x,y,20+(i%3)*4,19+(i%4)*4,true));}
   for(int i=0;i<70&&!plants.isEmpty();i++){float x=30+(i*131)%1540,y=40+(i*223)%920;decorations.add(new Decoration(plants.get(i%plants.size()),plants.get(i%plants.size()),x,y,34+(i%3)*12,40+(i%3)*10,true));}
  }
  // Per-pixel collisions use the very same destination rectangle as rendering.
  // Test the feet/body of the moving character against opaque object pixels.
  boolean collides(Decoration d,float x,float y){
   Bitmap mask=d.mask;
   float left=d.x-d.w/2,top=d.y-d.h*.8f;
   if(x+10<left||x-10>left+d.w||y+12<top||y-12>top+d.h)return false;
   if(mask==null)return Math.abs(x-d.x)<d.w*.4f+9&&Math.abs(y-d.y)<d.h*.4f+9;
   int bw=mask.getWidth(),bh=mask.getHeight();
   for(int ix=-2;ix<=2;ix++)for(int iy=-2;iy<=2;iy++){
    float sampleX=x+ix*4f,sampleY=y+iy*4f;
    float u=(sampleX-left)/d.w,v=(sampleY-top)/d.h;
    if(u<0||u>=1||v<0||v>=1)continue;
    int sx=Math.min(bw-1,(int)(u*bw)),sy=Math.min(bh-1,(int)(v*bh));
    if(Color.alpha(mask.getPixel(sx,sy))>=200)return true;
   }
   return false;
  }
  boolean blocked(float x,float y){
   if(x<18||y<18||x>WORLD_W-18||y>WORLD_H-18)return true;
   for(Decoration d:decorations)if(d.solid&&collides(d,x,y))return true;
   return false;
  }
  int expNeeded(){return 12+playerLevel*8;}
  void earnExp(int amount){
   exp+=amount;
   while(exp>=expNeeded()){
    exp-=expNeeded();playerLevel++;maxHp+=4;hp=Math.min(maxHp,hp+4);attackPower++;
    playerHealthVisible=3f;
   }
  }
  void damageText(float x,float y,int damage,float remaining){
   if(remaining<=0)return;
   float age=1f-remaining/.9f;
   float rise=age*31f,spread=(float)Math.sin(damage*7.1)*9f;
   paint.setTypeface(Typeface.create("monospace",Typeface.BOLD));
   paint.setTextSize(21f+5f*(1f-age));paint.setTextAlign(Paint.Align.CENTER);
   String label="-"+damage;float tx=x+spread,ty=y-rise;
   paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(4);paint.setColor(col("#382029"));
   canvas.drawText(label,tx,ty,paint);
   paint.setStyle(Paint.Style.FILL);paint.setColor(col("#ffbc58"));canvas.drawText(label,tx,ty,paint);
   paint.setColor(col("#fff2ae"));canvas.drawText(label,tx-1,ty-2,paint);
   paint.setTypeface(Typeface.DEFAULT);paint.setTextAlign(Paint.Align.LEFT);
  }
  // 24x24 pixel-art item icons: layered outlines, highlights and earthy palette.
  void itemIcon(String item,float x,float y,float scale){
   canvas.save();canvas.translate(x,y);canvas.scale(scale,scale);
   int outline=col("#292634"),shadow=col("#58404a"),bone=col("#f2dfb3"),cream=col("#fff1cc");
   int meat=col("#b94d54"),meatHi=col("#ec8a72"),leather=col("#986344"),leatherHi=col("#d49b64");
   int steel=col("#94b8bf"),steelHi=col("#e3f0df"),steelDark=col("#506d7c"),gold=col("#e2b75f");
   if(item.equals("Fleisch")){
    drawRect(outline,3,8,14,11);drawRect(outline,7,5,9,14);
    drawRect(meat,4,8,12,9);drawRect(meat,8,6,7,11);
    drawRect(meatHi,6,8,7,4);drawRect(col("#f4b08a"),8,7,4,2);
    drawRect(shadow,7,15,8,2);drawRect(bone,14,15,5,3);drawRect(cream,18,13,3,4);drawRect(cream,18,17,3,3);
   }else if(item.equals("Leder")){
    drawRect(outline,4,3,15,19);drawRect(leather,5,4,13,17);
    drawRect(leatherHi,7,5,9,14);drawRect(col("#b47d51"),8,7,7,9);
    drawRect(outline,5,3,4,4);drawRect(outline,15,3,4,4);drawRect(outline,5,18,4,4);drawRect(outline,15,18,4,4);
    drawRect(cream,8,7,2,2);drawRect(shadow,14,14,2,3);drawRect(leather,10,11,4,6);
   }else if(item.equals("Feder")){
    drawRect(outline,12,2,5,3);drawRect(outline,8,5,10,12);drawRect(outline,5,9,9,9);
    drawRect(steelHi,12,3,4,5);drawRect(col("#c9d7c3"),9,6,7,9);drawRect(steel,6,10,8,6);
    drawRect(cream,12,5,2,9);drawRect(shadow,9,14,4,3);drawRect(bone,9,14,2,7);drawRect(cream,10,13,2,9);
   }else if(item.equals("Knochen")){
    drawRect(outline,4,4,7,7);drawRect(outline,13,13,7,7);drawRect(outline,8,8,9,9);
    drawRect(bone,5,5,5,5);drawRect(bone,14,14,5,5);drawRect(bone,9,8,5,9);
    drawRect(cream,6,5,3,3);drawRect(cream,10,8,3,7);drawRect(shadow,14,15,3,2);
    drawRect(cream,3,7,4,3);drawRect(cream,17,16,4,3);
   }else if(item.equals("Horn")){
    drawRect(outline,3,15,15,6);drawRect(outline,7,10,12,7);drawRect(outline,12,5,8,7);
    drawRect(leather,4,16,13,4);drawRect(bone,8,11,10,6);drawRect(cream,13,6,6,6);
    drawRect(shadow,6,17,9,2);drawRect(gold,9,12,5,2);drawRect(cream,17,3,3,5);drawRect(steelHi,18,2,2,3);
   }else if(item.equals("Schwert")){
    drawRect(outline,4,17,6,6);drawRect(leather,5,17,4,5);drawRect(gold,3,14,12,3);
    drawRect(outline,10,4,7,12);drawRect(steelDark,11,5,5,10);drawRect(steel,12,4,4,10);
    drawRect(steelHi,14,3,3,9);drawRect(cream,15,2,2,3);drawRect(gold,6,16,2,3);
   }else if(item.equals("Ruestung")){
    drawRect(outline,5,3,14,19);drawRect(outline,2,6,6,11);drawRect(outline,16,6,6,11);
    drawRect(steelDark,6,4,12,17);drawRect(steel,8,5,8,15);
    drawRect(steelDark,3,7,5,9);drawRect(steelDark,16,7,5,9);
    drawRect(steelHi,9,6,6,3);drawRect(gold,10,10,4,5);drawRect(steelHi,10,16,4,3);
    drawRect(shadow,7,20,10,2);
   }else if(item.equals("Axt")){
    drawRect(outline,12,3,5,19);drawRect(leather,13,5,3,16);drawRect(leatherHi,14,6,1,13);
    drawRect(outline,3,4,12,12);drawRect(steelDark,4,5,10,10);
    drawRect(steel,4,6,8,8);drawRect(steelHi,4,7,3,6);drawRect(gold,11,10,4,2);
    drawRect(outline,3,8,2,4);
   }
   canvas.restore();
  }
  void addDrop(String item,float x,float y){
   drops.add(new Drop(item,x+(random.nextFloat()-.5f)*22,y+(random.nextFloat()-.5f)*22,random.nextFloat()*6f));
  }
  void loot(Mob m){
   if(m.kind.equals("black_grouse")){
    addDrop("Feder",m.x,m.y);if(random.nextFloat()<.8f)addDrop("Fleisch",m.x,m.y);
   }else if(!m.hostile()){
    addDrop("Fleisch",m.x,m.y);
    if(random.nextFloat()<(m.kind.equals("boar")?.85f:.6f))addDrop("Leder",m.x,m.y);
    if(m.kind.equals("boar")&&random.nextFloat()<.3f)addDrop("Horn",m.x,m.y);
   }else{
    if(random.nextFloat()<.65f)addDrop("Knochen",m.x,m.y);
    if(random.nextFloat()<.3f)addDrop("Horn",m.x,m.y);
    if(random.nextFloat()<.18f)addDrop("Schwert",m.x,m.y);
    if(random.nextFloat()<.12f)addDrop("Ruestung",m.x,m.y);
    if(random.nextFloat()<.12f)addDrop("Axt",m.x,m.y);
   }
  }
  Drop nearestDrop(){
   Drop nearest=null;float best=65;
   for(Drop d:drops){float distance=(float)Math.hypot(px-d.x,py-d.y);if(distance<best){best=distance;nearest=d;}}
   return nearest;
  }
  void pickup(){
   if(inventoryOpen)return;
   Drop d=nearestDrop();if(d==null)return;
   inventory.put(d.item,inventory.containsKey(d.item)?inventory.get(d.item)+1:1);
   pickupNotice="+1 "+d.item;noticeTime=1.8f;drops.remove(d);
  }
  void drawInventory(float vw,float vh){
   drawRect(col("#151b20"),vw/2-235,vh/2-170,470,340);
   drawRect(col("#a78b61"),vw/2-231,vh/2-166,462,332);
   drawRect(col("#292f34"),vw/2-225,vh/2-160,450,320);
   paint.setColor(col("#f5deb0"));paint.setTextSize(25);paint.setTypeface(Typeface.DEFAULT_BOLD);
   canvas.drawText("INVENTAR",vw/2-205,vh/2-123,paint);
   paint.setTextSize(14);canvas.drawText("I - Schliessen",vw/2+112,vh/2-128,paint);
   String[] items={"Fleisch","Leder","Feder","Knochen","Horn","Schwert","Ruestung","Axt"};
   for(int i=0;i<items.length;i++){
    float x=vw/2-205+(i%4)*105,y=vh/2-102+(i/4)*100;
    drawRect(col("#121b20"),x,y,91,88);drawRect(col("#46515a"),x+3,y+3,85,82);
    itemIcon(items[i],x+22,y+8,1.9f);
    paint.setColor(Color.WHITE);paint.setTextSize(11);canvas.drawText(items[i],x+7,y+66,paint);
    int amount=inventory.containsKey(items[i])?inventory.get(items[i]):0;
    paint.setTextSize(14);paint.setColor(col("#f8db94"));canvas.drawText("x"+amount,x+62,y+20,paint);
   }
   paint.setTypeface(Typeface.DEFAULT);
  }
  void healthBar(float x,float y,float width,int current,int maximum){
   float ratio=Math.max(0,Math.min(1,current/(float)Math.max(1,maximum)));
   drawRect(col("#231f20"),x-width/2-2,y-2,width+4,9);
   drawRect(col("#7a2d2b"),x-width/2,y,width,5);
   drawRect(col("#60c86c"),x-width/2,y,width*ratio,5);
  }
  void drawRect(int col,float x,float y,float w,float h){paint.setColor(col);canvas.drawRect(x,y,x+w,y+h,paint);}
  int col(String hex){return Color.parseColor(hex);}
  void drawSprite(String kind,String action,int dir,float x,float y,float w,float h,float time,boolean freeze){
   String direction=new String[]{"front","back","left","right"}[Math.max(0,Math.min(3,dir))];
   Bitmap b=sprites.get(kind+"_"+action+"_"+direction);
   if(b==null)b=sprites.get(kind+"_idle_"+direction);
   if(b==null){drawRect(kind.startsWith("orc")?col("#71994b"):kind.startsWith("player")?col("#4775a0"):col("#b98d62"),x-w/2,y-h/2,w,h);return;}
   int frameWidth=b.getHeight();int count=Math.max(1,b.getWidth()/Math.max(1,frameWidth));
   int frame=freeze?count-1:((int)(Math.max(0,time)*8))%count;
   paint.setColor(Color.WHITE);paint.setFilterBitmap(false);
   canvas.drawBitmap(b,new Rect(frame*frameWidth,0,(frame+1)*frameWidth,b.getHeight()),new RectF(x-w/2,y-h/2,x+w/2,y+h/2),paint);
  }
  void update(float dt){
   elapsed+=dt;noticeTime=Math.max(0,noticeTime-dt);swordTimer=Math.max(0,swordTimer-dt);sword=swordTimer>0;attackCooldown=Math.max(0,attackCooldown-dt);attackAnim=Math.max(0,attackAnim-dt);hurtAnim=Math.max(0,hurtAnim-dt);playerHealthVisible=Math.max(0,playerHealthVisible-dt);playerDamageTime=Math.max(0,playerDamageTime-dt);
   if(hp<=0){deathAnim+=dt;return;}if(inventoryOpen)return;
   float mx=joyX,my=joyY,length=(float)Math.hypot(mx,my);
   if(length>1){mx/=length;my/=length;}
   if(Math.abs(mx)+Math.abs(my)>.08f){
    playerDir=Math.abs(mx)>Math.abs(my)?(mx<0?2:3):(my<0?1:0);
    float step=140*dt/3f;
    for(int sub=0;sub<3;sub++){
     float nx=px+mx*step,ny=py+my*step;
     if(!blocked(nx,py))px=nx;
     if(!blocked(px,ny))py=ny;
    }
   }
   for(Mob m:mobs){
    if(m.dead){m.deathTimer+=dt;continue;}
    m.anim+=dt;m.attackTimer=Math.max(0,m.attackTimer-dt);m.hurtTimer=Math.max(0,m.hurtTimer-dt);m.healthVisible=Math.max(0,m.healthVisible-dt);m.damageNumberTime=Math.max(0,m.damageNumberTime-dt);
    float distance=(float)Math.hypot(px-m.x,py-m.y);
    float vx=0,vy=0;String action="idle";
    if(m.hurtTimer>0){action="hurt";}
    else if(m.hostile()&&distance<330){
     m.dir=direction(px-m.x,py-m.y);
     if(distance>43){vx=(px-m.x)/Math.max(1,distance)*75;vy=(py-m.y)/Math.max(1,distance)*75;action="run";}
     else {action="attack";if(m.attackTimer<=0){int dealt=Math.max(1,m.damage);hp=Math.max(0,hp-dealt);lastPlayerDamage=dealt;playerDamageTime=.9f;hurtAnim=.32f;playerHealthVisible=3f;m.attackTimer=1.3f;}}
    }else if(!m.hostile()&&distance<110){
     vx=(m.x-px)/Math.max(1,distance)*105;vy=(m.y-py)/Math.max(1,distance)*105;
     action=m.kind.equals("black_grouse")?"flight":"run";
    }else{
     m.decision-=dt;
     if(m.decision<=0){m.decision=1.2f+random.nextFloat()*2.5f;float angle=random.nextFloat()*6.28318f;
      if(random.nextFloat()<.27f){m.vx=0;m.vy=0;}else{m.vx=(float)Math.cos(angle)*35;m.vy=(float)Math.sin(angle)*35;}
     }
     vx=m.vx;vy=m.vy;action=Math.abs(vx)+Math.abs(vy)<1?"idle":"walk";
    }
    if(Math.abs(vx)+Math.abs(vy)>1){
     float step=dt/3f;
     for(int sub=0;sub<3;sub++){
      float nx=m.x+vx*step,ny=m.y+vy*step;
      if(!blocked(nx,m.y))m.x=nx;else m.decision=0;
      if(!blocked(m.x,ny))m.y=ny;else m.decision=0;
     }
     m.dir=direction(vx,vy);
    }
    if(!m.action.equals(action)){m.action=action;m.anim=0;}
   }
  }
  int direction(float vx,float vy){return Math.abs(vx)>Math.abs(vy)?(vx<0?2:3):(vy<0?1:0);}
  void attack(){
   if(hp<=0||attackCooldown>0||inventoryOpen)return;
   attackCooldown=.42f;attackAnim=.4f;swordTimer=3f;sword=true;
   float fx=playerDir==2?-1:playerDir==3?1:0,fy=playerDir==1?-1:playerDir==0?1:0;
   for(Mob m:mobs){
    if(m.dead)continue;
    float dx=m.x-px,dy=m.y-py,d=(float)Math.hypot(dx,dy);
    if(d>76||d<.01f)continue;
    if((dx*fx+dy*fy)/d<-.1f)continue;
    m.hp-=attackPower;m.lastDamage=attackPower;m.damageNumberTime=.9f;m.hurtTimer=.28f;m.healthVisible=3f;m.anim=0;
    if(m.hp<=0){m.dead=true;m.deathTimer=0;if(m.hostile())kills++;else animalKills++;earnExp((m.hostile()?8:3)+m.level*2);loot(m);}
   }
  }
  void restart(){
   playerLevel=1;exp=0;attackPower=3;maxHp=12;hp=maxHp;playerDamageTime=0;px=180;py=180;joyX=joyY=0;deathAnim=0;swordTimer=0;sword=false;attackAnim=0;playerHealthVisible=0;kills=animalKills=0;mobs.clear();drops.clear();inventory.clear();inventoryOpen=false;spawnMobs();
  }
  @Override protected void onDraw(Canvas c){
   canvas=c;float dt=Math.min(.05f,(System.nanoTime()-previous)/1000000000f);previous=System.nanoTime();update(dt);
   float w=getWidth(),h=getHeight(),scale=Math.min(w/800f,h/450f),vw=w/scale,vh=h/scale;
   paint.reset();paint.setAntiAlias(false);paint.setFilterBitmap(false);paint.setColor(Color.WHITE);
   c.drawColor(col("#407d46"));c.save();c.scale(scale,scale);c.save();c.translate(vw/2-px,vh/2-py);
   int startX=(int)Math.floor((px-vw/2)/32f)*32-32,endX=(int)Math.ceil((px+vw/2)/32f)*32+32;
   int startY=(int)Math.floor((py-vh/2)/32f)*32-32,endY=(int)Math.ceil((py+vh/2)/32f)*32+32;
   for(int x=startX;x<endX;x+=32)for(int y=startY;y<endY;y+=32){
    int n=(x*37+y*19)%7;drawRect(n<3?col("#407d46"):col("#47884a"),x,y,32,32);
    if(n==1)drawRect(col("#6a9a53"),x+8,y+7,5,4);
   }
   for(Decoration d:decorations){
    if(d.image!=null){paint.setColor(Color.WHITE);c.drawBitmap(d.image,null,new RectF(d.x-d.w/2,d.y-d.h*.8f,d.x+d.w/2,d.y+d.h*.2f),paint);}
    else drawRect(col("#82776b"),d.x-d.w/2,d.y-d.h/2,d.w,d.h);
   }
   for(Drop d:drops){float bob=(float)Math.sin(elapsed*3+d.bob)*2;
    drawRect(col("#28342b"),d.x-11,d.y+7,22,5);itemIcon(d.item,d.x-12,d.y-17+bob,1f);
   }
   for(Mob m:mobs){
    if(m.dead&&m.deathTimer>2.5f)continue;
    String action=m.dead?"death":m.hurtTimer>0?"hurt":m.action;
    float size=m.hostile()?65:m.kind.equals("boar")?45:m.kind.equals("deer")?43:36;
    drawSprite(m.kind,action,m.dir,m.x,m.y,size,size,m.dead?m.deathTimer:m.anim,m.dead&&m.deathTimer>.7f);
    if(!m.dead){paint.setColor(Color.WHITE);paint.setTextSize(12);paint.setTextAlign(Paint.Align.CENTER);canvas.drawText("Lv. "+m.level,m.x,m.y-size*.64f,paint);paint.setTextAlign(Paint.Align.LEFT);}
    if(m.healthVisible>0&&(!m.dead||m.deathTimer<1f))healthBar(m.x,m.y-size*.65f-12,42,m.hp,m.maxHp);
    damageText(m.x,m.y-size*.65f-22,m.lastDamage,m.damageNumberTime);
   }
   String playerKind=sword?"player_sword":"player_unarmed";
   String playerAction=hp<=0?"death":hurtAnim>0?"hurt":attackAnim>0&&sword?"attack":Math.abs(joyX)+Math.abs(joyY)>.08f?"run":"idle";
   drawSprite(playerKind,playerAction,playerDir,px,py,80,80,hp<=0?deathAnim:attackAnim>0?.4f-attackAnim:elapsed,hp<=0&&deathAnim>.8f);
   if(playerHealthVisible>0)healthBar(px,py-56,52,hp,maxHp);
   damageText(px,py-66,lastPlayerDamage,playerDamageTime);
   Drop nearby=nearestDrop();
   if(nearby!=null&&!inventoryOpen){paint.setColor(col("#fff0c5"));paint.setTextSize(13);paint.setTextAlign(Paint.Align.CENTER);
    canvas.drawText("E  "+nearby.item+" aufheben",px,py-60,paint);paint.setTextAlign(Paint.Align.LEFT);}
   c.restore();
   drawRect(col("#142b24"),0,0,vw,37);paint.setColor(Color.WHITE);paint.setTextSize(18);
   c.drawText("Lv. "+playerLevel+"  EXP "+exp+"/"+expNeeded()+"  DMG "+attackPower+"  Orks "+kills+"  Tiere "+animalKills,12,25,paint);
   if(noticeTime>0){paint.setColor(col("#f7e0a6"));paint.setTextSize(17);c.drawText(pickupNotice,12,60,paint);}
   if(inventoryOpen)drawInventory(vw,vh);
   if(hp<=0){drawRect(col("#111111"),vw/2-180,vh/2-45,360,90);paint.setColor(Color.WHITE);paint.setTextSize(24);c.drawText("GAME OVER - Tippen",vw/2-145,vh/2+7,paint);}
   c.restore();postInvalidateDelayed(16);
  }
  @Override public boolean onTouchEvent(MotionEvent e){
   int type=e.getActionMasked();
   if(type==MotionEvent.ACTION_DOWN){
    if(inventoryOpen){inventoryOpen=false;return true;}
    if(hp<=0){restart();return true;}
    // In BlueStacks, a left mouse click arrives as a primary mouse/touch press.
    if((e.getSource()&InputDevice.SOURCE_MOUSE)==InputDevice.SOURCE_MOUSE){
     if(e.getButtonState()==0||(e.getButtonState()&MotionEvent.BUTTON_PRIMARY)!=0)attack();
    }else attack();
   }
   return true;
  }
  @Override public boolean onGenericMotionEvent(MotionEvent e){
   if((e.getSource()&InputDevice.SOURCE_MOUSE)==InputDevice.SOURCE_MOUSE&&e.getActionMasked()==MotionEvent.ACTION_BUTTON_PRESS
      &&e.getActionButton()==MotionEvent.BUTTON_PRIMARY){attack();return true;}
   return super.onGenericMotionEvent(e);
  }
  @Override public boolean onKeyDown(int key,KeyEvent e){
   switch(key){case KeyEvent.KEYCODE_A:case KeyEvent.KEYCODE_DPAD_LEFT:joyX=-1;break;
    case KeyEvent.KEYCODE_D:case KeyEvent.KEYCODE_DPAD_RIGHT:joyX=1;break;
    case KeyEvent.KEYCODE_W:case KeyEvent.KEYCODE_DPAD_UP:joyY=-1;break;
    case KeyEvent.KEYCODE_S:case KeyEvent.KEYCODE_DPAD_DOWN:joyY=1;break;
    case KeyEvent.KEYCODE_SPACE:attack();break;
    case KeyEvent.KEYCODE_E:pickup();break;
    case KeyEvent.KEYCODE_I:inventoryOpen=!inventoryOpen;break;
    case KeyEvent.KEYCODE_ENTER:if(hp<=0)restart();break;
   }return true;
  }
  @Override public boolean onKeyUp(int key,KeyEvent e){
   if(key==KeyEvent.KEYCODE_A||key==KeyEvent.KEYCODE_D||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)joyX=0;
   if(key==KeyEvent.KEYCODE_W||key==KeyEvent.KEYCODE_S||key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN)joyY=0;
   return true;
  }
 }
}
